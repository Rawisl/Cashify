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
            model = "baidu/cobuddy:free",
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
// 3. TẠO GIAO DỊCH QUỸ CHUNG KÈM LOGIC SỬA
// ======================================================================
app.MapPost("/api/v1/workspace/transaction/add", async (HttpRequest request, TransactionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;
        string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "A member";

        if (body.Amount <= 0) return Results.BadRequest(new { error = "invalid budget" });
        if (string.IsNullOrEmpty(body.WorkspaceId) || body.WorkspaceId == "PERSONAL") return Results.BadRequest(new { error = "This API is for Group fund" });

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();
        if (!wsSnap.Exists) return Results.NotFound(new { error = "Not found group" });

        var members = wsSnap.GetValue<List<string>>("members");
        var ownerId = wsSnap.GetValue<string>("ownerId");

        if (members == null || !members.Contains(uid)) return Results.StatusCode(403);

        var transId = !string.IsNullOrEmpty(body.Id) ? body.Id : Guid.NewGuid().ToString();
        var transRef = wsRef.Collection("transactions").Document(transId);

        if (!string.IsNullOrEmpty(body.Id))
        {
            var existingTransSnap = await transRef.GetSnapshotAsync();
            if (existingTransSnap.Exists)
            {
                var creatorId = existingTransSnap.GetValue<string>("userId");
                if (uid != creatorId && uid != ownerId) return Results.StatusCode(403);
            }
        }

        var transactionData = new Dictionary<string, object>
        {
            { "id", transId }, { "amount", body.Amount }, { "categoryId", body.CategoryId },
            { "note", body.Note ?? "" }, { "timestamp", body.Timestamp }, { "paymentMethod", body.PaymentMethod ?? "Cash" },
            { "type", body.Type }, { "workspaceId", body.WorkspaceId }, { "userId", uid }, { "firestoreCategoryId", body.FirestoreCategoryId ?? "" }
        };

        var batch = db.StartBatch();
        batch.Set(transRef, transactionData);

        // BẮN THÔNG BÁO CHO CÁC THÀNH VIÊN KHÁC
        string actionName = string.IsNullOrEmpty(body.Id) ? "Adding" : "Updating";
        foreach (var memberId in members)
        {
            if (memberId != uid) // Không tự gửi thông báo cho chính mình
            {
                var notifRef = db.Collection("users").Document(memberId).Collection("notifications").Document();
                batch.Set(notifRef, new
                {
                    type = "WORKSPACE_TRANS",
                    title = "Group fund fluctation",
                    message = $"{senderName} has just {actionName} a transaction in group fund.",
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    isRead = false,
                    referenceId = body.WorkspaceId // Bấm vào sẽ bay sang trang Quỹ này
                });
            }
        }

        await batch.CommitAsync();
        return Results.Ok(new { message = "Lưu giao dịch Quỹ thành công", id = transId });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
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
        string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "A member";

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.TransactionId)) return Results.BadRequest("Data lacking");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();
        if (!wsSnap.Exists) return Results.NotFound(new { error = "Not found group" });

        var members = wsSnap.GetValue<List<string>>("members");
        var ownerId = wsSnap.GetValue<string>("ownerId");

        if (members == null || !members.Contains(uid)) return Results.StatusCode(403);

        var transRef = wsRef.Collection("transactions").Document(body.TransactionId);
        var transSnap = await transRef.GetSnapshotAsync();
        if (!transSnap.Exists) return Results.NotFound(new { error = "Transaction deleted before" });

        var creatorId = transSnap.GetValue<string>("userId");
        if (uid != creatorId && uid != ownerId) return Results.StatusCode(403);

        var batch = db.StartBatch();
        batch.Delete(transRef);

        var logRef = wsRef.Collection("logs").Document();
        batch.Set(logRef, new { actionType = "DELETE_TRANSACTION", message = "has delete a transaction", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

        // BẮN THÔNG BÁO CHO CÁC THÀNH VIÊN KHÁC 
        foreach (var memberId in members)
        {
            if (memberId != uid)
            {
                var notifRef = db.Collection("users").Document(memberId).Collection("notifications").Document();
                batch.Set(notifRef, new
                {
                    type = "WORKSPACE_TRANS",
                    title = "Transcation deleted",
                    message = $"{senderName} has just deleted a transaction from group fund.",
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    isRead = false,
                    referenceId = body.WorkspaceId
                });
            }
        }

        await batch.CommitAsync();
        return Results.Ok(new { message = "Deleted transaction successflly" });
    }
    catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
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

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;
        string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "Unknown user";

        if (string.IsNullOrEmpty(body.TargetUid) || uid == body.TargetUid) return Results.BadRequest("UID invalid");

        var db = FirestoreDb.Create(projectId);
        var currentUserRef = db.Collection("users").Document(uid);
        var targetUserRef = db.Collection("users").Document(body.TargetUid);
        var targetUserSnap = await targetUserRef.GetSnapshotAsync();

        if (!targetUserSnap.Exists)
            return Results.NotFound(new { message = "Không tìm thấy người dùng nhận lời mời" });

        var existingFriendSnap = await currentUserRef.Collection("friends").Document(body.TargetUid).GetSnapshotAsync();
        if (existingFriendSnap.Exists)
            return Results.Conflict(new { message = "Người này đã là bạn bè của bạn rồi" });

        var existingSentRequestSnap = await currentUserRef.Collection("sent_requests").Document(body.TargetUid).GetSnapshotAsync();
        if (existingSentRequestSnap.Exists)
            return Results.Conflict(new { message = "Bạn đã gửi lời mời cho người này rồi" });

        var incomingRequestSnap = await currentUserRef.Collection("friend_requests").Document(body.TargetUid).GetSnapshotAsync();
        if (incomingRequestSnap.Exists)
            return Results.Conflict(new { message = "Người này đã gửi lời mời cho bạn" });

        var batch = db.StartBatch();
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        var sentRequestData = new Dictionary<string, object>
        {
            { "toUid", body.TargetUid },
            { "timestamp", timestamp }
        };

        var incomingRequestData = new Dictionary<string, object>
        {
            { "fromUid", uid },
            { "timestamp", timestamp }
        };

        // Google.Cloud.Firestore serializes dictionaries reliably here; anonymous objects can fail at runtime.
        batch.Set(currentUserRef.Collection("sent_requests").Document(body.TargetUid), sentRequestData);
        batch.Set(targetUserRef.Collection("friend_requests").Document(uid), incomingRequestData);

        // BẮN THÔNG BÁO KẾT BẠN
        var notifRef = db.Collection("users").Document(body.TargetUid).Collection("notifications").Document();
        batch.Set(notifRef, new
        {
            type = "FRIEND_REQUEST",
            title = "Friend request",
            message = $"{senderName} has sent you a friend request.",
            timestamp = timestamp,
            isRead = false,
            referenceId = uid // ID người gửi
        });

        await batch.CommitAsync();
        return Results.Ok(new { message = "sent request successfully" });
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Friend request failed: {ex}");
        return Results.Problem(
            title: "Gửi lời mời thất bại",
            detail: ex.Message,
            statusCode: 500);
    }
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
        if (string.IsNullOrEmpty(body.TargetUid)) return Results.BadRequest("UID invalid");

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
        return Results.Ok(new { message = "successfully" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

// ======================================================================
// 13. ENDPOINT: GỢI Ý KẾT BẠN CHỈ GỒM NGƯỜI CHƯA CÓ QUAN HỆ
// ======================================================================
app.MapGet("/api/v1/friend/suggestions", async (HttpRequest request) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        var db = FirestoreDb.Create(projectId);
        var currentUserRef = db.Collection("users").Document(uid);

        var friendIds = (await currentUserRef.Collection("friends").GetSnapshotAsync()).Documents.Select(doc => doc.Id);
        var sentIds = (await currentUserRef.Collection("sent_requests").GetSnapshotAsync()).Documents.Select(doc => doc.Id);
        var incomingIds = (await currentUserRef.Collection("friend_requests").GetSnapshotAsync()).Documents.Select(doc => doc.Id);
        var excludedIds = new HashSet<string>(friendIds.Concat(sentIds).Concat(incomingIds)) { uid };

        var usersSnapshot = await db.Collection("users").GetSnapshotAsync();
        var suggestions = usersSnapshot.Documents
            .Where(doc => !excludedIds.Contains(doc.Id))
            .Select(doc => new
            {
                uid = doc.ContainsField("uid") ? doc.GetValue<string>("uid") : doc.Id,
                email = doc.ContainsField("email") ? doc.GetValue<string>("email") : "",
                displayName = doc.ContainsField("displayName") ? doc.GetValue<string>("displayName") : "",
                avatarUrl = doc.ContainsField("avatarUrl") ? doc.GetValue<string>("avatarUrl") : "",
                phoneNumber = doc.ContainsField("phoneNumber") ? doc.GetValue<string>("phoneNumber") : ""
            })
            .Where(user => !string.IsNullOrWhiteSpace(user.uid))
            .ToList();

        return Results.Ok(suggestions);
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Friend suggestions failed: {ex}");
        return Results.Problem(
            title: "Tải gợi ý kết bạn thất bại",
            detail: ex.Message,
            statusCode: 500);
    }
});

