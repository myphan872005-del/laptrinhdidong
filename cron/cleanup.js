const cron = require("node-cron");
const db = require("../db"); // Đường dẫn chuẩn trỏ ra ngoài thư mục db

// =========================================================================
// 🗑️ CHẾ ĐỘ THỰC TẾ: Chạy lúc 00:00 mỗi ngày, dọn rác đã tồn tại quá 7 ngày
// =========================================================================
cron.schedule("0 0 * * *", async () => {
  console.log(
    "🧹 [CRON] Bắt đầu phiên dọn dẹp thùng rác định kỳ lúc nửa đêm...",
  );
  let connection;
  try {
    connection = await db.getConnection();

    // Lệnh dọn rác chuẩn: Xóa các điểm is_deleted = 1 và thời gian xóa cách đây HƠN 7 NGÀY
    const sqlCleanup = `
            DELETE FROM stop_points 
            WHERE is_deleted = 1 
            AND updated_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
        `;

    const [result] = await connection.query(sqlCleanup);

    if (result.affectedRows > 0) {
      console.log(
        `✅ [CRON] Đã dọn dẹp vĩnh viễn ${result.affectedRows} điểm dừng hết hạn 7 ngày.`,
      );
    } else {
      // Dòng này có thể comment lại cho đỡ rác log console mỗi ngày nếu muốn
      console.log(
        "ℹ️ [CRON] Không có rác nào quá hạn 7 ngày. Thùng rác ổn định.",
      );
    }
  } catch (error) {
    console.error("💥 [CRON LỖI]:", error);
  } finally {
    if (connection) connection.release();
  }
});
