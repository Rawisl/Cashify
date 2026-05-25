using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class SocialEndpoints
{
    public static void MapSocialEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1");

        //load các bài đăng
        group.MapPost("/post/feed", async (HttpRequest request, FeedRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid; // LẤY UID ĐỂ CHECK LIKE

                Query query = db.Collection("posts");

                if (body.FriendIds != null && body.FriendIds.Count > 0)
                {
                    var limitedFriends = body.FriendIds.Take(30).ToList();
                    query = query.WhereIn("userId", limitedFriends);
                }

                query = query.OrderByDescending("timestamp");
                if (body.LastTimestamp > 0)
                    query = query.StartAfter(body.LastTimestamp);
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
        group.MapGet("/post/{postId}/comments", async (HttpRequest request, string postId, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var commentsRef = db.Collection("posts").Document(postId).Collection("comments");

                // Load comment xếp theo thời gian cũ nhất lên trước (như Facebook)
                var snapshot = await commentsRef.OrderBy("timestamp").GetSnapshotAsync();

                var comments = snapshot.Documents.Select(doc => doc.ToDictionary()).ToList();
                return Results.Ok(comments);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        //load bài viết tường nhà của riêng 1 user (chỉ load bài của riêng nó)
        group.MapGet("/post/wall/{targetUid}", async (HttpRequest request, FirestoreDb db, string targetUid, int limit = 10, long lastTimestamp = 0) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                // Lấy UID của người ĐANG XEM để check xem nó đã like bài trên tường này chưa
                var token = authHeader.Substring("Bearer ".Length);
                var currentViewerUid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                Query query = db.Collection("posts").WhereEqualTo("userId", targetUid).OrderByDescending("timestamp");

                if (lastTimestamp > 0)
                    query = query.StartAfter(lastTimestamp);
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
        group.MapPost("/post/create", async (HttpRequest request, CreatePostRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;

                if (string.IsNullOrEmpty(body.Content) && string.IsNullOrEmpty(body.ImageUrl))
                    return Results.BadRequest("Nội dung không được để trống");

                // Lấy tên tác giả từ Firestore
                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                var authorName = userSnap.Exists && userSnap.ContainsField("displayName")
                    ? userSnap.GetValue<string>("displayName")
                    : "Người dùng Cashify";

                var authorAvatarUrl = userSnap.Exists && userSnap.ContainsField("avatarUrl")
                    ? userSnap.GetValue<string>("avatarUrl") : "";

                var postId = Guid.NewGuid().ToString();
                var postRef = db.Collection("posts").Document(postId);

                var postData = new Dictionary<string, object>
        {
            { "postId", postId },
            { "userId", uid },
            { "authorName", authorName },
            { "authorAvatarUrl", authorAvatarUrl },
            { "type", body.Type ?? "USER_POST" },
            { "content", body.Content ?? "" },
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
        group.MapPost("/post/like", async (HttpRequest request, LikeActionRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(body.PostId);
                var likeRef = postRef.Collection("likes").Document(uid);

                // Dùng Transaction để đảm bảo tính toán số lượng Tim chuẩn xác 100%
                await db.RunTransactionAsync(async transaction =>
                {
                    DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
                    if (!postSnap.Exists)
                        throw new Exception("Bài viết không tồn tại");

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
        group.MapPost("/post/edit", async (HttpRequest request, EditPostRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(body.PostId);
                var postSnap = await postRef.GetSnapshotAsync();

                if (!postSnap.Exists)
                    return Results.NotFound("Bài viết không tồn tại");
                if (postSnap.GetValue<string>("userId") != uid)
                    return Results.StatusCode(403); // Cấm sửa trộm

                var updates = new Dictionary<string, object>
        {
            { "content", body.NewContent },
            { "isEdited", true }
        };
                // Nếu có update ảnh mới thì lưu, không thì giữ nguyên ảnh cũ
                if (body.NewImageUrl != null)
                    updates["imageUrl"] = body.NewImageUrl;

                await postRef.UpdateAsync(updates);
                return Results.Ok(new { message = "Đã sửa bài viết" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        //xóa bài viết (Dọn sạch cả Sub-collection để chống rác Data)
        group.MapPost("/post/delete", async (HttpRequest request, DeletePostRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(body.PostId);
                var postSnap = await postRef.GetSnapshotAsync();

                if (!postSnap.Exists)
                    return Results.NotFound("Bài viết không tồn tại");
                if (postSnap.GetValue<string>("userId") != uid)
                    return Results.StatusCode(403); // Cấm xóa trộm

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

        //add bình luận
        group.MapPost("/comment/add", async (HttpRequest request, AddCommentRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(body.PostId);
                var commentId = Guid.NewGuid().ToString();
                var commentRef = postRef.Collection("comments").Document(commentId);

                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                var authorName = userSnap.Exists && userSnap.ContainsField("displayName")
                    ? userSnap.GetValue<string>("displayName") : "Người dùng Cashify";
                var authorAvatarUrl = userSnap.Exists && userSnap.ContainsField("avatarUrl")
                    ? userSnap.GetValue<string>("avatarUrl") : "";

                await db.RunTransactionAsync(async transaction =>
                {
                    DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
                    if (!postSnap.Exists)
                        throw new Exception("Bài viết không tồn tại");

                    var commentData = new Dictionary<string, object>
            {
                { "commentId", commentId },
                { "userId", uid },
                { "authorName", authorName },
                { "authorAvatarUrl", authorAvatarUrl },
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
        group.MapPost("/comment/edit", async (HttpRequest request, EditCommentRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var commentRef = db.Collection("posts").Document(body.PostId).Collection("comments").Document(body.CommentId);
                var commentSnap = await commentRef.GetSnapshotAsync();

                if (!commentSnap.Exists)
                    return Results.NotFound("Bình luận không tồn tại");
                if (commentSnap.GetValue<string>("userId") != uid)
                    return Results.StatusCode(403);

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
        group.MapPost("/comment/delete", async (HttpRequest request, DeleteCommentRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(body.PostId);
                var commentRef = postRef.Collection("comments").Document(body.CommentId);

                await db.RunTransactionAsync(async transaction =>
                {
                    var postSnap = await transaction.GetSnapshotAsync(postRef);
                    var commentSnap = await transaction.GetSnapshotAsync(commentRef);

                    if (!postSnap.Exists || !commentSnap.Exists)
                        throw new Exception("Không tìm thấy dữ liệu");

                    var postOwnerId = postSnap.GetValue<string>("userId");
                    var commentOwnerId = commentSnap.GetValue<string>("userId");

                    // Chỉ chủ comment hoặc chủ bài viết mới có quyền xóa
                    if (uid != commentOwnerId && uid != postOwnerId)
                        throw new Exception("Không có quyền xóa");

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
        group.MapPost("/user/batch-profiles", async (HttpRequest request, BatchProfileRequest body, FirestoreDb db) =>
        {
            try
            {
                if (body.UserIds == null || body.UserIds.Count == 0)
                    return Results.Ok(new Dictionary<string, object>());

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

        // GỬI LỜI MỜI KẾT BẠN
        group.MapPost("/friend/request", async (HttpRequest request, FriendActionRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var decodedToken = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);
                var uid = decodedToken.Uid;
                string senderName = decodedToken.Claims.TryGetValue("name", out var nameObj) ? nameObj.ToString()! : "Unknown user";

                if (string.IsNullOrEmpty(body.TargetUid) || uid == body.TargetUid)
                    return Results.BadRequest("UID invalid");

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

        //ĐỒNG Ý KẾT BẠN
        group.MapPost("/friend/accept", async (HttpRequest request, FriendActionRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                if (string.IsNullOrEmpty(body.TargetUid))
                    return Results.BadRequest("UID không hợp lệ");

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

        //TỪ CHỐI / HỦY LỜI MỜI / HỦY KẾT BẠN (GỘP CHUNG 3 IN 1)
        group.MapPost("/friend/remove", async (HttpRequest request, FriendActionRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                if (string.IsNullOrEmpty(body.TargetUid))
                    return Results.BadRequest("UID invalid");

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

        //GỢI Ý KẾT BẠN CHỈ GỒM NGƯỜI CHƯA CÓ QUAN HỆ
        group.MapGet("/friend/suggestions", async (HttpRequest request, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
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

        // LẤY CHI TIẾT 1 BÀI VIẾT
        group.MapGet("/post/{postId}", async (HttpRequest request, string postId, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                var postRef = db.Collection("posts").Document(postId);
                var postSnap = await postRef.GetSnapshotAsync();

                if (!postSnap.Exists)
                    return Results.NotFound(new { message = "Bài viết không tồn tại" });

                var postDict = postSnap.ToDictionary();

                // Check xem người đang xem đã like chưa
                var likeSnap = await postRef.Collection("likes").Document(uid).GetSnapshotAsync();
                postDict["isLiked"] = likeSnap.Exists;

                // Đảm bảo có shareCount (field cũ có thể chưa có)
                if (!postDict.ContainsKey("shareCount"))
                    postDict["shareCount"] = 0L;

                return Results.Ok(postDict);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });
    }
}