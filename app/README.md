UED Custom Maps - Geo-Tracking Journal
Sinh viên thực hiện: Luyện Ngọc Lâm Hoan

Mã sinh viên: 3120223064 - Lớp: 23cntt2

1. Giới thiệu
   Ứng dụng di động dành cho phượt thủ ghi lại hành trình theo thời gian thực, hỗ trợ đánh dấu các điểm dừng chân (StopPoints) kèm hình ảnh và ghi chú.

2. Công nghệ sử dụng (Tech Stack)
   Language: Kotlin

UI Framework: Jetpack Compose

Architecture: MVVM (Model-View-ViewModel)

Dependency Injection: Hilt

Database: Room Persistence (3 tables: Journeys, TrackPoints, StopPoints)

Map Engine: OSMDroid (OpenStreetMap)

Location Services: Google Play Services Location

3. Tính năng chính (Phase 1)
   Hành trình: Tạo mới, tìm kiếm và xóa hành trình.

Theo dõi (Tracking): Tự động ghi tọa độ dưới nền (Background Service).

Logic Segment: Hỗ trợ Tạm dừng/Tiếp tục mà không gây lỗi nối đường kẻ chéo (Segmented Polyline).

Điểm dừng: Đánh dấu vị trí hiện tại kèm ghi chú và ảnh từ Gallery.

Định vị: Chế độ định vị "Follow me" và tự động quay về điểm gốc (Checkpoint).