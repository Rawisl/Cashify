using CashifyBackend.Models;
using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using System.Text;
using System.Text.Json;

namespace CashifyBackend.Features;

public static class UtilityEndpoints
{
    public static void MapUtilityEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1");

        // QUÉT HÓA ĐƠN AI
        group.MapPost("/scan-bill", async (HttpRequest request, ScanRequest req, IHttpClientFactory httpClientFactory) =>
        {
            var authHeader = request.Headers["Authorization"].FirstOrDefault();
            if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                return Results.Unauthorized();

            try // TẦNG TRY OUTER (Xác thực Token & Gọi API)
            {
                var token = authHeader.Substring("Bearer ".Length);
                await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);

                var apiKey = Environment.GetEnvironmentVariable("OPENROUTER_API_KEY");
                if (string.IsNullOrEmpty(apiKey))
                {
                    return Results.Problem("API Key is missing in server configuration.", statusCode: 500);
                }

                var client = httpClientFactory.CreateClient();
                client.Timeout = TimeSpan.FromSeconds(60);
                var openRouterUrl = "https://openrouter.ai/api/v1/chat/completions";

                // 1. ULTRA-LEAN PROMPT (Tối giản tối đa để né Token Limit)
                var systemPrompt = $"Extract receipt info to JSON. NO markdown. NO extra text.\n" +
                                   $"Schema:\n{{\"amount\":0,\"description\":\"\",\"category\":\"Khác\",\"paymentMethod\":\"Cash\"}}\n" +
                                   $"Valid Payment: Cash, Card, Bank.";
                var userPrompt = req.OcrText; // Truyền chay vào luôn cho nhẹ

                // 3. PAYLOAD: 
                var payload = new
                {
                    //tham khảo thử mấy model khác con nào ngon thì vứt vào chứ openrouter/free hơi hên xui
                    //nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free
                    model = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
                    messages = new[]
                    {
                new { role = "system", content = systemPrompt },
                new { role = "user", content = userPrompt }
            },
                    temperature = 0.0 // ÉP VỀ 0: Chống sáng tạo, biến AI thành cái máy bóc tách data 100% logic.
                };

                var jsonPayload = JsonSerializer.Serialize(payload);
                var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

                client.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");
                client.DefaultRequestHeaders.Add("HTTP-Referer", "http://localhost");
                client.DefaultRequestHeaders.Add("X-Title", "Cashify App");

                try // TẦNG TRY INNER (Xử lý chuỗi JSON trả về)
                {
                    var response = await client.PostAsync(openRouterUrl, content);
                    if (!response.IsSuccessStatusCode)
                    {
                        var errorMsg = await response.Content.ReadAsStringAsync();
                        return Results.Problem($"AI Provider Error: {errorMsg}", statusCode: (int)response.StatusCode);
                    }

                    var responseString = await response.Content.ReadAsStringAsync();

                    using var jsonDoc = JsonDocument.Parse(responseString);
                    var aiMessage = jsonDoc.RootElement
                                           .GetProperty("choices")[0]
                                           .GetProperty("message")
                                           .GetProperty("content")
                                           .GetString()?.Trim();

                    if (aiMessage != null && aiMessage.Contains("{"))
                    {
                        aiMessage = aiMessage.Substring(aiMessage.IndexOf("{"));
                        if (aiMessage.LastIndexOf("}") != -1)
                        {
                            aiMessage = aiMessage.Substring(0, aiMessage.LastIndexOf("}") + 1);
                        }
                    }

                    return Results.Content(aiMessage ?? "{}", "application/json");
                }
                catch (Exception ex)
                {
                    return Results.Problem($"Lỗi xử lý kết quả AI: {ex.Message}", statusCode: 500);
                }
            }
            catch (FirebaseAuthException)
            {
                return Results.Json(new { error = "TOKEN_INVALID", message = "Xác thực thất bại hoặc phiên đăng nhập hết hạn!" }, statusCode: 401);
            }
            catch (Exception ex)
            {
                return Results.Problem($"Lỗi hệ thống: {ex.Message}", statusCode: 500);
            }
        });


        //CẤP CHỮ KÝ CLOUDINARY
        group.MapGet("/cloudinary/sign", async (HttpRequest request) =>
        {
            //Gặp bug băm tham số trong thư viện SDK của cloudinary, tham khảo tài liệu băm chay tại cloudinary.com/documentation/response_signatures
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

                // 1. Xác thực Token Firebase
                var token = authHeader.Substring("Bearer ".Length);
                await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);

                // 2. Kéo Config từ .env (Diệt sạch khoảng trắng và dấu ngoặc kép nếu có)
                var cloudName = Environment.GetEnvironmentVariable("CLOUDINARY_CLOUD_NAME")?.Replace("\"", "").Trim();
                var apiKey = Environment.GetEnvironmentVariable("CLOUDINARY_API_KEY")?.Replace("\"", "").Trim();
                var apiSecret = Environment.GetEnvironmentVariable("CLOUDINARY_API_SECRET")?.Replace("\"", "").Trim();

                if (string.IsNullOrEmpty(cloudName) || string.IsNullOrEmpty(apiKey) || string.IsNullOrEmpty(apiSecret))
                    return Results.Problem("Lỗi cấu hình Cloudinary trên Server", statusCode: 500);

                long timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                string folder = "cashify_uploads";

                // 3. TỰ TAY GHÉP CHUỖI (Chuẩn A-Z: folder rồi tới timestamp)
                string stringToSign = $"folder={folder}&timestamp={timestamp}";

                // 4. Kẹp API_SECRET vào đuôi chuỗi
                string stringToHash = stringToSign + apiSecret;

                // 5. Băm SHA-1 thủ công
                using var sha1 = System.Security.Cryptography.SHA1.Create();
                byte[] hashBytes = sha1.ComputeHash(System.Text.Encoding.UTF8.GetBytes(stringToHash));
                string signature = BitConverter.ToString(hashBytes).Replace("-", "").ToLower();

                // 6. Trả hàng về cho Android
                return Results.Ok(new
                {
                    signature = signature,
                    timestamp = timestamp,
                    apiKey = apiKey,
                    cloudName = cloudName,
                    folder = folder
                });
            }
            catch (Exception ex)
            {
                return Results.Problem($"Lỗi hệ thống: {ex.Message}");
            }
        });
    }
}