// ======================================================================
// 14. ENDPOINT: DANH SÁCH BẠN BÈ CÓ THỂ NHẮN TIN
// ======================================================================
app.MapGet("/api/v1/friend/messages/chats", async (HttpRequest request) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        var db = FirestoreDb.Create(projectId);
        var friendIds = (await db.Collection("users").Document(uid).Collection("friends").GetSnapshotAsync())
            .Documents
            .Select(doc => doc.Id)
            .ToList();

        if (friendIds.Count == 0) return Results.Ok(Array.Empty<object>());

        var users = new List<object>();
        foreach (var friendId in friendIds)
        {
            var friendSnap = await db.Collection("users").Document(friendId).GetSnapshotAsync();
            if (!friendSnap.Exists) continue;
            users.Add(new
            {
                uid = friendSnap.ContainsField("uid") ? friendSnap.GetValue<string>("uid") : friendId,
                email = friendSnap.ContainsField("email") ? friendSnap.GetValue<string>("email") : "",
                displayName = friendSnap.ContainsField("displayName") ? friendSnap.GetValue<string>("displayName") : "",
                avatarUrl = friendSnap.ContainsField("avatarUrl") ? friendSnap.GetValue<string>("avatarUrl") : "",
                phoneNumber = friendSnap.ContainsField("phoneNumber") ? friendSnap.GetValue<string>("phoneNumber") : ""
            });
        }

        return Results.Ok(users);
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Friend chat list failed: {ex}");
        return Results.Problem(title: "Tải danh sách trò chuyện thất bại", detail: ex.Message, statusCode: 500);
    }
});

