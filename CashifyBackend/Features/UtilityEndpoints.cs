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

        // SCAN BILL VIA AI
        group.MapPost("/scan-bill", async (HttpRequest request, ScanRequest req, IHttpClientFactory httpClientFactory) =>
        {
            var authHeader = request.Headers["Authorization"].FirstOrDefault();
            if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                return Results.Unauthorized();

            try // OUTER TRY: Token validation & API call
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

                // 1. ULTRA-LEAN PROMPT to avoid Token Limit
                var systemPrompt = $"Extract receipt info to JSON. NO markdown. NO extra text.\n" +
                                   $"Schema:\n{{\"amount\":0,\"description\":\"\",\"category\":\"Khác\",\"paymentMethod\":\"Cash\"}}\n" +
                                   $"Valid Payment: Cash, Card, Bank.";
                var userPrompt = req.OcrText; // Pass raw text directly

                // 2. PAYLOAD
                var payload = new
                {
                    // Consider testing other models if openrouter/free is inconsistent
                    model = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
                    messages = new[]
                    {
                        new { role = "system", content = systemPrompt },
                        new { role = "user", content = userPrompt }
                    },
                    temperature = 0.0 // Strict data extraction mode
                };

                var jsonPayload = JsonSerializer.Serialize(payload);
                var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

                client.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");
                client.DefaultRequestHeaders.Add("HTTP-Referer", "http://localhost");
                client.DefaultRequestHeaders.Add("X-Title", "Cashify App");

                try // INNER TRY: Process returned JSON
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
                    return Results.Problem($"Error processing AI response: {ex.Message}", statusCode: 500);
                }
            }
            catch (FirebaseAuthException)
            {
                return Results.Json(new { error = "TOKEN_INVALID", message = "Authentication failed or session expired!" }, statusCode: 401);
            }
            catch (Exception ex)
            {
                return Results.Problem($"System error: {ex.Message}", statusCode: 500);
            }
        });


        // CLOUDINARY SIGNATURE GENERATION
        group.MapGet("/cloudinary/sign", async (HttpRequest request) =>
        {
            // Manual hashing workaround for Cloudinary SDK parameter hashing bug: cloudinary.com/documentation/response_signatures
            // Ref: cloudinary.com/documentation/response_signatures
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                // 1. Verify Firebase Token
                var token = authHeader.Substring("Bearer ".Length);
                await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);

                // 2. Fetch Config from .env (Remove whitespace and quotes)
                var cloudName = Environment.GetEnvironmentVariable("CLOUDINARY_CLOUD_NAME")?.Replace("\"", "").Trim();
                var apiKey = Environment.GetEnvironmentVariable("CLOUDINARY_API_KEY")?.Replace("\"", "").Trim();
                var apiSecret = Environment.GetEnvironmentVariable("CLOUDINARY_API_SECRET")?.Replace("\"", "").Trim();

                if (string.IsNullOrEmpty(cloudName) || string.IsNullOrEmpty(apiKey) || string.IsNullOrEmpty(apiSecret))
                    return Results.Problem("Cloudinary server configuration error", statusCode: 500);

                long timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                string folder = "cashify_uploads";

                // 3. MANUAL STRING CONCATENATION (Alphabetical: folder then timestamp)
                string stringToSign = $"folder={folder}&timestamp={timestamp}";

                // 4. Append API_SECRET
                string stringToHash = stringToSign + apiSecret;

                // 5. Manual SHA-1 Hashing
                using var sha1 = System.Security.Cryptography.SHA1.Create();
                byte[] hashBytes = sha1.ComputeHash(System.Text.Encoding.UTF8.GetBytes(stringToHash));
                string signature = BitConverter.ToString(hashBytes).Replace("-", "").ToLower();

                // 6. Return response to client
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
                return Results.Problem($"System error: {ex.Message}");
            }
        });
    }
}