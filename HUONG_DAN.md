# Build APK không cần Android Studio (dùng GitHub Actions)

1. Tạo tài khoản https://github.com (miễn phí).
2. Bấm dấu + > New repository > đặt tên (vd: tiktok-ad-skipper) > Create repository.
3. Trong repo mới bấm "uploading an existing file", kéo THẢ TOÀN BỘ nội dung đã giải nén
   (nhớ cả thư mục ẩn .github) > Commit changes.
   Nếu không kéo được thư mục ẩn: Add file > Create new file, gõ tên
   .github/workflows/build.yml rồi dán nội dung file đó vào.
4. Vào tab Actions > chọn "Build APK" > chờ ~3-5 phút cho tới khi có dấu tích xanh.
5. Bấm vào lần chạy đó > kéo xuống mục Artifacts > tải "TikTokAdSkipper-apk" (file zip chứa app-debug.apk).
6. Giải nén, chép app-debug.apk sang điện thoại và cài.
