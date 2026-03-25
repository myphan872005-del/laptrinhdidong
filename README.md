# 📱 UED Custom Maps

## 🚀 Features đã triển khai

### 1. 📍 GPS Tracking (Foreground Service)
- Theo dõi vị trí người dùng liên tục
- Chạy ngầm bằng Service
- Sử dụng FusedLocationProviderClient

### 2. 🧵 Vẽ đường đi (Polyline)
- Lưu các điểm GPS theo thời gian
- Hiển thị đường đi trên bản đồ

### 3. 💾 Lưu dữ liệu (SharedPreferences)
- Lưu map và polyline
- Khôi phục lại khi mở app

## 🏗️ Kiến trúc

- `TrackingService`: xử lý GPS
- `MapRepository`: lưu dữ liệu
- `MapViewModel`: quản lý logic
- `UI`: Compose + Navigation