// ======================================================================
// 15. ENDPOINT: DANH SÁCH CUỘC TRÒ CHUYỆN GẦN ĐÂY
// ======================================================================
app.MapGet("/api/v1/friend/messages/conversations", async (HttpRequest request) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        var db = FirestoreDb.Create(projectId);
        var friendIds = (await db.Collection("users").Document(uid).Collection("friends").GetSnapshotAsync())
            .Documents
            .Select(doc => doc.Id)
            .ToList();

        var conversations = new List<DirectConversationSummary>();
        foreach (var friendId in friendIds)
        {
            var orderedIds = new[] { uid, friendId }.OrderBy(id => id, StringComparer.Ordinal).ToArray();
            var chatId = $"{orderedIds[0]}_{orderedIds[1]}";
            var messagesRef = db.Collection("direct_chats").Document(chatId).Collection("messages");
            var latestSnapshot = await messagesRef.OrderByDescending("timestamp").Limit(1).GetSnapshotAsync();
            if (latestSnapshot.Count == 0) continue;

            var friendSnap = await db.Collection("users").Document(friendId).GetSnapshotAsync();
            if (!friendSnap.Exists) continue;

            var latest = latestSnapshot.Documents[0];
            var unreadSnapshot = await messagesRef
                .WhereEqualTo("receiverId", uid)
                .WhereEqualTo("isRead", false)
                .GetSnapshotAsync();

            conversations.Add(new DirectConversationSummary(
                            FriendUid: friendSnap.ContainsField("uid") ? friendSnap.GetValue<string>("uid") : friendId,
                            FriendEmail: friendSnap.ContainsField("email") ? friendSnap.GetValue<string>("email") : "",
                            FriendDisplayName: friendSnap.ContainsField("displayName") ? friendSnap.GetValue<string>("displayName") : "",
                            FriendAvatarUrl: friendSnap.ContainsField("avatarUrl") ? friendSnap.GetValue<string>("avatarUrl") : "",
                            LatestMessageText: latest.ContainsField("text") ? latest.GetValue<string>("text") : "",
                            LatestMessageTimestamp: latest.ContainsField("timestamp") ? latest.GetValue<long>("timestamp") : 0,
                            UnreadCount: unreadSnapshot.Count
                        ));
        }

        return Results.Ok(conversations.OrderByDescending(item => item.LatestMessageTimestamp));
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Direct conversation list failed: {ex}");
        return Results.Problem(title: "Tải danh sách trò chuyện thất bại", detail: ex.Message, statusCode: 500);
    }
});

// ======================================================================
// 16. ENDPOINT: TẢI TIN NHẮN TRỰC TIẾP VỚI MỘT NGƯỜI BẠN
// ======================================================================
app.MapGet("/api/v1/friend/messages/{friendUid}", async (HttpRequest request, string friendUid) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
        {
            Console.Error.WriteLine("Load direct friend messages rejected: missing or invalid bearer token");
            return Results.Unauthorized();
        }

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrWhiteSpace(friendUid) || uid == friendUid)
            return Results.BadRequest(new { message = "Người nhận không hợp lệ" });

        var db = FirestoreDb.Create(projectId);
        var senderFriendSnap = await db.Collection("users").Document(uid).Collection("friends").Document(friendUid).GetSnapshotAsync();
        var receiverFriendSnap = await db.Collection("users").Document(friendUid).Collection("friends").Document(uid).GetSnapshotAsync();
        if (!senderFriendSnap.Exists || !receiverFriendSnap.Exists)
        {
            Console.Error.WriteLine($"Load direct friend messages rejected: users are not mutual friends ({uid}, {friendUid})");
            return Results.Json(new { message = "Chỉ bạn bè mới có thể xem tin nhắn với nhau" }, statusCode: 403);
        }

        var orderedIds = new[] { uid, friendUid }.OrderBy(id => id, StringComparer.Ordinal).ToArray();
        var chatId = $"{orderedIds[0]}_{orderedIds[1]}";
        var messagesSnapshot = await db.Collection("direct_chats")
            .Document(chatId)
            .Collection("messages")
            .OrderBy("timestamp")
            .GetSnapshotAsync();

        var messages = messagesSnapshot.Documents.Select(doc => new
        {
            messageId = doc.Id,
            senderId = doc.ContainsField("senderId") ? doc.GetValue<string>("senderId") : "",
            receiverId = doc.ContainsField("receiverId") ? doc.GetValue<string>("receiverId") : "",
            senderName = doc.ContainsField("senderName") ? doc.GetValue<string>("senderName") : "",
            senderAvatar = doc.ContainsField("senderAvatar") ? doc.GetValue<string>("senderAvatar") : "",
            text = doc.ContainsField("text") ? doc.GetValue<string>("text") : "",
            timestamp = doc.ContainsField("timestamp") ? doc.GetValue<long>("timestamp") : 0,
            isRead = doc.ContainsField("isRead") && doc.GetValue<bool>("isRead"),
            isRecalled = doc.ContainsField("isRecalled") && doc.GetValue<bool>("isRecalled")
        });

        return Results.Ok(messages);
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Load direct friend messages failed: {ex}");
        return Results.Problem(title: "Tải tin nhắn thất bại", detail: ex.Message, statusCode: 500);
    }
});

