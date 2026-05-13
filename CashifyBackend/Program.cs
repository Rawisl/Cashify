using DotNetEnv;
using System.Text.Json;
using System.Text;
using FirebaseAdmin;
using Google.Cloud.Firestore;
using FirebaseAdmin.Auth;
using CloudinaryDotNet;

// Tải API Key từ file .env lên bộ nhớ
Env.Load();

var builder = WebApplication.CreateBuilder(args);

// Đăng ký HttpClientFactory để tối ưu hóa việc gọi API ra bên ngoài (chống nghẽn cổng mạng)
builder.Services.AddHttpClient();

var app = builder.Build();
app.MapGet("/", () => "Ok chào bọn m, t đã ở đây được 3 đêm rồi");

// ======================================================================
// 1. KHỞI TẠO FIREBASE ADMIN
// ======================================================================
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

// ======================================================================
// 2. QUÉT HÓA ĐƠN AI
// ======================================================================
app.MapPost("/api/v1/scan-bill", async (HttpRequest request, ScanRequest req, IHttpClientFactory httpClientFactory) =>
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

        var categories = "Ăn uống, Cafe, Mua sắm, Di chuyển, Xăng xe, Hóa đơn, Giải trí, Tiền trọ, Sức khỏe, Giáo dục, Khác";

        // 1. ULTRA-LEAN PROMPT (Tối giản tối đa để né Token Limit)
        var systemPrompt = $"Extract receipt info to JSON. NO markdown. NO extra text.\n" +
                           $"Schema:\n{{\"amount\":0,\"description\":\"\",\"category\":\"Khác\",\"paymentMethod\":\"Cash\"}}\n" +
                           $"Valid Categories: {categories}.\n" +
                           $"Valid Payment: Cash, Card, Bank.";
        var userPrompt = req.OcrText; // Truyền chay vào luôn cho nhẹ

        // 3. PAYLOAD: 
        var payload = new
        {
            //tham khảo thử mấy model khác con nào ngon thì vứt vào chứ openrouter/free hơi hên xui
            //baidu/cobuddy:free
            model = "baidu/qianfan-ocr-fast:free",
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
    catch (FirebaseAuthException authEx)
    {
        return Results.Json(new { error = "TOKEN_INVALID", message = "Xác thực thất bại hoặc phiên đăng nhập hết hạn!" }, statusCode: 401);
    }
    catch (Exception ex)
    {
        return Results.Problem($"Lỗi hệ thống: {ex.Message}", statusCode: 500);
    }
});

// ======================================================================
// 3. TẠO GIAO DỊCH QUỸ CHUNG
// ======================================================================
app.MapPost("/api/v1/workspace/transaction/add", async (HttpRequest request, TransactionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
            return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (body.Amount <= 0) return Results.BadRequest(new { error = "Số tiền không hợp lệ" });
        if (string.IsNullOrEmpty(body.WorkspaceId) || body.WorkspaceId == "PERSONAL")
            return Results.BadRequest(new { error = "API này chỉ dành cho Quỹ chung" });

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound(new { error = "Không tìm thấy Quỹ này" });

        var members = wsSnap.GetValue<List<string>>("members");

        // --- THÊM DÒNG NÀY ĐỂ FIX LỖI CS0103 ---
        var ownerId = wsSnap.GetValue<string>("ownerId");
        // ---------------------------------------

        if (members == null || !members.Contains(uid))
            return Results.StatusCode(403);

        var transId = !string.IsNullOrEmpty(body.Id) ? body.Id : Guid.NewGuid().ToString();
        var transRef = wsRef.Collection("transactions").Document(transId);

        // NẾU LÀ HÀNH ĐỘNG SỬA (Đã có ID từ trước) -> Phải check xem có quyền sửa không!
        if (!string.IsNullOrEmpty(body.Id))
        {
            var existingTransSnap = await transRef.GetSnapshotAsync();
            if (existingTransSnap.Exists)
            {
                var creatorId = existingTransSnap.GetValue<string>("userId");
                // Cấm sửa trộm giao dịch của người khác (Ngoại trừ Owner)
                if (uid != creatorId && uid != ownerId)
                {
                    return Results.StatusCode(403);
                }
            }
        }

        var transactionData = new Dictionary<string, object>
        {
            { "id", transId },
            { "amount", body.Amount },
            { "categoryId", body.CategoryId },
            { "note", body.Note ?? "" },
            { "timestamp", body.Timestamp },
            { "paymentMethod", body.PaymentMethod ?? "Cash" },
            { "type", body.Type },
            { "workspaceId", body.WorkspaceId },
            { "userId", uid },
            { "firestoreCategoryId", body.FirestoreCategoryId ?? "" }
        };

        await transRef.SetAsync(transactionData);
        return Results.Ok(new { message = "Lưu giao dịch Quỹ thành công", id = transId });
    }
    catch (Exception ex)
    {
        return Results.Problem($"Lỗi hệ thống: {ex.Message}");
    }
});

