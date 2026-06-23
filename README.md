# 💰 Cashify — Backend (ASP.NET Core API)

API backend cho ứng dụng quản lý tài chính cá nhân Cashify, được xây dựng với ASP.NET Core 9.0 và Firebase Firestore.

## 1. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Framework | ASP.NET Core Web API (.NET 9.0) |
| Database | Firebase Firestore (NoSQL, cloud) |
| Authentication | Firebase Admin SDK |
| Cloud Storage | Cloudinary |
| AI / Chat | OpenRouter |
| Environment Variables | DotNetEnv |
| HTTP Client | HttpClientFactory |

## 2. Yêu cầu cài đặt

- .NET 9.0 SDK
- Firebase project đã bật Firestore database
- Cloudinary account

Kiểm tra:

```powershell
dotnet --version   # >= 9.0
```

## 3. Cấu trúc thư mục

```
CashifyBackend/
├─ Config/
│  └─ FirebaseConfig.cs          # Khởi tạo Firebase Admin SDK
├─ Features/
│  ├─ ChatEndpoints.cs           # Nhắn tin giữa người dùng (Friend Chat)
│  ├─ SocialEndpoints.cs         # Bảng tin, bài đăng, bình luận
│  ├─ UtilityEndpoints.cs        # Quét hóa đơn AI (OpenRouter) & Cloudinary
│  └─ WorkspaceEndpoints.cs      # Quản lý nhóm/không gian làm việc
├─ Models/
├─ Properties/
├─ Program.cs                    # Entry point
├─ CashifyBackend.csproj
├─ CashifyBackend.sln
├─ appsettings.json
├─ .env                           # Biến môi trường
└─ firebase-admin.json           # Service account key (bắt buộc, không commit)
```

## 4. Khởi động cơ sở dữ liệu

Không cần khởi động database riêng — dự án dùng **Firebase Firestore**, là cloud database được quản lý sẵn trên Firebase Console. Chỉ cần đảm bảo:

- Project Firebase đã tạo Firestore database.
- File `firebase-admin.json` (service account key) đã được tải về và đặt đúng vị trí (xem mục 5).

Lưu ý: Một số tính năng yêu cầu tạo Composite Index trên Firestore Console nếu có thông báo lỗi từ API.

## 5. Cấu hình biến môi trường (`.env`)

Tạo file `.env` ngay tại thư mục gốc `CashifyBackend/`:

```bash
# === BẮT BUỘC ===
OPENROUTER_API_KEY=your_openrouter_api_key
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret
```

Không có biến môi trường tùy chọn.

**Lưu ý quan trọng:** file `firebase-admin.json` phải được đặt tại thư mục gốc của dự án — đây là điều kiện bắt buộc để khởi tạo Firebase Admin SDK, độc lập với file `.env`.

Tối thiểu để chạy được: `OPENROUTER_API_KEY`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` và file `firebase-admin.json`.

Hướng dẫn: File firebase-admin.json là file chứa toàn bộ thông tin tài khoản dịch vụ, bạn có thể tải nó từ: Firebase Console > Project Settings > Service Accounts > Generate New Private Key.

## 6. Áp dụng migrations

Không áp dụng — Firestore là NoSQL schema-less, không cần migration để tạo schema.

## 7. Chạy API

Dự án dùng **DotNetEnv** nên có thể tự động tải biến môi trường từ file `.env` khi khởi động. Nếu muốn nạp thủ công, có thể dùng cách dưới đây.

**PowerShell (Windows):**

```powershell
$env:OPENROUTER_API_KEY="your_key"
$env:CLOUDINARY_CLOUD_NAME="your_cloud_name"
$env:CLOUDINARY_API_KEY="your_api_key"
$env:CLOUDINARY_API_SECRET="your_api_secret"

dotnet run
```

**Bash (Linux/macOS):**

```bash
export OPENROUTER_API_KEY="your_key"
export CLOUDINARY_CLOUD_NAME="your_cloud_name"
export CLOUDINARY_API_KEY="your_api_key"
export CLOUDINARY_API_SECRET="your_api_secret"

dotnet run
```

## 8. Kiểm tra
- Môi trường Local: http://localhost:5283/
- Môi trường thực tế: https://api.cashify.io.vn
- Health check: Truy cập vào tên miền https://api.cashify.io.vn để nhận phản hồi "Hi guys!!"

## 9. Lệnh hữu ích

```powershell
dotnet build   # Build project
dotnet run     # Chạy project
dotnet clean   # Xoá build artifacts
dotnet restore # Khôi phục các thư viện NuGet
```

## 10. Lỗi thường gặp

| Lỗi | Nguyên nhân / Cách xử lý |
|---|---|
| `CẢNH BÁO: Không tìm thấy file firebase-admin.json!` | File `firebase-admin.json` không tồn tại tại thư mục gốc — đặt file service account key đúng vị trí. |
| Firebase initialization failed | File `firebase-admin.json` không hợp lệ hoặc Project ID không đúng — kiểm tra lại file JSON tải từ Firebase Console. |
| Cloudinary upload failed | Thiếu hoặc sai một trong `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. |
| OpenRouter API call failed | `OPENROUTER_API_KEY` không hợp lệ hoặc đã hết hạn. |
| Cổng 5283 bận | Đổi port trong `Program.cs`, hoặc đóng ứng dụng đang dùng port đó. |
