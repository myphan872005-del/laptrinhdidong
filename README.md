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
