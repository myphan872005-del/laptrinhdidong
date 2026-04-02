const mysql = require("mysql2/promise");
require("dotenv").config();

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "custom_maps_db",
  port: process.env.DB_PORT || 3307, // Default MySQL là 3306, bạn dùng 3307 thì sửa trong .env

  // --- TỐI ƯU HIỆU NĂNG ---
  waitForConnections: true,
  connectionLimit: 20, // Tăng lên một chút nếu app có nhiều user đồng bộ cùng lúc
  queueLimit: 0,

  // --- TỐI ƯU ĐỘ ỔN ĐỊNH ---
  enableKeepAlive: true,
  keepAliveInitialDelay: 10000, // 10 giây gửi tín hiệu "nuôi" kết nối 1 lần
  connectTimeout: 10000, // 10 giây không kết nối được thì báo lỗi ngay
});

// Logic kiểm tra kết nối (Giữ nguyên ý tưởng của Hoan nhưng viết gọn hơn)
(async () => {
  try {
    const connection = await pool.getConnection();
    console.log(
      "🟢 [MySQL] Kết nối Database thành công trên cổng " +
        (process.env.DB_PORT || 3307),
    );
    connection.release();
  } catch (err) {
    console.error("🔴 [MySQL] Lỗi kết nối Database:");
    console.error("- Message:", err.message);
    console.error("- Code:", err.code);
    console.error("- Hãy kiểm tra lại file .env hoặc MySQL Service!");
  }
})();

module.exports = pool;
