using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class WorkspaceEndpoints
{
    public static void MapWorkspaceEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/workspace");

        //TẠO QUỸ CHUNG(KÈM DATA MẪU VÀ LOG)
        group.MapPost("/create", async (HttpRequest request, WorkspaceCreateRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.Name))
                    return Results.BadRequest("Tên quỹ không được để trống");

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

        //RỜI QUỸ / XÓA QUỸ(KHÔNG KÈM NHƯỢNG QUYỀN)
        group.MapPost("/leave", async (HttpRequest request, WorkspaceLeaveRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId))
                    return Results.BadRequest("Thiếu WorkspaceId");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

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
                        foreach (var doc in subDocs.Documents)
                            batch.Delete(doc.Reference);
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

        //NHƯỢNG QUYỀN TRƯỞNG NHÓM
        group.MapPost("/transfer-owner", async (HttpRequest request, WorkspaceTransferRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.NewOwnerId))
                    return Results.BadRequest("Thiếu Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (uid != ownerId)
                    return Results.StatusCode(403); // Chỉ Owner cũ mới được gọi API này
                if (!members.Contains(body.NewOwnerId))
                    return Results.BadRequest(new { error = "Người nhận không có trong Quỹ" });

                var batch = db.StartBatch();
                batch.Update(wsRef, "ownerId", body.NewOwnerId);

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new { actionType = "TRANSFER_OWNER", message = "đã nhường quyền trưởng nhóm", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Nhượng quyền thành công" });
            }
            catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
        });

        //TẠO/SỬA GIAO DỊCH QUỸ CHUNG
        group.MapPost("/transaction/add", async (HttpRequest request, TransactionRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;
                string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "A member";

                if (body.Amount <= 0)
                    return Results.BadRequest(new { error = "invalid budget" });
                if (string.IsNullOrEmpty(body.WorkspaceId) || body.WorkspaceId == "PERSONAL")
                    return Results.BadRequest(new { error = "This API is for Group fund" });


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Not found group" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

                var transId = !string.IsNullOrEmpty(body.Id) ? body.Id : Guid.NewGuid().ToString();
                var transRef = wsRef.Collection("transactions").Document(transId);

                if (!string.IsNullOrEmpty(body.Id))
                {
                    var existingTransSnap = await transRef.GetSnapshotAsync();
                    if (existingTransSnap.Exists)
                    {
                        var creatorId = existingTransSnap.GetValue<string>("userId");
                        if (uid != creatorId && uid != ownerId)
                            return Results.StatusCode(403);
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

        //XÓA GIAO DỊCH QUỸ CHUNG (KÈM PHÂN QUYỀN)
        group.MapPost("/transaction/delete", async (HttpRequest request, TransactionDeleteRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;
                string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "A member";

                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.TransactionId))
                    return Results.BadRequest("Data lacking");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Not found group" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

                var transRef = wsRef.Collection("transactions").Document(body.TransactionId);
                var transSnap = await transRef.GetSnapshotAsync();
                if (!transSnap.Exists)
                    return Results.NotFound(new { error = "Transaction deleted before" });

                var creatorId = transSnap.GetValue<string>("userId");
                if (uid != creatorId && uid != ownerId)
                    return Results.StatusCode(403);

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

        //GỬI LỜI MỜI VÀO QUỸ (WORKSPACE INVITATIONS)
        group.MapPost("/invite/send", async (HttpRequest request, WorkspaceInviteSendRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) || body.TargetUids == null || body.TargetUids.Count == 0)
                    return Results.BadRequest("Data lacking");


                var batch = db.StartBatch();
                var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Not found group" });

                var ownerId = wsSnap.GetValue<string>("ownerId");
                if (uid != ownerId)
                    return Results.StatusCode(403);

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

        //ĐỒNG Ý LỜI MỜI VÀO QUỸ
        group.MapPost("/invite/accept", async (HttpRequest request, WorkspaceInviteHandleRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.InvitationId))
                    return Results.BadRequest("Data lacking");



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

        //TỪ CHỐI LỜI MỜI VÀO QUỸ
        group.MapPost("/invite/decline", async (HttpRequest request, WorkspaceInviteHandleRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                if (string.IsNullOrEmpty(body.InvitationId))
                    return Results.BadRequest("Data lacking");



                // Chỉ việc xé giấy mời vứt đi
                var inviteRef = db.Collection("users").Document(uid).Collection("workspace_invitations").Document(body.InvitationId);
                await inviteRef.DeleteAsync();

                return Results.Ok(new { message = "Rejected the invitation." });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        //ĐUỔI THÀNH VIÊN (KICK MEMBER)
        group.MapPost("/member/kick", async (HttpRequest request, WorkspaceKickRequest body, FirestoreDb db) =>
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


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

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

        //XÓA MỀM DANH MỤC CHO OWNER
        group.MapPost("/category/delete", async (HttpRequest request, CategoryDeleteRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.CategoryId))
                    return Results.BadRequest("Thiếu Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Không tìm thấy quỹ");

                // CHỈ TRƯỞNG NHÓM MỚI ĐƯỢC XÓA DANH MỤC
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

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

        //CHỈNH SỬA DANH MỤC QUỸ
        group.MapPost("/category/edit", async (HttpRequest request, EditCategoryRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.CategoryId) || string.IsNullOrEmpty(body.Name))
                    return Results.BadRequest("Thiếu thông tin chỉnh sửa");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Không tìm thấy quỹ");

                // Bảo mật: Chỉ Trưởng nhóm (Owner) mới được quyền chỉnh sửa cấu trúc danh mục nhóm
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

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

        //KHÔI PHỤC DANH MỤC BỊ ẨN QUỸ
        group.MapPost("/category/restore", async (HttpRequest request, RestoreCategoryRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.CategoryId))
                    return Results.BadRequest("Thiếu Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Không tìm thấy quỹ");
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

                var catRef = wsRef.Collection("categories").Document(body.CategoryId);

                // Trả trạng thái xóa mềm về lại 0 để hiển thị lên bảng tin Android
                await catRef.UpdateAsync("isDeleted", 0);

                return Results.Ok(new { message = "Khôi phục danh mục thành công!" });
            }
            catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
        });

        //GỬI TIN NHẮN VÀO QUỸ CHUNG (KÈM BẮN THÔNG BÁO)
        group.MapPost("/message/send", async (HttpRequest request, WorkspaceMessageSendRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.WorkspaceId) ||
    (string.IsNullOrWhiteSpace(body.Text) && string.IsNullOrWhiteSpace(body.ImageUrl)))
                    return Results.BadRequest(new { error = "Data lacking" });

                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Không tìm thấy Quỹ" });

                var members = wsSnap.GetValue<List<string>>("members");
                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403); // Phải là thành viên mới được chat

                var workspaceName = wsSnap.ContainsField("name") ? wsSnap.GetValue<string>("name") : "Group";

                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                string senderName = userSnap.Exists && userSnap.ContainsField("displayName") ? userSnap.GetValue<string>("displayName") : "User";
                string senderAvatar = userSnap.Exists && userSnap.ContainsField("avatarUrl") ? userSnap.GetValue<string>("avatarUrl") : "";

                var batch = db.StartBatch();
                var msgRef = wsRef.Collection("messages").Document();
                var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

                // Lưu tin nhắn
                var messageData = new Dictionary<string, object>
                {
                    { "senderId", uid },
                    { "senderName", senderName },
                    { "senderAvatar", senderAvatar },
                    { "text", body.Text?.Trim() ?? "" },
                    { "timestamp", timestamp },
                    { "imageUrl", body.ImageUrl ?? "" },
                    { "isRecalled", false }
                };
                batch.Set(msgRef, messageData);

                // Rải thông báo cho các thành viên khác
                foreach (var memberId in members)
                {
                    if (memberId != uid)
                    {
                        var notifRef = db.Collection("users").Document(memberId).Collection("notifications").Document();
                        batch.Set(notifRef, new
                        {
                            type = "WORKSPACE_CHAT",
                            title = workspaceName,
                            message = $"{senderName}: {body.Text.Trim()}",
                            timestamp = timestamp,
                            isRead = false,
                            referenceId = body.WorkspaceId
                        });
                    }
                }

                await batch.CommitAsync();
                return Results.Ok(new { message = "Gửi tin nhắn thành công", messageId = msgRef.Id });
            }
            catch (Exception ex) { return Results.Problem($"Lỗi hệ thống: {ex.Message}"); }
        });

        //THU HỒI TIN NHẮN QUỸ (ĐÃ CÓ TRONG MẪU CỦA ÔNG, TUI CHUẨN HÓA LẠI CHÚT)
        group.MapPost("/message/recall", async (HttpRequest request, MessageRecallRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;

                if (string.IsNullOrWhiteSpace(body.WorkspaceId) || string.IsNullOrWhiteSpace(body.MessageId))
                    return Results.BadRequest(new { message = "Thông tin không hợp lệ" });

                // 1. Trỏ thẳng vào document tin nhắn nằm trong sub-collection của Workspace
                var messageRef = db.Collection("workspaces")
                                   .Document(body.WorkspaceId)
                                   .Collection("messages")
                                   .Document(body.MessageId);

                var messageSnap = await messageRef.GetSnapshotAsync();
                if (!messageSnap.Exists)
                    return Results.NotFound(new { message = "Không tìm thấy tin nhắn" });

                // 2. Kiểm tra bảo mật (Quyền sở hữu)
                var senderId = messageSnap.ContainsField("senderId") ? messageSnap.GetValue<string>("senderId") : "";
                var workspaceSnap = await db.Collection("workspaces").Document(body.WorkspaceId).GetSnapshotAsync();
                var ownerId = workspaceSnap.ContainsField("ownerId") ? workspaceSnap.GetValue<string>("ownerId") : "";

                // Cho phép người gửi HOẶC chủ nhóm (Owner) thực hiện thu hồi
                if (senderId != uid && ownerId != uid)
                    return Results.Json(new { message = "Bạn không có quyền thu hồi tin nhắn này" }, statusCode: 403);

                // 3. Tiến hành dọn rác DB: Ghi đè chuỗi rỗng và lật cờ True
                var updates = new Dictionary<string, object>
        {
            { "text", "" },
            { "imageUrl", "" },
            { "isRecalled", true }
        };

                await messageRef.UpdateAsync(updates);
                return Results.Ok(new { message = "Thu hồi tin nhắn nhóm thành công" });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Recall workspace message failed: {ex}");
                return Results.Problem(title: "Lỗi thu hồi tin nhắn", detail: ex.Message, statusCode: 500);
            }
        });
    }
}