// ======================================================================
// 4. ENDPOINT: TẠO QUỸ CHUNG (KÈM DATA MẪU VÀ LOG)
// ======================================================================
app.MapPost("/api/v1/workspace/create", async (HttpRequest request, WorkspaceCreateRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
            return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.Name)) return Results.BadRequest("Tên quỹ không được để trống");

        var db = FirestoreDb.Create(projectId);
        var batch = db.StartBatch();

        var wsRef = db.Collection("workspaces").Document();
        var workspaceData = new Dictionary<string, object>
        {
            { "name", body.Name },
            { "ownerId", uid },
            { "members", new List<string> { uid } },
            { "type", body.Type ?? "Trống" },
            { "iconName", body.IconName ?? "ic_other" }
        };
        batch.Set(wsRef, workspaceData);

        var catRef = wsRef.Collection("categories");
        string[] defaultNames = { "Food & Dining", "Transport", "Shopping", "Salary", "Bonus" };
        string[] defaultIcons = { "ic_food", "ic_transport", "ic_shopping", "ic_salary", "ic_bonus" };
        string[] defaultColors = { "#FFB74D", "#4FC3F7", "#F06292", "#81C784", "#FFF176" };
        int[] defaultTypes = { 0, 0, 0, 1, 1 };

        for (int i = 0; i < defaultNames.Length; i++)
        {
            var newCatRef = catRef.Document();
            batch.Set(newCatRef, new
            {
                name = defaultNames[i],
                iconName = defaultIcons[i],
                colorCode = defaultColors[i],
                type = defaultTypes[i],
                workspaceId = wsRef.Id,
                isDefault = 1,
                isDeleted = 0
            });
        }

        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new
        {
            actionType = "CREATE_WORKSPACE",
            message = "đã khởi tạo quỹ nhóm này",
            userId = uid,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Tạo Quỹ thành công!", workspaceId = wsRef.Id });
    }
    catch (Exception ex)
    {
        return Results.Problem($"Lỗi hệ thống: {ex.Message}");
    }
});

