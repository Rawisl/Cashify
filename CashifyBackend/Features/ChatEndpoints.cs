using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class ChatEndpoints
{
    public static void MapChatEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/friend/messages");

        //DANH SÁCH BẠN BÈ CÓ THỂ NHẮN TIN
        group.MapGet("/chats", async (HttpRequest request, FirestoreDb db) => {
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
                return Results.Problem(title: "Tải danh sách trò chuyện thất bại", detail: ex.Message, statusCode: 500);
            }
        });

        //DANH SÁCH CUỘC TRÒ CHUYỆN GẦN ĐÂY
        group.MapGet("/conversations", async (HttpRequest request, FirestoreDb db) => {
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
                return Results.Problem(title: "Tải danh sách trò chuyện thất bại", detail: ex.Message, statusCode: 500);
            }
        });

        //TẢI TIN NHẮN TRỰC TIẾP VỚI MỘT NGƯỜI BẠN
        group.MapGet("/{friendUid}", async (HttpRequest request, string friendUid, FirestoreDb db) => {
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

        //GỬI TIN NHẮN TRỰC TIẾP CHO BẠN BÈ
        group.MapPost("/send", async (HttpRequest request, DirectFriendMessageRequest body, FirestoreDb db) => {
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
    }
}