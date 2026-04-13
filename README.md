Trang giới thiệu tổng hợp các thức thực hiện và trình bày sản phẩm của nhóm khi làm xong sản phẩm Nhóm 1 
                                                
                                                                                    
                                                        UED Custom Maps - Geo-Tracking Journal


    UED Custom Maps là một ứng dụng di động được xây dựng nhằm hỗ trợ người dùng ghi lại và quản lý hành trình di chuyển của mình một cách chi tiết, trực quan và hiệu quả. Ứng dụng sử dụng công nghệ định vị toàn cầu (GPS Tracking) để liên tục thu thập các điểm tọa độ trong quá trình di chuyển của người dùng, sau đó hiển thị lại trên bản đồ dưới dạng các điểm pixel được nối lại với nhau bằng các đoạn thẳng, từ đó tạo thành đường đi thực tế mà người dùng đã trải qua.

    Không chỉ dừng lại ở việc theo dõi quỹ đạo di chuyển, ứng dụng còn cho phép người dùng đánh dấu các điểm dừng chân (Check-in) tại bất kỳ vị trí nào trên bản đồ. Tại mỗi điểm check-in, người dùng có thể đính kèm thêm thông tin như hình ảnh, video hoặc ghi chú cá nhân nhằm lưu giữ lại những khoảnh khắc đáng nhớ trong hành trình của mình. Điều này giúp ứng dụng không chỉ đơn thuần là một công cụ theo dõi vị trí mà còn trở thành một dạng nhật ký hành trình (Geo-Tracking Journal) mang tính cá nhân hóa cao.

    Một điểm nổi bật quan trọng của hệ thống là được thiết kế theo kiến trúc Offline-First, cho phép ứng dụng vẫn hoạt động ổn định ngay cả trong điều kiện không có kết nối Internet. Toàn bộ dữ liệu như tọa độ GPS, thông tin check-in, hình ảnh và ghi chú đều được lưu trữ cục bộ trên thiết bị thông qua cơ sở dữ liệu nội bộ (Room Database) và bộ nhớ trong (Internal Storage). Khi thiết bị khôi phục kết nối mạng, hệ thống sẽ tự động thực hiện quá trình đồng bộ dữ liệu lên máy chủ một cách thông minh mà không cần sự can thiệp từ người dùng.

    Ngoài ra, ứng dụng còn cung cấp khả năng chia sẻ hành trình lên hệ thống cộng đồng, nơi người dùng có thể khám phá, tham khảo và tương tác với các hành trình của những người dùng khác. Điều này góp phần tạo nên một nền tảng kết nối, chia sẻ trải nghiệm du lịch và di chuyển giữa các cá nhân.

+ Thông tin tác giả
   . Sinh viên thực hiện: Nguyễn Hoàng Minh Trí
   . Mã số sinh viên: 3120223215
   . Lớp: 23cntt1
+ Tính năng nổi bật (Core Features)
1. Background GPS Tracking:
    Ứng dụng có khả năng ghi lại quỹ đạo di chuyển của người dùng liên tục ngay cả khi tắt màn hình hoặc ứng dụng bị hệ thống đưa về nền. Tính năng này được triển khai thông qua Foreground Service kết hợp với cơ chế START_STICKY, đảm bảo quá trình theo dõi không bị gián đoạn trong các tình huống sử dụng thực tế.
2. Offline-First Architecture:
    Toàn bộ dữ liệu phát sinh trong quá trình sử dụng như tọa độ GPS, hình ảnh, video và ghi chú đều được lưu trữ trực tiếp trên thiết bị thông qua Room Database và Internal Storage. Điều này giúp ứng dụng hoạt động ổn định trong môi trường không có mạng và giảm thiểu phụ thuộc vào kết nối Internet.
3. Smart Sync:
    Ứng dụng tự động đồng bộ dữ liệu lên máy chủ khi có kết nối mạng trở lại. Dữ liệu được gửi dưới dạng JSON kết hợp với Multipart Data (đối với hình ảnh/video) thông qua WorkManager, đảm bảo quá trình đồng bộ diễn ra an toàn, tối ưu và không làm ảnh hưởng đến trải nghiệm người dùng.
4. Interactive Map:
    Cung cấp giao diện bản đồ tương tác cho phép hiển thị trực quan quỹ đạo di chuyển cùng với các điểm đánh dấu (marker) theo thời gian thực. Người dùng có thể dễ dàng theo dõi, xem lại và phân tích hành trình của mình một cách trực quan.
5. Community Discovery:
    Cho phép người dùng chia sẻ hành trình cá nhân lên hệ thống, từ đó tạo ra một cộng đồng nơi mọi người có thể khám phá, học hỏi và tham khảo các chuyến đi từ những người dùng khác.
+ Công nghệ sử dụng (Tech Stack)
    Frontend (Android): Kotlin, Jetpack Compose, Hilt (Dependency Injection), Coil, OSMDroid, Retrofit, Coroutines/Flow.
    Backend: Laravel, MySQL, RESTful API.
    Network Tunneling: Ngrok (Hỗ trợ kết nối và review ứng dụng từ xa mà không cần triển khai server local).
+ Hướng dẫn cài đặt & Chạy ứng dụng (Dành cho Giảng viên/Reviewer)

Ứng dụng đã được cấu hình sẵn để kết nối trực tiếp đến máy chủ thông qua Ngrok. Vì vậy, người chấm không cần cài đặt XAMPP hoặc triển khai backend trên máy cá nhân.

Các bước thực hiện:

1. Clone hoặc tải mã nguồn Android về máy.
2. Mở dự án bằng Android Studio (phiên bản mới nhất, hỗ trợ Jetpack Compose).
3. Chờ quá trình Gradle Sync hoàn tất để tải đầy đủ các thư viện cần thiết.
4. (Tùy chọn) Kiểm tra cấu hình API tại file NetworkConfig.kt.
5. Kết nối thiết bị Android thật hoặc sử dụng Emulator, sau đó nhấn Run (Shift + F10) để chạy ứng dụng.
6. Khi mở ứng dụng lần đầu, cấp đầy đủ các quyền cần thiết như quyền truy cập vị trí (chọn “Luôn cho phép”) và quyền gửi 6. thông báo để đảm bảo ứng dụng hoạt động đúng chức năng.
7. vào app thành công
