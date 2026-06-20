using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class ChatEndpoints
{
    public static void MapChatEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/friend/messages");

        // Lấy danh sách bạn bè có thể nhắn tin
        group.MapGet("/chats", async (HttpRequest request, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;

                var friendIds = (await db.Collection("users").Document(uid).Collection("friends").GetSnapshotAsync())
                    .Documents
                    .Select(doc => doc.Id)
                    .ToList();

                if (friendIds.Count == 0)
                    return Results.Ok(Array.Empty<object>());

                var users = new List<object>();
                foreach (var friendId in friendIds)
                {
                    var friendSnap = await db.Collection("users").Document(friendId).GetSnapshotAsync();
                    if (!friendSnap.Exists)
                        continue;
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
                return Results.Problem(title: "Failed to load chat list", detail: ex.Message, statusCode: 500);
            }
        });

        // Lấy danh sách các cuộc trò chuyện gần đây (kèm tin nhắn mới nhất và số lượng chưa đọc)
        group.MapGet("/conversations", async (HttpRequest request, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;

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
                    if (latestSnapshot.Count == 0)
                        continue;

                    var friendSnap = await db.Collection("users").Document(friendId).GetSnapshotAsync();
                    if (!friendSnap.Exists)
                        continue;

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
                return Results.Problem(title: "Failed to load conversation list", detail: ex.Message, statusCode: 500);
            }
        });

        // Tải chi tiết tin nhắn trực tiếp với một người bạn
        group.MapGet("/{friendUid}", async (HttpRequest request, string friendUid, FirestoreDb db) =>
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
                    return Results.BadRequest(new { message = "Invalid recipient" });

                var senderFriendSnap = await db.Collection("users").Document(uid).Collection("friends").Document(friendUid).GetSnapshotAsync();
                var receiverFriendSnap = await db.Collection("users").Document(friendUid).Collection("friends").Document(uid).GetSnapshotAsync();
                if (!senderFriendSnap.Exists || !receiverFriendSnap.Exists)
                {
                    Console.Error.WriteLine($"Load direct friend messages rejected: users are not mutual friends ({uid}, {friendUid})");
                    return Results.Json(new { message = "Only friends can view messages" }, statusCode: 403);
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
                return Results.Problem(title: "Failed to load messages", detail: ex.Message, statusCode: 500);
            }
        });

        // Gửi tin nhắn trực tiếp cho bạn bè (có kèm bắn thông báo in-app)
        group.MapPost("/send", async (HttpRequest request, DirectFriendMessageRequest body, FirestoreDb db) =>
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
                    return Results.BadRequest(new { message = "Invalid recipient" });
                if (string.IsNullOrWhiteSpace(body.Text) && string.IsNullOrWhiteSpace(body.ImageUrl))
                    return Results.BadRequest(new { message = "Message cannot be empty" });

                var senderRef = db.Collection("users").Document(uid);
                var receiverRef = db.Collection("users").Document(body.ReceiverId);
                var senderSnap = await senderRef.GetSnapshotAsync();
                var receiverSnap = await receiverRef.GetSnapshotAsync();

                if (!senderSnap.Exists || !receiverSnap.Exists)
                    return Results.NotFound(new { message = "User not found" });

                var senderFriendSnap = await senderRef.Collection("friends").Document(body.ReceiverId).GetSnapshotAsync();
                var receiverFriendSnap = await receiverRef.Collection("friends").Document(uid).GetSnapshotAsync();
                if (!senderFriendSnap.Exists || !receiverFriendSnap.Exists)
                {
                    Console.Error.WriteLine($"Send direct friend message rejected: users are not mutual friends ({uid}, {body.ReceiverId})");
                    return Results.Json(new { message = "Only friends can message each other" }, statusCode: 403);
                }

                var orderedIds = new[] { uid, body.ReceiverId }.OrderBy(id => id, StringComparer.Ordinal).ToArray();
                var chatId = $"{orderedIds[0]}_{orderedIds[1]}";
                var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                var senderName = senderSnap.ContainsField("displayName") ? senderSnap.GetValue<string>("displayName") : "User";
                var senderAvatar = senderSnap.ContainsField("avatarUrl") ? senderSnap.GetValue<string>("avatarUrl") : "";

                // Mở Batch để ghi tin nhắn và thông báo cùng lúc
                var batch = db.StartBatch();

                // Ghi tin nhắn vào phòng chat
                var msgRef = db.Collection("direct_chats").Document(chatId).Collection("messages").Document();
                var messageData = new Dictionary<string, object>
                {
                    { "senderId", uid },
                    { "receiverId", body.ReceiverId },
                    { "senderName", senderName ?? "User" },
                    { "senderAvatar", senderAvatar ?? "" },
                    { "text", body.Text?.Trim() ?? "" },
                    { "timestamp", timestamp },
                    { "isRead", false },
                    { "imageUrl", body.ImageUrl ?? "" },
                    { "isRecalled", false }
                };
                batch.Set(msgRef, messageData);

                // Ghi In-app Notification cho người nhận
                var notifRef = receiverRef.Collection("notifications").Document();
                string notifMessage = string.IsNullOrWhiteSpace(body.Text) ? "Sent an image" : body.Text.Trim();

                batch.Set(notifRef, new
                {
                    type = "FRIEND_CHAT",
                    title = senderName ?? "Friend",
                    message = notifMessage,
                    timestamp = timestamp,
                    isRead = false,
                    referenceId = uid
                });

                await batch.CommitAsync();

                return Results.Ok(new { message = "Message sent successfully", chatId });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Direct friend message failed: {ex}");
                return Results.Problem(
                    title: "Failed to send message",
                    detail: ex.Message,
                    statusCode: 500);
            }
        });

        // Thu hồi tin nhắn trực tiếp
        group.MapPatch("/{friendUid}/{messageId}/recall", async (HttpRequest request, string friendUid, string messageId, FirestoreDb db) => {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                {
                    Console.Error.WriteLine("Recall direct message rejected: missing or invalid bearer token");
                    return Results.Unauthorized();
                }

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;

                if (string.IsNullOrWhiteSpace(friendUid) || string.IsNullOrWhiteSpace(messageId))
                    return Results.BadRequest(new { message = "Invalid recall information" });

                // Lấy chatId chuẩn theo logic ghép ID
                var orderedIds = new[] { uid, friendUid }.OrderBy(id => id, StringComparer.Ordinal).ToArray();
                var chatId = $"{orderedIds[0]}_{orderedIds[1]}";

                // Trỏ tới Document tin nhắn cần thu hồi
                var messageRef = db.Collection("direct_chats")
                                   .Document(chatId)
                                   .Collection("messages")
                                   .Document(messageId);

                var messageSnap = await messageRef.GetSnapshotAsync();

                if (!messageSnap.Exists)
                    return Results.NotFound(new { message = "Message to recall not found" });

                // Kiểm tra quyền (Chỉ người gửi mới được thu hồi tin nhắn của chính mình)
                var senderId = messageSnap.ContainsField("senderId") ? messageSnap.GetValue<string>("senderId") : "";
                if (senderId != uid)
                    return Results.Json(new { message = "You do not have permission to recall this message" }, statusCode: 403);

                var isRecalled = messageSnap.ContainsField("isRecalled") && messageSnap.GetValue<bool>("isRecalled");
                if (isRecalled)
                    return Results.BadRequest(new { message = "This message was already recalled" });

                // Xóa nội dung và bật cờ
                var updates = new Dictionary<string, object>
                {
                    { "text", "" },
                    { "imageUrl", "" },
                    { "isRecalled", true }
                };

                await messageRef.UpdateAsync(updates);

                return Results.Ok(new { message = "Message recalled successfully", messageId });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Recall message failed: {ex}");
                return Results.Problem(
                    title: "Failed to recall message",
                    detail: ex.Message,
                    statusCode: 500);
            }
        });
    }
}