// ======================================================================
// 17. ENDPOINT: GỬI TIN NHẮN TRỰC TIẾP CHO BẠN BÈ
// ======================================================================
app.MapPost("/api/v1/friend/message/send", async (HttpRequest request, DirectFriendMessageRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
        {
            Console.Error.WriteLine("Send direct friend message rejected: missing or invalid bearer token");
            return Results.Unauthorized();
        }

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrWhiteSpace(body.ReceiverId) || uid == body.ReceiverId)
            return Results.BadRequest(new { message = "Người nhận không hợp lệ" });
        if (string.IsNullOrWhiteSpace(body.Text))
            return Results.BadRequest(new { message = "Tin nhắn không được để trống" });

        var db = FirestoreDb.Create(projectId);
        var senderRef = db.Collection("users").Document(uid);
        var receiverRef = db.Collection("users").Document(body.ReceiverId);
        var senderSnap = await senderRef.GetSnapshotAsync();
        var receiverSnap = await receiverRef.GetSnapshotAsync();

        if (!senderSnap.Exists || !receiverSnap.Exists)
            return Results.NotFound(new { message = "Không tìm thấy người dùng" });

        var senderFriendSnap = await senderRef.Collection("friends").Document(body.ReceiverId).GetSnapshotAsync();
        var receiverFriendSnap = await receiverRef.Collection("friends").Document(uid).GetSnapshotAsync();
        if (!senderFriendSnap.Exists || !receiverFriendSnap.Exists)
        {
            Console.Error.WriteLine($"Send direct friend message rejected: users are not mutual friends ({uid}, {body.ReceiverId})");
            return Results.Json(new { message = "Chỉ bạn bè mới có thể nhắn tin cho nhau" }, statusCode: 403);
        }

        var orderedIds = new[] { uid, body.ReceiverId }.OrderBy(id => id, StringComparer.Ordinal).ToArray();
        var chatId = $"{orderedIds[0]}_{orderedIds[1]}";
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var senderName = senderSnap.ContainsField("displayName") ? senderSnap.GetValue<string>("displayName") : "User";
        var senderAvatar = senderSnap.ContainsField("avatarUrl") ? senderSnap.GetValue<string>("avatarUrl") : "";

        var messageData = new Dictionary<string, object>
        {
            { "senderId", uid },
            { "receiverId", body.ReceiverId },
            { "senderName", senderName ?? "User" },
            { "senderAvatar", senderAvatar ?? "" },
            { "text", body.Text.Trim() },
            { "timestamp", timestamp },
            { "isRead", false },
            { "isRecalled", false }
        };

        await db.Collection("direct_chats")
            .Document(chatId)
            .Collection("messages")
            .Document()
            .SetAsync(messageData);

        return Results.Ok(new { message = "Gửi tin nhắn thành công", chatId });
    }
    catch (Exception ex)
    {
        Console.Error.WriteLine($"Direct friend message failed: {ex}");
        return Results.Problem(
            title: "Gửi tin nhắn thất bại",
            detail: ex.Message,
            statusCode: 500);
    }
});

