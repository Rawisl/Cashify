using CashifyBackend.Config;
using CashifyBackend.Features;
using DotNetEnv;
using Google.Cloud.Firestore;

// Tải API Key từ file .env lên bộ nhớ
Env.Load();

var builder = WebApplication.CreateBuilder(args);

string projectId = FirebaseConfig.Initialize();

// Đăng ký HttpClientFactory để tối ưu hóa việc gọi API ra bên ngoài (chống nghẽn cổng mạng)
builder.Services.AddHttpClient();
builder.Services.AddSingleton(FirestoreDb.Create(projectId));

var app = builder.Build();
app.MapGet("/", () => "Ok chào bọn m, t đã ở đây được 3 đêm rồi");

app.MapUtilityEndpoints();
app.MapWorkspaceEndpoints();
app.MapSocialEndpoints();
app.MapChatEndpoints();

app.Run("http://0.0.0.0:5283");
