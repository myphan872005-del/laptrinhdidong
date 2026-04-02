const express = require("express");
const db = require("../db");
const verifyToken = require("../middleware/auth");
const router = express.Router();

router.post("/sync", verifyToken, async (req, res) => {
  console.log("========== KIỂM TRA HÀNG TỪ ANDROID ==========");
  if (req.body.stopPoints && req.body.stopPoints.length > 0) {
    // Chỉ in ra điểm đầu tiên để xem cấu trúc JSON
    console.log(JSON.stringify(req.body.stopPoints[0], null, 2));
  }
  console.log("==============================================");

  let connection;
  try {
    connection = await db.getConnection();
    await connection.beginTransaction(); // Bắt đầu Khóa an toàn

    const {
      id,
      title,
      startTime,
      startLat,
      startLon,
      updatedAt,
      isDeleted,
      trackPoints,
      stopPoints,
    } = req.body;
    const userId = req.user.userId;

    if (!id || !title) throw new Error("Thiếu thông tin hành trình!");

    // =========================================================
    // 1. HARD DELETE (Xóa vĩnh viễn nếu điện thoại báo xóa hành trình)
    // =========================================================
    if (isDeleted === 1) {
      // Xóa media trước (vì nó là bảng con sâu nhất)
      await connection.query(
        "DELETE FROM stop_point_media WHERE stop_point_id IN (SELECT local_id FROM stop_points WHERE journey_id = ?)",
        [id],
      );
      await connection.query("DELETE FROM stop_points WHERE journey_id = ?", [
        id,
      ]);
      await connection.query("DELETE FROM track_points WHERE journey_id = ?", [
        id,
      ]);
      await connection.query(
        "DELETE FROM journeys WHERE local_id = ? AND user_id = ?",
        [id, userId],
      );

      await connection.commit();
      console.log(`🗑️ Đã xóa vĩnh viễn hành trình: ${id}`);
      return res
        .status(200)
        .json({ message: "Đã dọn dẹp sạch sẽ hành trình!" });
    }

    // =========================================================
    // 2. LƯU/CẬP NHẬT HÀNH TRÌNH (journeys)
    // =========================================================
    const sqlJourney = `
        INSERT INTO journeys (user_id, local_id, title, start_lat, start_lon, start_time, updated_at, is_deleted) 
        VALUES (?, ?, ?, ?, ?, ?, ?, 0)
        ON DUPLICATE KEY UPDATE 
            title = VALUES(title), start_lat = VALUES(start_lat), start_lon = VALUES(start_lon), 
            updated_at = VALUES(updated_at), is_deleted = 0
    `;
    await connection.query(sqlJourney, [
      userId,
      id,
      title,
      startLat,
      startLon,
      startTime,
      updatedAt || startTime,
    ]);

    // =========================================================
    // 3. ĐỒNG BỘ TỌA ĐỘ ĐƯỜNG ĐI (track_points) - Bulk Insert
    // =========================================================
    if (trackPoints && trackPoints.length > 0) {
      await connection.query("DELETE FROM track_points WHERE journey_id = ?", [
        id,
      ]);

      const trackValues = trackPoints.map((tp) => [
        id,
        tp.segment_id,
        tp.latitude,
        tp.longitude,
        tp.timestamp,
      ]);

      const sqlTrack =
        "INSERT INTO track_points (journey_id, segment_id, latitude, longitude, timestamp) VALUES ?";
      await connection.query(sqlTrack, [trackValues]);
      console.log(`✅ Đã lưu ${trackPoints.length} tọa độ.`);
    }

    // =========================================================
    // 4. ĐỒNG BỘ ĐIỂM DỪNG & MEDIA (stop_points & stop_point_media)
    // =========================================================
    if (stopPoints && stopPoints.length > 0) {
      for (const sp of stopPoints) {
        const isDeletedStatus =
          sp.is_deleted === 1 ||
          sp.is_deleted === true ||
          sp.isDeleted === 1 ||
          sp.isDeleted === true
            ? 1
            : 0;
        const mediaItems = sp.media || sp.mediaList || sp.media_list || [];
        const thumbUri = sp.thumbnail_uri || sp.thumbnailUri || null;

        if (isDeletedStatus === 1) {
          // 🧟‍♂️ LOGIC CHỐNG ZOMBIE: Nếu là rác, CHỈ UPDATE.
          // Nếu DB không có (do Cron đã xóa), lệnh này sẽ không làm gì cả (0 affected rows) -> Chống hồi sinh!
          await connection.query(
            "UPDATE stop_points SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE local_id = ?",
            [sp.local_id],
          );
          // Xóa luôn media của nó cho chắc cú
          await connection.query(
            "DELETE FROM stop_point_media WHERE stop_point_id = ?",
            [sp.local_id],
          );
          console.log(
            `🗑️ Đã đánh dấu xóa (hoặc chặn Zombie hồi sinh) điểm: ${sp.local_id}`,
          );
        } else {
          // 🟢 LOGIC BÌNH THƯỜNG: Thêm mới hoặc cập nhật điểm còn sống
          console.log(
            `--- DEBUG ĐIỂM DỪNG --- ID: ${sp.local_id} | Note: ${sp.note} | Số Media: ${mediaItems.length}`,
          );

          const sqlStopPoint = `
            INSERT INTO stop_points 
                (journey_id, local_id, latitude, longitude, note, thumbnail_uri, timestamp, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)
            ON DUPLICATE KEY UPDATE 
                note = VALUES(note),
                thumbnail_uri = VALUES(thumbnail_uri),
                updated_at = CURRENT_TIMESTAMP,
                is_deleted = 0
        `;
          await connection.query(sqlStopPoint, [
            id,
            sp.local_id,
            sp.latitude,
            sp.longitude,
            sp.note,
            thumbUri,
            sp.timestamp,
          ]);

          // Xử lý Media (Chỉ khi có media và điểm không bị xóa)
          if (mediaItems.length > 0) {
            await connection.query(
              "DELETE FROM stop_point_media WHERE stop_point_id = ?",
              [sp.local_id],
            );
            const mediaValues = mediaItems.map((m) => [
              sp.local_id,
              m.local_id || m.id,
              m.file_uri || m.fileUri,
              m.media_type || m.mediaType,
            ]);
            const sqlMedia =
              "INSERT INTO stop_point_media (stop_point_id, local_id, file_uri, media_type) VALUES ?";
            await connection.query(sqlMedia, [mediaValues]);
            console.log(
              `📸 Đã lưu ${mediaItems.length} media cho điểm dừng ${sp.local_id}`,
            );
          }
        }
      }
      console.log(`✅ Đã xử lý xong ${stopPoints.length} điểm dừng.`);
    }

    // Hoàn tất mọi thứ thành công!
    await connection.commit();
    res.status(200).json({
      message: "Đồng bộ Full hệ thống thành công!",
    });
  } catch (error) {
    if (connection) await connection.rollback();
    console.error("💥 Lỗi Sync MySQL:", error);
    res.status(500).json({ message: "Lỗi Server!", details: error.message });
  } finally {
    if (connection) connection.release();
  }
});

// API PUBLIC (Dành cho Feed cộng đồng)
router.get("/public", async (req, res) => {
  try {
    const sql = `
            SELECT j.*, u.display_name, u.avatar_url 
            FROM journeys j
            JOIN users u ON j.user_id = u.id
            WHERE j.is_deleted = 0 AND j.isPublic = 1 
            ORDER BY j.start_time DESC
        `;
    const [rows] = await db.query(sql);
    res.json(rows);
  } catch (error) {
    res.status(500).json({ message: "Lỗi Server!" });
  }
});

module.exports = router;
