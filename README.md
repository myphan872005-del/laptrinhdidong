# 📱 UED Custom Maps
my name:Trí 
**Trạng thái:** Đã hoàn thiện chức năng toàn phần

## 🚀 Các tính năng chính

**1. 📍 Theo dõi vị trí (GPS chạy ngầm)**
- Lấy tọa độ liên tục kể cả khi ẩn app.
- Có bộ lọc tự động để tránh bị lệch vị trí (trôi GPS), nhất là lúc đang đứng yên.

**2. 🧵 Vẽ đường đi trực tiếp**
- Ghi lại các điểm tọa độ và vẽ thành đường đi trên bản đồ theo thời gian thực.

**3. 💾 Tự động lưu hành trình**
- Lưu lại bản đồ và quãng đường đang đi dang dở.
- Tắt app mở lại vẫn khôi phục y xì hành trình cũ, không bị mất.

---

## ⚙️ Cấu trúc App
- Dùng **OSMDroid** để hiển thị bản đồ.
- Code chia lớp gọn gàng: TrackingService (xử lý GPS ngầm), MapRepository (quản lý dữ liệu), MapViewModel (xử lý logic) và Giao diện làm bằng Jetpack Compose.

---

## 🚧 Những phần cần nâng cấp (Sẽ làm sau)
- Chưa có bảng điều khiển (Bật/Tắt/Tạm dừng) hiện trên thanh thông báo của điện thoại.
- Chưa đi test thử xem vẽ đường đi một quãng đường thật dài thì app có bị đơ không.
- Chức năng quản lý hành trình còn hơi "cùi": Bấm xóa là bay sạch mọi thứ, chưa chia ra lưu riêng được từng chuyến đi.
- **Mục tiêu tương lai:** Sẽ tối ưu app thật mượt, thật nhẹ để gánh được cả những *"hành trình khám phá xuyên lục địa"* của anh em mà không lo giật lag hay tốn pin! 🌍🚀