const express = require("express");
const cors = require("cors");
const path = require("path");
const fs = require("fs");
const morgan = require("morgan");

require("dotenv").config();
require("./cron/cleanup");

const app = express();

// --- 1. KHỞI TẠO THƯ MỤC LƯU TRỮ (Chuẩn hóa vào public/uploads) ---
const publicDir = path.join(__dirname, "public");
const uploadDir = path.join(__dirname, "public/uploads");
const avatarDir = path.join(__dirname, "public/uploads/avatars");
const mediaDir = path.join(__dirname, "public/uploads/media"); // Dành cho ảnh/video điểm dừng sau này

// Tự động tạo thư mục nếu chưa có
[publicDir, uploadDir, avatarDir, mediaDir].forEach((dir) => {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
});

// --- 2. MIDDLEWARE ---
app.use(cors());
app.use(morgan("dev"));

// TĂNG GIỚI HẠN LÊN 50MB ĐỂ GÁNH CỤC DATA ĐỒNG BỘ SIÊU TO KHỔNG LỒ
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ extended: true, limit: "50mb" }));

// Cấp quyền truy cập file tĩnh (Đường dẫn chuẩn: http://localhost:3000/uploads/...)
app.use("/uploads", express.static(uploadDir));

// --- 3. ROUTES ---
const authRoutes = require("./routes/auth");
const journeyRoutes = require("./routes/journeys");
const discoveryRoutes = require("./routes/discovery");

app.use("/api/auth", authRoutes);
app.use("/api/journeys", journeyRoutes);
app.use("/api/discovery", discoveryRoutes);

// API Test
app.get("/", (req, res) => {
  res.json({
    status: "success",
    message: "🚀 Hệ thống Geo-Tracking đang trực chiến!",
    time: new Date().toLocaleString(),
  });
});

// --- 4. XỬ LÝ LỖI (ERROR HANDLER) ---
// Bắt lỗi 404
app.use((req, res, next) => {
  res.status(404).json({ message: "Endpoint không tồn tại!" });
});

// Bắt lỗi Server (500)
app.use((err, req, res, next) => {
  console.error("💥 Lỗi hệ thống:", err.stack);
  // Nếu lỗi do gửi cục JSON quá bự
  if (err.type === "entity.too.large") {
    return res.status(413).json({ message: "Dữ liệu gửi lên quá lớn!" });
  }
  res.status(500).json({ message: "Đã xảy ra lỗi phía Server!" });
});

// --- 5. LẮNG NGHE ---
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`
    *****************************************
    🚀 Server: http://localhost:${PORT}
    📁 Uploads: ${uploadDir}
    🛡️ Mode: ${process.env.NODE_ENV || "development"}
    *****************************************
    `);
});
