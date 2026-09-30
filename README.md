# TikTok Ad Skipper (Android)

Tự vuốt qua quảng cáo, livestream và quảng cáo game trên app TikTok bằng Accessibility Service.
Mỗi loại có công tắc bật/tắt riêng trong app.

## Cách dựng dự án (Android Studio)

1. **New Project → Empty Views Activity**
   - Name: `TikTok Ad Skipper`
   - Package: `com.example.tiktokadskip`
   - Language: Kotlin, Minimum SDK: API 24
2. Chép các file vào đúng chỗ (ghi đè file có sẵn):
   - `AdSkipService.kt`, `MainActivity.kt` → `app/src/main/java/com/example/tiktokadskip/`
   - `AndroidManifest.xml` → `app/src/main/`
   - `accessibility_service_config.xml` → `app/src/main/res/xml/` (tạo thư mục `xml` nếu chưa có)
3. Mở `res/values/strings.xml`, thêm dòng:
   ```xml
   <string name="service_description">Tự động vuốt qua video quảng cáo trên TikTok.</string>
   ```
4. Xóa file layout `activity_main.xml` nếu có (MainActivity tự dựng giao diện).
5. **Build → Build APK(s)**, chép APK sang điện thoại và cài.

## Sử dụng

1. Mở app → bấm **Mở cài đặt Trợ năng**.
2. Chọn **TikTok Ad Skipper** → bật.
   - Nếu nút bị mờ (Android 13+): vào Cài đặt → Ứng dụng → TikTok Ad Skipper → ⋮ → **Cho phép cài đặt bị hạn chế**, rồi bật lại.
3. Mở TikTok và lướt bình thường.

## Nếu không bắt được quảng cáo

Nhãn quảng cáo có thể khác tùy phiên bản/ngôn ngữ TikTok.

1. Đổi `DEBUG = true` trong `AdSkipService.kt`, build lại.
2. Cắm điện thoại, mở Logcat, lọc tag `AdSkip`, lướt tới một video quảng cáo.
3. Tìm dòng `text=...` hoặc `desc=...` là nhãn quảng cáo, rồi thêm vào `AD_LABELS` (viết thường).

## Livestream và game

- **Livestream:** nhận diện qua nhãn `LIVE` / `Trực tiếp` trên video. Nhãn `LIVE` ở thanh menu trên cùng được bỏ qua (15% màn hình phía trên) để không vuốt liên tục.
- **Game:** chỉ nhận diện được **quảng cáo game** qua nút như "Chơi ngay" / "Play now" / "Cài đặt ngay". Video nội dung về game do người dùng đăng thì không có nhãn chung để nhận diện, nên app không chặn được.
- Nếu không bắt được, dùng chế độ DEBUG ở trên để lấy đúng chữ rồi thêm vào `LIVE_LABELS` hoặc `GAME_LABELS`.

## Ưu tiên nhạc yêu thích

Nhập danh sách bài hát/nghệ sĩ trong app (ngăn cách bằng dấu phẩy). App đọc dòng tên nhạc hiện ở góc dưới video TikTok rồi so khớp (không phân biệt hoa thường và dấu tiếng Việt).

- **Rung khi gặp nhạc yêu thích:** điện thoại rung nhẹ khi video có nhạc khớp danh sách, bạn biết để dừng lại xem.
- **Chỉ xem nhạc yêu thích:** tự vuốt qua video có nhạc không khớp, dừng lại ở video khớp. Mặc định **tắt**. Vuốt liên tiếp tối đa 25 video rồi tự nghỉ 1 phút.
- App không thay đổi được thuật toán gợi ý của TikTok, chỉ lọc những gì hiện ra trong feed.
- Nếu không nhận được nhạc: bật `DEBUG = true`, xem dòng `music=...` trong Logcat, rồi thêm dấu hiệu tương ứng vào `MUSIC_MARKERS`.

## Chạy nền ổn định

Accessibility Service tự chạy nền khi đã bật, không cần Foreground Service hay thông báo. Một số hãng máy (Xiaomi, Oppo, Vivo, Realme, Samsung) hay tắt app nền, nên hãy:

1. Bấm nút **Không tối ưu pin cho app** trong app, chọn **Cho phép**.
2. Bật **Tự khởi động** (Autostart) cho app trong Cài đặt của máy (Xiaomi/Oppo/Vivo).
3. Ghim app trong màn hình đa nhiệm (Recent apps → biểu tượng ghim/khóa) để hệ thống không dọn.
4. Nếu service tự tắt, vào lại Trợ năng và bật lại.

## Lưu ý

- Quảng cáo vẫn hiện thoáng qua khoảng 1 giây trước khi bị vuốt.
- Cần cập nhật nếu TikTok đổi giao diện.
- App chỉ đọc màn hình TikTok, không kết nối mạng, không gửi dữ liệu.
