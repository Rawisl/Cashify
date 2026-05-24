using System.Text.Json;
using FirebaseAdmin;

namespace CashifyBackend.Config;

public static class FirebaseConfig
{
    public static string Initialize()
    {
        string credentialPath = Path.Combine(Directory.GetCurrentDirectory(), "firebase-admin.json");
        string projectId = "";

        if (File.Exists(credentialPath))
        {
            // Cách mới: Đẩy đường dẫn vào biến môi trường hệ thống
            Environment.SetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", credentialPath);
            FirebaseApp.Create();

            // Tự động bóc tách Project ID từ file json để kết nối Database
            var jsonContent = File.ReadAllText(credentialPath);
            using var doc = JsonDocument.Parse(jsonContent);
            projectId = doc.RootElement.GetProperty("project_id").GetString() ?? "";

            Console.WriteLine($"Firebase Admin (Project: {projectId}) khởi tạo thành công!");
        }
        else
        {
            Console.WriteLine("CẢNH BÁO: Không tìm thấy file firebase-admin.json!");
        }
        return projectId;
    }
}