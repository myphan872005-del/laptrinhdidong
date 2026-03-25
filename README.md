# 📱 UED Custom Maps

## 🚀 Features đã triển khai

### 1. 📍 GPS Tracking (Foreground Service)
- Theo dõi vị trí người dùng bằng FusedLocationProviderClient
- Chạy ngầm bằng TrackingService
- Có lọc nhiễu GPS:
  - Accuracy
  - Speed
  - Distance

### 2. 🧵 Vẽ đường đi (Polyline)
- Lưu danh sách các điểm GPS (GeoPointData)
- Vẽ đường đi theo thời gian thực trên bản đồ

### 3. 💾 Lưu dữ liệu
- Lưu map và polyline bằng SharedPreferences
- Có thể khôi phục lại dữ liệu khi mở app

---

## 🏗️ Kiến trúc

- `TrackingService`: xử lý GPS nền
- `MapRepository`: lưu và đọc dữ liệu
- `MapViewModel`: quản lý logic
- `UI`: Jetpack Compose + Navigation

---

## 📌 Ghi chú
- Sử dụng OSMDroid để hiển thị bản đồ
- Tối ưu để tránh nhiễu GPS khi đứng yên

## 1 số vấn đề:
- Chưa có hiển thị và kiểm soát ghi hành trình trên thanh thông báo
- chưa test được polyline chặng đường dài
- chưa có mục quản lý hành trình rõ ràng ( bấm xóa là xóa hết hành trình luôn :> )