// ======================================================================
// 18. ENDPOINT: CẤP CHỮ KÝ CLOUDINARY
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
// 14. ENDPOINT: GỬI LỜI MỜI VÀO QUỸ (WORKSPACE INVITATIONS)
// ======================================================================
app.MapPost("/api/v1/workspace/invite/send", async (HttpRequest request, WorkspaceInviteSendRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
        var uid = decodedToken.Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || body.TargetUids == null || body.TargetUids.Count == 0)
            return Results.BadRequest("Data lacking");

        var db = FirestoreDb.Create(projectId);
        var batch = db.StartBatch();
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();
        if (!wsSnap.Exists) return Results.NotFound(new { error = "Not found group" });

        var ownerId = wsSnap.GetValue<string>("ownerId");
        if (uid != ownerId) return Results.StatusCode(403);

        string inviterName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "Your friend";
        string inviterAvatar = decodedToken.Claims.TryGetValue("picture", out var picObj) ? picObj.ToString()! : "";

        foreach (var targetUid in body.TargetUids)
        {
            var inviteRef = db.Collection("users").Document(targetUid).Collection("workspace_invitations").Document(body.WorkspaceId);
            batch.Set(inviteRef, new
            {
                workspaceId = body.WorkspaceId,
                workspaceName = body.WorkspaceName ?? "group",
                inviterName = inviterName,
                inviterAvatar = inviterAvatar,
                timestamp = timestamp
            });

            // BẮN THÔNG BÁO MỜI VÀO QUỸ
            var notifRef = db.Collection("users").Document(targetUid).Collection("notifications").Document();
            batch.Set(notifRef, new
            {
                type = "WORKSPACE_INVITE",
                title = "Invite to group",
                message = $"{inviterName} has invited you to group fund: {(body.WorkspaceName ?? "group")}.",
                timestamp = timestamp,
                isRead = false,
                referenceId = body.WorkspaceId
            });
        }

        await batch.CommitAsync();
        return Results.Ok(new { message = "Invitation sent succesfully!" });
    }
    catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
});

// ======================================================================
// 15. ENDPOINT: ĐỒNG Ý LỜI MỜI
// ======================================================================
app.MapPost("/api/v1/workspace/invite/accept", async (HttpRequest request, WorkspaceInviteHandleRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.InvitationId)) return Results.BadRequest("Data lacking");

        var db = FirestoreDb.Create(projectId);

        var inviteRef = db.Collection("users").Document(uid).Collection("workspace_invitations").Document(body.InvitationId);

        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists)
        {
            await inviteRef.DeleteAsync();
            return Results.BadRequest(new { error = "WORKSPACE_DELETED", message = "Group has been deleted or not existed" });
        }

        var batch = db.StartBatch();

        batch.Update(wsRef, "members", FieldValue.ArrayUnion(uid));

        batch.Delete(inviteRef);

        await batch.CommitAsync();
        return Results.Ok(new { message = "Join group successfully!" });
    }
    catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
});

// ======================================================================
// 16. ENDPOINT: TỪ CHỐI LỜI MỜI
// ======================================================================
app.MapPost("/api/v1/workspace/invite/decline", async (HttpRequest request, WorkspaceInviteHandleRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
        if (string.IsNullOrEmpty(body.InvitationId)) return Results.BadRequest("Data lacking");

        var db = FirestoreDb.Create(projectId);

        // Chỉ việc xé giấy mời vứt đi
        var inviteRef = db.Collection("users").Document(uid).Collection("workspace_invitations").Document(body.InvitationId);
        await inviteRef.DeleteAsync();

        return Results.Ok(new { message = "Rejected the invitation." });
    }
    catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
});

// CÁC ENDPOINT LIÊN QUAN ĐẾN POST
//load các bài đăng
app.MapPost("/api/v1/post/feed", async (HttpRequest request, FeedRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid; // LẤY UID ĐỂ CHECK LIKE

        var db = FirestoreDb.Create(projectId);
        Query query = db.Collection("posts");

        if (body.FriendIds != null && body.FriendIds.Count > 0)
        {
            var limitedFriends = body.FriendIds.Take(30).ToList();
            query = query.WhereIn("userId", limitedFriends);
        }

        query = query.OrderByDescending("timestamp");
        if (body.LastTimestamp > 0) query = query.StartAfter(body.LastTimestamp);
        query = query.Limit(body.Limit > 0 ? body.Limit : 10);

        var snapshot = await query.GetSnapshotAsync();

        // CHẠY SONG SONG KỂM TRA LIKE CHO 10 BÀI POST CÙNG LÚC
        var likeCheckTasks = snapshot.Documents.Select(async doc =>
        {
            var postDict = doc.ToDictionary();

            // Chọc vào sub-collection likes để xem uid này có tồn tại không
            var likeSnap = await db.Collection("posts").Document(doc.Id).Collection("likes").Document(uid).GetSnapshotAsync();

            // Gắn thêm trường isLiked cho Android
            postDict["isLiked"] = likeSnap.Exists;

            return postDict;
        });

        var posts = (await Task.WhenAll(likeCheckTasks)).ToList();

        return Results.Ok(posts);
    }
    catch (Exception ex) { return Results.Problem($"Lỗi lấy feed: {ex.Message}"); }
});

