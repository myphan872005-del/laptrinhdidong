<<<<<<< HEAD
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
=======
PHASE 1: CORE TRACKING & SYNC (COMPLETED)
1. Kiến trúc Singleton Repository (Bắt buộc)
Thay đổi: Chuyển MapRepository sang Singleton để Service và UI dùng chung 1 instance duy nhất.

Cách dùng: Tuyệt đối không khởi tạo mới. Phải dùng:
MapRepository.getInstance(context)

2. Đồng bộ dữ liệu (StateFlow)
Cơ chế: Dữ liệu từ Service ghi vào file sẽ tự "chảy" về UI qua StateFlow.

Kết quả: Tọa độ và trạng thái nút bấm tự cập nhật thời gian thực mà không cần load lại trang.

UI Code: val maps by viewModel.maps.collectAsState()

3. Tối ưu Tracking & Pin
Lọc nhiễu GPS (3 lớp): * Sai số > 25m (Bỏ).

Di chuyển < 3m (Bỏ).

Vận tốc nhảy vọt (Bỏ).

Battery Adaptive: * Đang sạc: Quét mỗi 3s.

Dùng pin: Quét mỗi 10s.

Pin yếu (<15%): Quét mỗi 30s.

4. Tính năng đã chạy
[x] Start/Stop Tracking ngầm (Foreground Service).

[x] Notification có nút "Dừng ghi" đồng bộ với App.

[x] Lưu/Xóa danh sách nhiều hành trình.

🛠 Lưu ý cho Team
Check Log: Lọc từ khóa DEBUG_APP trong Logcat để xem luồng dữ liệu.

Android 14: Đã cấu hình quyền Location chạy ngầm và Notification.
>>>>>>> 603753dd2a8820997e3d3e2903eaf40f82684e47
