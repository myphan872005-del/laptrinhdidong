# UED Custom Maps - Geo-Tracking Journal

## Thông tin sinh viên
* **Họ và tên:** Luyện Ngọc Lâm Hoan
* **Mã sinh viên:** 3120223064
* **Lớp:** 23cntt2
* **Đề tài:** Xây dựng ứng dụng ghi nhật ký hành trình (Geo-Tracking Journal) cho phượt thủ.

## Giới thiệu dự án
Ứng dụng cho phép người dùng ghi lại quãng đường di chuyển thực tế trên bản đồ, đánh dấu các địa điểm dừng chân kèm theo ghi chú và hình ảnh. Dự án tập trung vào khả năng hoạt động ổn định dưới nền (Background Service) và tối ưu hóa dữ liệu tọa độ.

## Công nghệ sử dụng (Tech Stack)
* **Ngôn ngữ:** Kotlin
* **UI Framework:** Jetpack Compose (Modern Toolkit)
* **Kiến trúc:** MVVM (Model-View-ViewModel) chuẩn.
* **Dependency Injection:** Hilt (Dagger) để quản lý tài nguyên.
* **Cơ sở dữ liệu:** Room Persistence (Lưu trữ cục bộ với 3 bảng: Journeys, TrackPoints, StopPoints).
* **Bản đồ:** OSMDroid (OpenStreetMap) tích hợp linh hoạt.
* **Định vị:** Google Play Services Location (Fused Location Provider).

## Tính năng hoàn thành (Phase 1)
1. **Quản lý hành trình:** Tạo mới, tìm kiếm và xóa hành trình với cơ chế Cascade Delete (Xóa sạch dữ liệu liên quan).
2. **Background Tracking:** Ghi tọa độ liên tục ngay cả khi tắt màn hình hoặc thoát ứng dụng.
3. **Logic Segment:** Xử lý nút Tạm dừng/Tiếp tục thông minh, không gây lỗi nối đường kẻ chéo giữa các đoạn di chuyển.
4. **Bộ lọc GPS:** Tích hợp lọc nhiễu dựa trên độ chính xác (Accuracy) và khoảng cách tối thiểu (Distance) để tiết kiệm pin.
5. **Điểm dừng (StopPoint):** Đánh dấu vị trí tức thời, cho phép lưu ghi chú và chọn ảnh từ Gallery.
6. **UI/UX:** Hệ thống định vị một chạm, đồng bộ icon mũi tên chuyên dụng và danh sách quản lý điểm dừng (StopList).

---
