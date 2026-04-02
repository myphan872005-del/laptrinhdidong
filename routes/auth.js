const express = require("express");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const path = require("path");
const db = require("../db");
const verifyToken = require("../middleware/auth");

const router = express.Router();

// --- CẤU HÌNH MULTER (NƠI LƯU TRỮ ẢNH) ---
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    // Lưu vào thư mục public/uploads/avatars
    cb(null, "public/uploads/avatars");
  },
  filename: (req, file, cb) => {
    // Đặt tên file: avatar-userId-timestamp.jpg
    const uniqueSuffix = Date.now() + path.extname(file.originalname);
    cb(null, "avatar-" + uniqueSuffix);
  },
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // Giới hạn 5MB cho nhẹ server
});

// --- 1. REGISTER (ĐĂNG KÝ) ---
router.post("/register", async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password || password.length < 6) {
      return res.status(400).json({
        message: "Username và Password (tối thiểu 6 ký tự) là bắt buộc!",
      });
    }

    const [existing] = await db.query(
      "SELECT id FROM users WHERE username = ?",
      [username],
    );
    if (existing.length > 0)
      return res.status(400).json({ message: "Tài khoản đã tồn tại!" });

    const hashedPassword = await bcrypt.hash(password, 10);
    const [result] = await db.query(
      "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)",
      [username, hashedPassword, username],
    );

    res
      .status(201)
      .json({ message: "Đăng ký thành công!", userId: result.insertId });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Lỗi Server khi đăng ký!" });
  }
});

// --- 2. LOGIN (ĐĂNG NHẬP) ---
router.post("/login", async (req, res) => {
  try {
    const { username, password } = req.body;
    const [users] = await db.query("SELECT * FROM users WHERE username = ?", [
      username,
    ]);

    if (users.length === 0)
      return res.status(400).json({ message: "Tài khoản không tồn tại!" });

    const user = users[0];
    const isMatch = await bcrypt.compare(password, user.password);

    if (!isMatch) return res.status(400).json({ message: "Sai mật khẩu!" });

    const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, {
      expiresIn: "7d",
    });

    console.log("-------------------------------");
    console.log("TOKEN CỦA HOAN ĐÂY:");
    console.log(token);
    console.log("-------------------------------");

    res.json({
      token,
      user: {
        id: user.id,
        username: user.username,
        display_name: user.display_name, // SỬA DÒNG NÀY (Đổi thành snake_case)
        avatar_url: user.avatar_url,
      },
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: "Lỗi Server khi đăng nhập!" });
  }
});

// --- 3. UPLOAD AVATAR (MỚI) ---
// Android sẽ gửi file kèm key tên là "avatar"
router.post(
  "/upload-avatar",
  verifyToken,
  upload.single("avatar"),
  async (req, res) => {
    try {
      if (!req.file)
        return res.status(400).json({ message: "Vui lòng chọn ảnh!" });

      const userId = req.user.userId;
      // URL này dùng để Android load ảnh (Thay IP 192.168.1.25 bằng IP thật của Hoan nếu đổi mạng)
      const avatarUrl = `http://192.168.1.25:3000/uploads/avatars/${req.file.filename}`;

      // Cập nhật link ảnh vào DB
      await db.query("UPDATE users SET avatar_url = ? WHERE id = ?", [
        avatarUrl,
        userId,
      ]);

      res.json({
        message: "Tải ảnh lên thành công!",
        avatarUrl: avatarUrl,
      });
    } catch (error) {
      console.error("Lỗi Upload:", error);
      res.status(500).json({ message: "Lỗi Server khi upload ảnh!" });
    }
  },
);

module.exports = router;
