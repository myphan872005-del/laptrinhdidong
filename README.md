# UED Custom Maps - Geo-Tracking Journal

## Thông tin sinh viên
- Họ và tên: Phan Thị Trúc My
- Mã sinh viên: 3120223120
- Lớp: 23CNTT2
- Đề tài: Xây dựng ứng dụng ghi nhật ký hành trình (Geo-Tracking Journal) cho phượt thủ.

---

## Giới thiệu dự án
Ứng dụng cho phép người dùng ghi lại quãng đường di chuyển thực tế trên bản đồ, đánh dấu các địa điểm dừng chân kèm theo ghi chú và hình ảnh. Dự án tập trung vào khả năng hoạt động ổn định dưới nền (Background Service) và tối ưu hóa dữ liệu tọa độ.

---

## Công nghệ sử dụng (Tech Stack)
- Ngôn ngữ: Kotlin
- UI Framework: Jetpack Compose (Modern Toolkit)
- Kiến trúc: MVVM (Model-View-ViewModel) chuẩn
- Dependency Injection: Hilt (Dagger) để quản lý tài nguyên
- Cơ sở dữ liệu: Room Persistence (Lưu trữ cục bộ với 3 bảng: Journeys, TrackPoints, StopPoints)
- Bản đồ: OSMDroid (OpenStreetMap) tích hợp linh hoạt
- Định vị: Google Play Services Location (Fused Location Provider)

---

## Tính năng hoàn thành (Phase 3)

### 🚀 Retrofit & Sync
- Viết logic đồng bộ dữ liệu từ Room lên Server khi có kết nối mạng
- Sử dụng cờ `isSynced` để kiểm soát trạng thái dữ liệu
- Đảm bảo dữ liệu được đẩy lên server một cách ổn định và tránh trùng lặp

---