const express = require("express");
const db = require("../db");
const verifyToken = require("../middleware/auth"); // Nhớ check lại đường dẫn middleware cho đúng nhé
const router = express.Router();

// =======================================================================
// 🚀 API 1: XUẤT BẢN HÀNH TRÌNH (Tạo Snapshot JSON)
// =======================================================================
router.post("/publish", verifyToken, async (req, res) => {
  const userId = req.user.userId;
  const { journeyId } = req.body;

  if (!journeyId)
    return res.status(400).json({ message: "Thiếu ID hành trình!" });

  let connection;
  try {
    connection = await db.getConnection();

    // 1. Lấy thông tin hành trình gốc (Chỉ lấy bài chưa bị xóa)
    const [journeys] = await connection.query(
      "SELECT * FROM journeys WHERE local_id = ? AND user_id = ? AND is_deleted = 0",
      [journeyId, userId],
    );
    if (journeys.length === 0) {
      return res
        .status(404)
        .json({ message: "Hành trình không tồn tại hoặc đã bị xóa!" });
    }
    const journey = journeys[0];

    // 2. Lấy toàn bộ tọa độ đường đi (Track Points)
    const [trackPoints] = await connection.query(
      "SELECT segment_id, latitude, longitude, timestamp FROM track_points WHERE journey_id = ? ORDER BY timestamp ASC",
      [journeyId],
    );

    // 3. Lấy toàn bộ điểm dừng (Stop Points)
    const [stopPoints] = await connection.query(
      "SELECT local_id, latitude, longitude, note, thumbnail_uri, timestamp FROM stop_points WHERE journey_id = ? AND is_deleted = 0 ORDER BY timestamp ASC",
      [journeyId],
    );

    // 4. Lấy Media nhét vào từng Điểm dừng
    for (let sp of stopPoints) {
      const [media] = await connection.query(
        "SELECT local_id, file_uri, media_type FROM stop_point_media WHERE stop_point_id = ?",
        [sp.local_id],
      );
      sp.media = media; // Gắn mảng ảnh/video thẳng vào object điểm dừng
    }

    // 5. 📦 ĐÓNG GÓI SNAPSHOT (Trái tim của hệ thống)
    const payload = {
      journey: {
        id: journey.local_id,
        title: journey.title,
        start_lat: journey.start_lat,
        start_lon: journey.start_lon,
        start_time: journey.start_time,
      },
      track_points: trackPoints,
      stop_points: stopPoints,
    };

    // 6. Lưu cục JSON vào bảng Khám phá
    const sqlInsert = `
            INSERT INTO discovery_posts (user_id, original_journey_id, payload)
            VALUES (?, ?, ?)
        `;
    // Chú ý: Phải dùng JSON.stringify để biến Object thành chuỗi JSON nhét vào MySQL
    await connection.query(sqlInsert, [
      userId,
      journeyId,
      JSON.stringify(payload),
    ]);

    // (Tùy chọn) Đánh dấu hành trình gốc là đã được chia sẻ
    await connection.query(
      "UPDATE journeys SET isPublic = 1 WHERE local_id = ?",
      [journeyId],
    );

    res
      .status(200)
      .json({ message: "Đã xuất bản hành trình lên Khám phá thành công!" });
  } catch (error) {
    console.error("💥 Lỗi Publish Discovery:", error);
    res.status(500).json({ message: "Lỗi Server khi đóng gói JSON!" });
  } finally {
    if (connection) connection.release();
  }
});

// =======================================================================
// 🌍 API 2: LẤY DANH SÁCH FEED (Trang chủ Khám phá)
// =======================================================================
router.get("/feed", verifyToken, async (req, res) => {
  try {
    // Chỉ cần lấy payload và thông tin tác giả, cực kỳ nhẹ!
    const sql = `
            SELECT p.id as post_id, p.original_journey_id, p.payload, p.likes_count, p.created_at,
                   u.display_name, u.avatar_url
            FROM discovery_posts p
            JOIN users u ON p.user_id = u.id
            ORDER BY p.created_at DESC
            LIMIT 50
        `;
    const [rows] = await db.query(sql);

    // Trả thẳng danh sách cho Android lướt Feed
    res.status(200).json(rows);
  } catch (error) {
    console.error("💥 Lỗi Load Feed:", error);
    res.status(500).json({ message: "Lỗi Server khi tải Feed!" });
  }
});

module.exports = router;