//load bình luận của 1 bài viết
app.MapGet("/api/v1/post/{postId}/comments", async (HttpRequest request, string postId) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var db = FirestoreDb.Create(projectId);
        var commentsRef = db.Collection("posts").Document(postId).Collection("comments");

        // Load comment xếp theo thời gian cũ nhất lên trước (như Facebook)
        var snapshot = await commentsRef.OrderBy("timestamp").GetSnapshotAsync();

        var comments = snapshot.Documents.Select(doc => doc.ToDictionary()).ToList();
        return Results.Ok(comments);
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//load bài viết tường nhà của riêng 1 user (chỉ load bài của riêng nó)
app.MapGet("/api/v1/post/wall/{targetUid}", async (HttpRequest request, string targetUid, int limit = 10, long lastTimestamp = 0) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        // Lấy UID của người ĐANG XEM để check xem nó đã like bài trên tường này chưa
        var token = authHeader.Substring("Bearer ".Length);
        var currentViewerUid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        Query query = db.Collection("posts").WhereEqualTo("userId", targetUid).OrderByDescending("timestamp");

        if (lastTimestamp > 0) query = query.StartAfter(lastTimestamp);
        query = query.Limit(limit);

        var snapshot = await query.GetSnapshotAsync();

        // ỐP LOGIC CHECK LIKE SONG SONG Y HỆT BÊN FEED VÀO ĐÂY
        var likeCheckTasks = snapshot.Documents.Select(async doc =>
        {
            var postDict = doc.ToDictionary();
            var likeSnap = await db.Collection("posts").Document(doc.Id).Collection("likes").Document(currentViewerUid).GetSnapshotAsync();
            postDict["isLiked"] = likeSnap.Exists;
            return postDict;
        });

        var posts = (await Task.WhenAll(likeCheckTasks)).ToList();

        return Results.Ok(posts);
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//tạo bài
app.MapPost("/api/v1/post/create", async (HttpRequest request, CreatePostRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        if (string.IsNullOrEmpty(body.Content)) return Results.BadRequest("Nội dung không được để trống");

        var db = FirestoreDb.Create(projectId);
        var postId = Guid.NewGuid().ToString();
        var postRef = db.Collection("posts").Document(postId);

        var postData = new Dictionary<string, object>
        {
            { "postId", postId },
            { "userId", uid },
            { "type", body.Type ?? "USER_POST" },
            { "content", body.Content },
            { "imageUrl", body.ImageUrl ?? "" },
            { "milestoneData", body.MilestoneData },
            { "likeCount", 0 },
            { "commentCount", 0 },
            { "timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() },
            { "isEdited", false }
        };

        await postRef.SetAsync(postData);
        return Results.Ok(new { message = "Đăng bài thành công", postId });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi tạo bài: {ex.Message}"); }
});

//thả chim
app.MapPost("/api/v1/post/like", async (HttpRequest request, LikeActionRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var postRef = db.Collection("posts").Document(body.PostId);
        var likeRef = postRef.Collection("likes").Document(uid);

        // Dùng Transaction để đảm bảo tính toán số lượng Tim chuẩn xác 100%
        await db.RunTransactionAsync(async transaction =>
        {
            DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
            if (!postSnap.Exists) throw new Exception("Bài viết không tồn tại");

            DocumentSnapshot likeSnap = await transaction.GetSnapshotAsync(likeRef);
            long currentLikes = postSnap.GetValue<long>("likeCount");

            if (likeSnap.Exists)
            {
                // Đã like rồi -> Giờ là UNLIKE
                transaction.Delete(likeRef);
                transaction.Update(postRef, "likeCount", Math.Max(0, currentLikes - 1));
            }
            else
            {
                // Chưa like -> Giờ là LIKE
                transaction.Set(likeRef, new Dictionary<string, object> { { "likedAt", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() } });
                transaction.Update(postRef, "likeCount", currentLikes + 1);
            }
        });

        return Results.Ok(new { message = "Thao tác Like/Unlike thành công" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//sửa bài
app.MapPost("/api/v1/post/edit", async (HttpRequest request, EditPostRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var postRef = db.Collection("posts").Document(body.PostId);
        var postSnap = await postRef.GetSnapshotAsync();

        if (!postSnap.Exists) return Results.NotFound("Bài viết không tồn tại");
        if (postSnap.GetValue<string>("userId") != uid) return Results.StatusCode(403); // Cấm sửa trộm

        var updates = new Dictionary<string, object>
        {
            { "content", body.NewContent },
            { "isEdited", true }
        };
        // Nếu có update ảnh mới thì lưu, không thì giữ nguyên ảnh cũ
        if (body.NewImageUrl != null) updates["imageUrl"] = body.NewImageUrl;

        await postRef.UpdateAsync(updates);
        return Results.Ok(new { message = "Đã sửa bài viết" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//xóa bài viết (Dọn sạch cả Sub-collection để chống rác Data)
app.MapPost("/api/v1/post/delete", async (HttpRequest request, DeletePostRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var postRef = db.Collection("posts").Document(body.PostId);
        var postSnap = await postRef.GetSnapshotAsync();

        if (!postSnap.Exists) return Results.NotFound("Bài viết không tồn tại");
        if (postSnap.GetValue<string>("userId") != uid) return Results.StatusCode(403); // Cấm xóa trộm

        // Khởi tạo xe rác (Batch) để gom lệnh xóa
        var batch = db.StartBatch();

        // 1. Quét dọn collection "likes"
        var likesSnap = await postRef.Collection("likes").GetSnapshotAsync();
        foreach (var doc in likesSnap.Documents)
        {
            batch.Delete(doc.Reference);
        }

        // 2. Quét dọn collection "comments"
        var commentsSnap = await postRef.Collection("comments").GetSnapshotAsync();
        foreach (var doc in commentsSnap.Documents)
        {
            batch.Delete(doc.Reference);
        }

        // 3. Đập bỏ Document gốc (Post)
        batch.Delete(postRef);

        // 4. Bấm nút hủy diệt cùng lúc toàn bộ
        await batch.CommitAsync();

        return Results.Ok(new { message = "Đã xóa bài viết và các dữ liệu liên quan" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});


//ENDPOINT CỦA COMMENT
//add bình luận
app.MapPost("/api/v1/comment/add", async (HttpRequest request, AddCommentRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var postRef = db.Collection("posts").Document(body.PostId);
        var commentId = Guid.NewGuid().ToString();
        var commentRef = postRef.Collection("comments").Document(commentId);

        await db.RunTransactionAsync(async transaction =>
        {
            DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
            if (!postSnap.Exists) throw new Exception("Bài viết không tồn tại");

            var commentData = new Dictionary<string, object>
            {
                { "commentId", commentId },
                { "userId", uid },
                { "content", body.Content },
                { "timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() },
                { "isEdited", false }
            };

            transaction.Set(commentRef, commentData);
            long currentComments = postSnap.GetValue<long>("commentCount");
            transaction.Update(postRef, "commentCount", currentComments + 1);
        });

        return Results.Ok(new { message = "Bình luận thành công", commentId });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//sửa bình luận
app.MapPost("/api/v1/comment/edit", async (HttpRequest request, EditCommentRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var commentRef = db.Collection("posts").Document(body.PostId).Collection("comments").Document(body.CommentId);
        var commentSnap = await commentRef.GetSnapshotAsync();

        if (!commentSnap.Exists) return Results.NotFound("Bình luận không tồn tại");
        if (commentSnap.GetValue<string>("userId") != uid) return Results.StatusCode(403);

        await commentRef.UpdateAsync(new Dictionary<string, object>
        {
            { "content", body.NewContent },
            { "isEdited", true }
        });

        return Results.Ok(new { message = "Đã sửa bình luận" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//xóa bình luận
app.MapPost("/api/v1/comment/delete", async (HttpRequest request, DeleteCommentRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        var db = FirestoreDb.Create(projectId);
        var postRef = db.Collection("posts").Document(body.PostId);
        var commentRef = postRef.Collection("comments").Document(body.CommentId);

        await db.RunTransactionAsync(async transaction =>
        {
            var postSnap = await transaction.GetSnapshotAsync(postRef);
            var commentSnap = await transaction.GetSnapshotAsync(commentRef);

            if (!postSnap.Exists || !commentSnap.Exists) throw new Exception("Không tìm thấy dữ liệu");

            var postOwnerId = postSnap.GetValue<string>("userId");
            var commentOwnerId = commentSnap.GetValue<string>("userId");

            // Chỉ chủ comment hoặc chủ bài viết mới có quyền xóa
            if (uid != commentOwnerId && uid != postOwnerId) throw new Exception("Không có quyền xóa");

            transaction.Delete(commentRef);

            // Giảm số đếm comment
            long currentComments = postSnap.GetValue<long>("commentCount");
            transaction.Update(postRef, "commentCount", Math.Max(0, currentComments - 1));
        });

        return Results.Ok(new { message = "Đã xóa bình luận" });
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//bulk profile fetch (ý tưởng cái này đại loại phục vụ cho Client-side Join. Khi tải 10 bài post, Andr gom 10 userId (loại bỏ trùng lặp), bắn lên rồi gọi API backend này để lấy về tên + avt của những người đó nhét vào cache trong ViewModel)
app.MapPost("/api/v1/user/batch-profiles", async (HttpRequest request, BatchProfileRequest body) =>
{
    try
    {
        if (body.UserIds == null || body.UserIds.Count == 0) return Results.Ok(new Dictionary<string, object>());

        var db = FirestoreDb.Create(projectId);
        var profiles = new Dictionary<string, object>();

        // Lọc trùng ID trước khi đi tìm trong Database để tối ưu hiệu năng
        var uniqueUids = body.UserIds.Distinct().ToList();

        foreach (var uid in uniqueUids)
        {
            var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
            if (userSnap.Exists)
            {
                profiles[uid] = new
                {
                    displayName = userSnap.ContainsField("displayName") ? userSnap.GetValue<string>("displayName") : "Người dùng Cashify",
                    avatarUrl = userSnap.ContainsField("avatarUrl") ? userSnap.GetValue<string>("avatarUrl") : ""
                };
            }
        }

        return Results.Ok(profiles);
    }
    catch (Exception ex) { return Results.Problem(ex.Message); }
});

//sửa danh mục cho Owner
app.MapPost("/api/v1/workspace/category/edit", async (HttpRequest request, EditCategoryRequest body) =>
{
    try
    {
        var authHeader = request.Headers["Authorization"].FirstOrDefault();
        if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer ")) return Results.Unauthorized();

        var token = authHeader.Substring("Bearer ".Length);
        var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

        if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.CategoryId) || string.IsNullOrEmpty(body.Name))
            return Results.BadRequest("Thiếu thông tin chỉnh sửa");

        var db = FirestoreDb.Create(projectId);
        var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
        var wsSnap = await wsRef.GetSnapshotAsync();

        if (!wsSnap.Exists) return Results.NotFound("Không tìm thấy quỹ");

        // Bảo mật: Chỉ Trưởng nhóm (Owner) mới được quyền chỉnh sửa cấu trúc danh mục nhóm
        if (uid != wsSnap.GetValue<string>("ownerId")) return Results.StatusCode(403);

        var catRef = wsRef.Collection("categories").Document(body.CategoryId);

        await catRef.UpdateAsync(new Dictionary<string, object>
        {
            { "name", body.Name },
            { "iconName", body.IconName },
            { "colorCode", body.ColorCode },
            { "type", body.Type }
        });

        return Results.Ok(new { message = "Cập nhật danh mục thành công!" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

//khôi phục danh mục đã ẩn (isDeleted = 0)
app.MapPost("/api/v1/workspace/category/restore", async (HttpRequest request, RestoreCategoryRequest body) =>
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
        if (uid != wsSnap.GetValue<string>("ownerId")) return Results.StatusCode(403);

        var catRef = wsRef.Collection("categories").Document(body.CategoryId);

        // Trả trạng thái xóa mềm về lại 0 để hiển thị lên bảng tin Android
        await catRef.UpdateAsync("isDeleted", 0);

        return Results.Ok(new { message = "Khôi phục danh mục thành công!" });
    }
    catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
});

// ======================================================================
// CHẠY SERVER - Lệnh này PHẢI nằm ngay trước các Model
// ======================================================================
app.Run("http://0.0.0.0:5283");

// ======================================================================
// TẤT CẢ CÁC MODEL (RECORDS DTO) PHẢI NẰM DƯỚI NÀY
// ======================================================================

// --- NHÓM MODEL CỦA SOCIAL POST & AI SCAN ---
public record ScanRequest(string OcrText);
public record FeedRequest(List<string> FriendIds, long LastTimestamp, int Limit);
public record CreatePostRequest(string Content, string? ImageUrl, string? Type, object? MilestoneData);
public record LikeActionRequest(string PostId);
public record AddCommentRequest(string PostId, string Content);
public record DeletePostRequest(string PostId);
public record BatchProfileRequest(List<string> UserIds);
public record DeleteCommentRequest(string PostId, string CommentId);
public record EditPostRequest(string PostId, string NewContent, string? NewImageUrl);
public record EditCommentRequest(string PostId, string CommentId, string NewContent);

// --- NHÓM MODEL CỦA QUỸ NHÓM (WORKSPACE) VÀ GIAO DỊCH ---
public record TransactionRequest(string? Id, long Amount, int CategoryId, string? Note, long Timestamp, string? PaymentMethod, int Type, string? WorkspaceId, string? FirestoreCategoryId);
public record WorkspaceCreateRequest(string? Name, string? Type, string? IconName);
public record WorkspaceLeaveRequest(string? WorkspaceId, string? NewOwnerId);
public record WorkspaceKickRequest(string? WorkspaceId, string? TargetUid);
public record WorkspaceTransferRequest(string? WorkspaceId, string? NewOwnerId);
public record TransactionDeleteRequest(string? WorkspaceId, string? TransactionId);
public record CategoryDeleteRequest(string? WorkspaceId, string? CategoryId);
public record MessageRecallRequest(string? WorkspaceId, string? MessageId);
public record EditCategoryRequest(string WorkspaceId, string CategoryId, string Name, string IconName, string ColorCode, int Type);
public record RestoreCategoryRequest(string WorkspaceId, string CategoryId);

// --- NHÓM MODEL CỦA BẠN BÈ VÀ LỜI MỜI ---
public record FriendActionRequest(string? TargetUid);
public record WorkspaceInviteSendRequest(string? WorkspaceId, string? WorkspaceName, List<string>? TargetUids);
public record WorkspaceInviteHandleRequest(string? WorkspaceId, string? InvitationId);

// --- NHÓM MODEL CỦA CHAT TRỰC TIẾP ---
public record DirectFriendMessageRequest(string? ReceiverId, string? Text);

public record DirectConversationSummary(
    string FriendUid,
    string FriendEmail,
    string FriendDisplayName,
    string FriendAvatarUrl,
    string LatestMessageText,
    long LatestMessageTimestamp,
    int UnreadCount
);