// ======================================================================
// 5. ENDPOINT: RỜI QUỸ / XÓA QUỸ (KHÔNG KÈM NHƯỢNG QUYỀN)
// ======================================================================
app.MapPost("/api/v1/workspace/leave", async (HttpRequest request, WorkspaceLeaveRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId)) return Results.BadRequest("Thiếu WorkspaceId");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

        var members = wsSnap.GetValue<List<string>>("members");
        var ownerId = wsSnap.GetValue<string>("ownerId");

        if (members == null || !members.Contains(uid)) return Results.StatusCode(403);

        var batch = db.StartBatch();

        // KỊCH BẢN CỦA TRƯỞNG NHÓM (OWNER)
        if (uid == ownerId)
        {
            if (members.Count > 1)
                return Results.BadRequest(new { error = "REQUIRE_TRANSFER", message = "Quỹ vẫn còn thành viên. Vui lòng nhượng quyền trước khi rời đi!" });

            // Quỹ có 1 mình -> Xóa sạch
            string[] subCollections = { "transactions", "categories", "logs", "messages" };
            foreach (var sub in subCollections)
            {
                var subDocs = await wsRef.Collection(sub).GetSnapshotAsync();
                foreach (var doc in subDocs.Documents) batch.Delete(doc.Reference);
            }
            batch.Delete(wsRef);
        }
        // KỊCH BẢN THÀNH VIÊN THƯỜNG
        else
        {
            members.Remove(uid);
            batch.Update(wsRef, "members", members);
            var logRef = wsRef.Collection("logs").Document();
            batch.Set(logRef, new { actionType = "LEAVE_WORKSPACE", message = "đã rời khỏi quỹ", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
        }

        await batch.CommitAsync();
        return Results.Ok(new { message = "Thao tác thành công" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// 5.5 ENDPOINT: NHƯỢNG QUYỀN TRƯỞNG NHÓM
// ======================================================================
app.MapPost("/api/v1/workspace/transfer-owner", async (HttpRequest request, WorkspaceTransferRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.NewOwnerId)) return Results.BadRequest("Thiếu Data");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();
        if (!wsSnap.Exists) return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

        var members = wsSnap.GetValue<List<string>>("members");
        var ownerId = wsSnap.GetValue<string>("ownerId");

        if (uid != ownerId) return Results.StatusCode(403); // Chỉ Owner cũ mới được gọi API này
        if (!members.Contains(body.NewOwnerId)) return Results.BadRequest(new { error = "Người nhận không có trong Quỹ" });

        var batch = db.StartBatch();
        batch.Update(wsRef, "ownerId", body.NewOwnerId);

        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new { actionType = "TRANSFER_OWNER", message = "đã nhường quyền trưởng nhóm", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Nhượng quyền thành công" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// 6. ENDPOINT: ĐUỔI THÀNH VIÊN (KICK MEMBER)
// ======================================================================
app.MapPost("/api/v1/workspace/member/kick", async (HttpRequest request, WorkspaceKickRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
            return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.TargetUid))
            return Results.BadRequest("Thiếu thông tin ID");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

        var ownerId = wsSnap.GetValue<string>("ownerId");
        var members = wsSnap.GetValue<List<string>>("members");

        if (uid != ownerId)
            return Results.StatusCode(403);

        if (body.TargetUid == ownerId)
            return Results.BadRequest(new { error = "Không thể tự kick chính mình." });

        if (members == null || !members.Contains(body.TargetUid))
            return Results.BadRequest(new { error = "Người này không còn trong quỹ." });

        var batch = db.StartBatch();
        members.Remove(body.TargetUid);
        batch.Update(wsRef, "members", members);

        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new
        {
            actionType = "KICK_MEMBER",
            message = "đã bị mời ra khỏi quỹ",
            userId = body.TargetUid,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Đã đuổi thành viên thành công" });
    }
    catch (Exception ex)
    {
        return Results.Problem($"Lỗi hệ thống: {ex.Message}");
    }
});

// ======================================================================
// 7. ENDPOINT: XÓA GIAO DỊCH QUỸ CHUNG (KÈM PHÂN QUYỀN)
// ======================================================================
app.MapPost("/api/v1/workspace/transaction/delete", async (HttpRequest request, TransactionDeleteRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.TransactionId))
            return Results.BadRequest("Thiếu thông tin");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

        var members = wsSnap.GetValue<List<string>>("members");
        var ownerId = wsSnap.GetValue<string>("ownerId");

        if (members == null || !members.Contains(uid)) return Results.StatusCode(403);

        var transRef = wsRef.Collection("transactions").Document(body.TransactionId);
        var transSnap = await transRef.GetSnapshotAsync();

        if (!transSnap.Exists) return Results.NotFound(new { error = "Giao dịch đã bị xóa từ trước" });

        var creatorId = transSnap.GetValue<string>("userId");

        //Không phải tác giả VÀ không phải Owner thì CẤM XÓA
        if (uid != creatorId && uid != ownerId)
        {
            return Results.StatusCode(403);
        }

        var batch = db.StartBatch();
        batch.Delete(transRef); // Trảm!

        // Ghi Log báo cáo hệ thống
        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new
        {
            actionType = "DELETE_TRANSACTION",
            message = "đã xóa một giao dịch",
            userId = uid,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Xóa giao dịch thành công" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// 8. ENDPOINT: XÓA MỀM DANH MỤC CHO OWNER
// ======================================================================
app.MapPost("/api/v1/workspace/category/delete", async (HttpRequest request, CategoryDeleteRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.CategoryId)) return Results.BadRequest("Thiếu Data");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound("Không tìm thấy quỹ");

        // CHỈ TRƯỞNG NHÓM MỚI ĐƯỢC XÓA DANH MỤC
        if (uid != wsSnap.GetValue<string>("ownerId")) return Results.StatusCode(403);

        var batch = db.StartBatch();
        var catRef = wsRef.Collection("categories").Document(body.CategoryId);

        // XÓA MỀM
        batch.Update(catRef, "isDeleted", 1);

        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new { actionType = "DELETE_CATEGORY", message = "đã ẩn một danh mục", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Ẩn danh mục thành công" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// 9. ENDPOINT: THU HỒI TIN NHẮN CHAT (KÈM GOD MODE)
// ======================================================================
app.MapPost("/api/v1/workspace/message/recall", async (HttpRequest request, MessageRecallRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.MessageId)) return Results.BadRequest("Thiếu Data");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound();

        var ownerId = wsSnap.GetValue<string>("ownerId");
        var msgRef = wsRef.Collection("messages").Document(body.MessageId);
        var msgSnap = await msgRef.GetSnapshotAsync();

        if (!msgSnap.Exists) return Results.NotFound("Tin nhắn không tồn tại");

        var senderId = msgSnap.GetValue<string>("senderId");

        // Tác giả HOẶC Trưởng nhóm mới được thu hồi
        if (uid != senderId && uid != ownerId) return Results.StatusCode(403);

        // Đánh dấu là đã thu hồi, giữ nguyên Data để Log
        await msgRef.UpdateAsync("isRecalled", true);

        return Results.Ok(new { message = "Thu hồi tin nhắn thành công" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// 10. ENDPOINT: GỬI LỜI MỜI KẾT BẠN
// ======================================================================
app.MapPost("/api/v1/friend/request", async (HttpRequest request, FriendActionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrEmpty(body.TargetUid) || uid == body.TargetUid) return Results.BadRequest("UID không hợp lệ");

        var db = FirestoreDb.Create(projectId);
        var batch = db.StartBatch();
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        // 1. Ghi vào danh sách Đã gửi của mình
        batch.Set(db.Collection("users").Document(uid).Collection("sent_requests").Document(body.TargetUid), new { timestamp });
        // 2. Ghi vào danh sách Lời mời đến của người kia
        batch.Set(db.Collection("users").Document(body.TargetUid).Collection("friend_requests").Document(uid), new { timestamp });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Gửi lời mời thành công" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

// ======================================================================
// 11. ENDPOINT: ĐỒNG Ý KẾT BẠN
// ======================================================================
app.MapPost("/api/v1/friend/accept", async (HttpRequest request, FriendActionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrEmpty(body.TargetUid)) return Results.BadRequest("UID không hợp lệ");

        var db = FirestoreDb.Create(projectId);
        var batch = db.StartBatch();
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        // 1. Xóa lời mời ở 2 bên
        batch.Delete(db.Collection("users").Document(uid).Collection("friend_requests").Document(body.TargetUid));
        batch.Delete(db.Collection("users").Document(body.TargetUid).Collection("sent_requests").Document(uid));

        // 2. Thêm vào danh sách bạn bè của cả 2 (Ghi 1 phát ăn luôn, không sợ lệch Data)
        batch.Set(db.Collection("users").Document(uid).Collection("friends").Document(body.TargetUid), new { timestamp });
        batch.Set(db.Collection("users").Document(body.TargetUid).Collection("friends").Document(uid), new { timestamp });

        await batch.CommitAsync();
        return Results.Ok(new { message = "Đã trở thành bạn bè" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

// ======================================================================
// 12. ENDPOINT: TỪ CHỐI / HỦY LỜI MỜI / HỦY KẾT BẠN (GỘP CHUNG 3 IN 1)
// ======================================================================
app.MapPost("/api/v1/friend/remove", async (HttpRequest request, FriendActionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrEmpty(body.TargetUid)) return Results.BadRequest("UID không hợp lệ");

        var db = FirestoreDb.Create(projectId);
        var batch = db.StartBatch();

        // Diệt cỏ tận gốc: Quét sạch mọi dấu vết (Bạn bè, Lời mời gửi, Lời mời đến) ở cả 2 người
        string[] collections = { "friends", "friend_requests", "sent_requests" };
        foreach (var col in collections)
        {
            batch.Delete(db.Collection("users").Document(uid).Collection(col).Document(body.TargetUid));
            batch.Delete(db.Collection("users").Document(body.TargetUid).Collection(col).Document(uid));
        }

        await batch.CommitAsync();
        return Results.Ok(new { message = "Thao tác thành công" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

// ======================================================================
// 13. ENDPOINT: CẤP CHỮ KÝ CLOUDINARY
// ======================================================================
app.MapGet("/api/v1/cloudinary/sign", async (HttpRequest request) =>
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


// ======================================================================
// CHẠY SERVER - Lệnh này PHẢI nằm ngay trước các Model
// ======================================================================
app.Run("http://0.0.0.0:5283");

// ======================================================================
// TẤT CẢ CÁC MODEL (CLASSES, RECORDS) PHẢI NẰM DƯỚI NÀY
// ======================================================================
public record ScanRequest(string OcrText);

public class TransactionRequest
{
    public string? Id { get; set; }
    public long Amount { get; set; }
    public int CategoryId { get; set; }
    public string? Note { get; set; }
    public long Timestamp { get; set; }
    public string? PaymentMethod { get; set; }
    public int Type { get; set; }
    public string? WorkspaceId { get; set; }
    public string? FirestoreCategoryId { get; set; }
}

public class WorkspaceCreateRequest
{
    public string? Name { get; set; }
    public string? Type { get; set; }
    public string? IconName { get; set; }
}

public class WorkspaceLeaveRequest
{
    public string? WorkspaceId { get; set; }
    public string? NewOwnerId { get; set; }
}

public class WorkspaceKickRequest
{
    public string? WorkspaceId { get; set; }
    public string? TargetUid { get; set; }
}

public class WorkspaceTransferRequest
{
    public string? WorkspaceId { get; set; }
    public string? NewOwnerId { get; set; }
}
public class TransactionDeleteRequest
{
    public string? WorkspaceId { get; set; }
    public string? TransactionId { get; set; }
}
public class CategoryDeleteRequest
{
    public string? WorkspaceId { get; set; }
    public string? CategoryId { get; set; }
}
public class MessageRecallRequest
{
    public string? WorkspaceId { get; set; }
    public string? MessageId { get; set; }
}
public class FriendActionRequest
{
    public string? TargetUid { get; set; }
}

