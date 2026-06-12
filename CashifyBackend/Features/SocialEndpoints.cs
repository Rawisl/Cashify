using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class SocialEndpoints
{
    public static void MapSocialEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1");

        // ---------------------------------------------------------
        // 1. TẢI FEED BẢNG TIN
        // ---------------------------------------------------------
        group.MapPost("/post/feed", async (HttpRequest request, FeedRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                // 1. Kéo Profile User xem có phải Admin không
                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = userSnap.ContainsField("role") && userSnap.GetValue<string>("role") == "ADMIN";

                Query query = db.Collection("posts");

                // NẾU KHÔNG PHẢI ADMIN THÌ MỚI BỊ LỌC THEO BẠN BÈ
                if (!isAdmin && body.FriendIds != null && body.FriendIds.Count > 0)
                {
                    var limitedFriends = body.FriendIds.Take(30).ToList();
                    query = query.WhereIn("userId", limitedFriends);
                }

                query = query.OrderByDescending("timestamp");
                if (body.LastTimestamp > 0)
                    query = query.StartAfter(body.LastTimestamp);
                query = query.Limit(body.Limit > 0 ? body.Limit : 10);

                var snapshot = await query.GetSnapshotAsync();

                var likeCheckTasks = snapshot.Documents.Select(async doc =>
                {
                    var postDict = doc.ToDictionary();
                    var likeSnap = await db.Collection("posts").Document(doc.Id).Collection("likes").Document(uid).GetSnapshotAsync();
                    postDict["isLiked"] = likeSnap.Exists;
                    return postDict;
                });

                var posts = (await Task.WhenAll(likeCheckTasks)).ToList();
                return Results.Ok(posts);
            }
            catch (Exception ex) { return Results.Problem($"Lỗi lấy feed: {ex.Message}"); }
        });

        // ---------------------------------------------------------
        // 2. TẢI BÌNH LUẬN
        // ---------------------------------------------------------
        group.MapGet("/post/{postId}/comments", async (HttpRequest request, string postId, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var commentsRef = db.Collection("posts").Document(postId).Collection("comments");
                var snapshot = await commentsRef.OrderBy("timestamp").GetSnapshotAsync();

                var comments = snapshot.Documents.Select(doc => doc.ToDictionary()).ToList();
                return Results.Ok(comments);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 3. TẢI BÀI TƯỜNG NHÀ 
        // ---------------------------------------------------------
        group.MapGet("/post/wall/{targetUid}", async (HttpRequest request, FirestoreDb db, string targetUid, int limit = 10, long lastTimestamp = 0) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var currentViewerUid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                Query query = db.Collection("posts").WhereEqualTo("userId", targetUid).OrderByDescending("timestamp");

                if (lastTimestamp > 0)
                    query = query.StartAfter(lastTimestamp);
                query = query.Limit(limit);

                var snapshot = await query.GetSnapshotAsync();

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

        // ---------------------------------------------------------
        // 4. TẠO BÀI ĐĂNG
        // ---------------------------------------------------------
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

                if (string.IsNullOrEmpty(body.Content) && string.IsNullOrEmpty(body.ImageUrl) && string.IsNullOrEmpty(body.MilestoneData))
                    return Results.BadRequest("Nội dung không được để trống");

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
                    { "audience", body.Audience ?? "FRIENDS" }, // HỨNG Audience TỪ ANDROID
                    { "content", body.Content ?? "" },
                    { "imageUrl", body.ImageUrl ?? "" },
                    { "milestoneData", body.MilestoneData },
                    { "likeCount", 0 },
                    { "commentCount", 0 },
                    { "timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() },
                    { "isEdited", false }
                };
                // Bắt sự kiện: Nếu là bài đăng MILESTONE, lưu cờ vào shared_achievements
                if (body.Type == "MILESTONE_POST" && !string.IsNullOrEmpty(body.MilestoneData))
                {
                    try
                    {
                        var milestoneObj = System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, System.Text.Json.JsonElement>>(body.MilestoneData);
                        if (milestoneObj != null && milestoneObj.ContainsKey("achievementId"))
                        {
                            string achId = milestoneObj["achievementId"].GetString();
                            var achRef = db.Collection("users").Document(uid).Collection("shared_achievements").Document(achId);
                            await achRef.SetAsync(new { sharedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
                        }
                    }
                    catch (Exception) { /* Bỏ qua nếu parse JSON lỗi */ }
                }
                await postRef.SetAsync(postData);
                return Results.Ok(new { message = "Đăng bài thành công", postId });
            }
            catch (Exception ex) { return Results.Problem($"Lỗi tạo bài: {ex.Message}"); }
        });

        // quét transactions và check shared_achievement cho post
        group.MapGet("/achievements/available", async (HttpRequest request, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                string? currentWorkspaceId = request.Query["workspaceId"]; // Nhận ID quỹ nếu đang đứng ở Quỹ
                bool isWorkspace = !string.IsNullOrEmpty(currentWorkspaceId) && currentWorkspaceId != "PERSONAL";

                var availableList = new List<AchievementSuggestion>();

                // 1. Lấy danh sách achievement đã chia sẻ để không báo lại
                var sharedSnap = await db.Collection("users").Document(uid).Collection("shared_achievements").GetSnapshotAsync();
                var sharedIds = new HashSet<string>(sharedSnap.Documents.Select(d => d.Id));

                // 2. Kéo Profile User (để lấy Chuỗi Streak)
                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                int streakDays = userSnap.ContainsField("streakDays") ? Convert.ToInt32(userSnap.GetValue<object>("streakDays")) : 0;

                // 3. Kéo Thống kê (Rẽ nhánh Cá nhân hoặc Quỹ nhóm)
                DocumentSnapshot statsSnap;
                if (isWorkspace)
                {
                    statsSnap = await db.Collection("workspaces").Document(currentWorkspaceId).Collection("workspace_stats").Document("summary").GetSnapshotAsync();
                }
                else
                {
                    statsSnap = await db.Collection("users").Document(uid).Collection("user_stats").Document("summary").GetSnapshotAsync();
                }

                int totalTrans = 0;
                long totalIncomeThisMonth = 0;
                long totalSpendThisMonth = 0;
                var now = DateTimeOffset.UtcNow;
                string monthKey = $"{now.Year}_{now.Month:D2}";

                if (statsSnap.Exists)
                {
                    try
                    {
                        totalTrans = statsSnap.ContainsField("totalTransactions") ? Convert.ToInt32(statsSnap.GetValue<object>("totalTransactions")) : 0;
                        totalIncomeThisMonth = statsSnap.ContainsField($"income_{monthKey}") ? Convert.ToInt64(statsSnap.GetValue<object>($"income_{monthKey}")) : 0;
                        totalSpendThisMonth = statsSnap.ContainsField($"spend_{monthKey}") ? Convert.ToInt64(statsSnap.GetValue<object>($"spend_{monthKey}")) : 0;
                    }
                    catch { }
                }

                // ==========================================
                // LUẬT CHƠI MỚI (NEW ACHIEVEMENTS)
                // ==========================================

                // 🌟 MỐC 1: SỐ LƯỢNG GIAO DỊCH (Cho cả Cá nhân và Quỹ)
                int[] transMilestones = { 10, 50, 100, 500 };
                foreach (var m in transMilestones)
                {
                    string achId = isWorkspace ? $"ws_ach_trans_{m}_{currentWorkspaceId}" : $"ach_trans_{m}";
                    if (totalTrans >= m && !sharedIds.Contains(achId))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = achId,
                            Title = isWorkspace ? "Tổ Đội Chăm Chỉ" : "Ong Chăm Chỉ",
                            Description = isWorkspace ? $"Quỹ nhóm đã đạt {m} giao dịch. Tinh thần đồng đội tuyệt vời!" : $"Đã ghi chép giao dịch thứ {m} trên Cashify!",
                            IconText = isWorkspace ? "🤝" : "🐝",
                            MonthLabel = "Thành tựu vĩnh viễn",
                            AmountLabel = $"{m} Giao dịch",
                            Progress = 100
                        });
                        break;
                    }
                }

                // 🌟 MỐC 2: CHUỖI NGÀY KỶ LUẬT (STREAK - Chỉ cá nhân)
                if (!isWorkspace)
                {
                    int[] streakMilestones = { 3, 7, 15, 30, 100, 160, 365 };
                    foreach (var s in streakMilestones)
                    {
                        string achId = $"ach_streak_{s}";
                        if (streakDays >= s && !sharedIds.Contains(achId))
                        {
                            availableList.Add(new AchievementSuggestion
                            {
                                Id = achId,
                                Title = "Kỷ Luật Thép",
                                Description = $"Bạn đã ghi chép liên tục {s} ngày không trượt phát nào!",
                                IconText = "🔥",
                                MonthLabel = "Chuỗi ngày",
                                AmountLabel = $"{s} Ngày",
                                Progress = 100
                            });
                            break;
                        }
                    }
                }

                if (statsSnap.Exists && !isWorkspace)
                {
                    // 🌟 MỐC 3: CÚ ĐÊM (NIGHT OWL)
                    bool isNightOwl = statsSnap.ContainsField("nightOwlUnlocked") && Convert.ToBoolean(statsSnap.GetValue<object>("nightOwlUnlocked"));
                    if (isNightOwl && !sharedIds.Contains("ach_night_owl"))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = "ach_night_owl",
                            Title = "Cú Đêm Tài Chính",
                            Description = "Kẻ thức thời là trang tuấn kiệt, ghi chép lúc 2h sáng mới chuẩn.",
                            IconText = "🦉",
                            MonthLabel = "Thành tựu vĩnh viễn",
                            AmountLabel = "Cú Đêm",
                            Progress = 100
                        });
                    }

                    // 🌟 MỐC 4: ĐẠI GIA CHỐT ĐƠN (BIG SPENDER)
                    bool isBigSpender = statsSnap.ContainsField("bigSpenderUnlocked") && Convert.ToBoolean(statsSnap.GetValue<object>("bigSpenderUnlocked"));
                    if (isBigSpender && !sharedIds.Contains("ach_big_spender"))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = "ach_big_spender",
                            Title = "Đại Gia Chốt Đơn",
                            Description = "Một lần tiêu bằng người ta làm cả tháng. Khét đấy!",
                            IconText = "🐋",
                            MonthLabel = "Thành tựu vĩnh viễn",
                            AmountLabel = "> 10 Triệu",
                            Progress = 100
                        });
                    }
                }

                // 🌟 MỐC 5: DƯƠNG THÁI QUÁ (DƯ DẢ - Cho cả Cá nhân và Quỹ)
                long surplus = totalIncomeThisMonth - totalSpendThisMonth;
                string surplusAchId = isWorkspace ? $"ws_recap_surplus_{now.Year}_{now.Month}_{currentWorkspaceId}" : $"recap_surplus_{now.Year}_{now.Month}";
                if (surplus > 0 && !sharedIds.Contains(surplusAchId))
                {
                    availableList.Add(new AchievementSuggestion
                    {
                        Id = surplusAchId,
                        Title = isWorkspace ? "Quỹ Nhóm Giàu Có" : "Dương Thái Quá",
                        Description = isWorkspace ? "Tháng này quỹ nhóm thu nhiều hơn chi. Sắp được đi ăn lẩu rồi!" : "Tháng này bạn làm ra nhiều hơn xài đi.",
                        IconText = "💰",
                        MonthLabel = $"Tháng {now.Month}/{now.Year}",
                        AmountLabel = $"Dư: {surplus.ToString("N0")}đ",
                        Progress = 100
                    });
                }

                return Results.Ok(availableList);
            }
            catch (Exception ex) { return Results.Problem($"Lỗi lấy thành tựu: {ex.Message}"); }
        });

        // ---------------------------------------------------------
        // 5. THẢ TIM (LIKE)
        // ---------------------------------------------------------
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

                await db.RunTransactionAsync(async transaction =>
                {
                    DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
                    if (!postSnap.Exists)
                        throw new Exception("Bài viết không tồn tại");

                    DocumentSnapshot likeSnap = await transaction.GetSnapshotAsync(likeRef);
                    long currentLikes = postSnap.GetValue<long>("likeCount");

                    if (likeSnap.Exists)
                    {
                        transaction.Delete(likeRef);
                        transaction.Update(postRef, "likeCount", Math.Max(0, currentLikes - 1));
                    }
                    else
                    {
                        transaction.Set(likeRef, new Dictionary<string, object> { { "likedAt", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() } });
                        transaction.Update(postRef, "likeCount", currentLikes + 1);
                    }
                });

                return Results.Ok(new { message = "Thao tác Like/Unlike thành công" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 6. SỬA BÀI VIẾT (Đã fix Update Audience)
        // ---------------------------------------------------------
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

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                if (postSnap.GetValue<string>("userId") != uid && !isAdmin)
                    return Results.StatusCode(403);

                var updates = new Dictionary<string, object>
                {
                    { "content", body.NewContent },
                    { "isEdited", true }
                };
                if (body.NewImageUrl != null)
                    updates["imageUrl"] = body.NewImageUrl;

                // Nếu Android có truyền Audience lên thì C# mới Update
                if (body.Audience != null)
                    updates["audience"] = body.Audience;

                await postRef.UpdateAsync(updates);
                return Results.Ok(new { message = "Đã sửa bài viết" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 7. XÓA BÀI VIẾT
        // ---------------------------------------------------------
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

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                if (postSnap.GetValue<string>("userId") != uid && !isAdmin)
                    return Results.StatusCode(403);

                var batch = db.StartBatch();

                var likesSnap = await postRef.Collection("likes").GetSnapshotAsync();
                foreach (var doc in likesSnap.Documents)
                    batch.Delete(doc.Reference);

                var commentsSnap = await postRef.Collection("comments").GetSnapshotAsync();
                foreach (var doc in commentsSnap.Documents)
                    batch.Delete(doc.Reference);

                batch.Delete(postRef);
                await batch.CommitAsync();

                return Results.Ok(new { message = "Đã xóa bài viết và các dữ liệu liên quan" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 8. TẠO BÌNH LUẬN
        // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // 9. SỬA BÌNH LUẬN
        // ---------------------------------------------------------
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
                    return Results.NotFound("Comment's not exist");

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                if (commentSnap.GetValue<string>("userId") != uid && !isAdmin)
                    return Results.StatusCode(403);

                await commentRef.UpdateAsync(new Dictionary<string, object>
                {
                    { "content", body.NewContent },
                    { "isEdited", true }
                });

                return Results.Ok(new { message = "Edited" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 10. XÓA BÌNH LUẬN
        // ---------------------------------------------------------
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

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                await db.RunTransactionAsync(async transaction =>
                {
                    var postSnap = await transaction.GetSnapshotAsync(postRef);
                    var commentSnap = await transaction.GetSnapshotAsync(commentRef);

                    if (!postSnap.Exists || !commentSnap.Exists)
                        throw new Exception("Không tìm thấy dữ liệu");

                    var postOwnerId = postSnap.GetValue<string>("userId");
                    var commentOwnerId = commentSnap.GetValue<string>("userId");

                    if (uid != commentOwnerId && uid != postOwnerId && !isAdmin)
                        throw new Exception("Không có quyền xóa");

                    transaction.Delete(commentRef);

                    long currentComments = postSnap.GetValue<long>("commentCount");
                    transaction.Update(postRef, "commentCount", Math.Max(0, currentComments - 1));
                });

                return Results.Ok(new { message = "Đã xóa bình luận" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 11. BATCH PROFILE FETCH
        // ---------------------------------------------------------
        group.MapPost("/user/batch-profiles", async (HttpRequest request, BatchProfileRequest body, FirestoreDb db) =>
        {
            try
            {
                if (body.UserIds == null || body.UserIds.Count == 0)
                    return Results.Ok(new Dictionary<string, object>());

                var profiles = new Dictionary<string, object>();
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

        // ---------------------------------------------------------
        // 12. GỬI YÊU CẦU KẾT BẠN
        // ---------------------------------------------------------
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

                var sentRequestData = new Dictionary<string, object> { { "toUid", body.TargetUid }, { "timestamp", timestamp } };
                var incomingRequestData = new Dictionary<string, object> { { "fromUid", uid }, { "timestamp", timestamp } };

                batch.Set(currentUserRef.Collection("sent_requests").Document(body.TargetUid), sentRequestData);
                batch.Set(targetUserRef.Collection("friend_requests").Document(uid), incomingRequestData);

                var notifRef = db.Collection("users").Document(body.TargetUid).Collection("notifications").Document();
                batch.Set(notifRef, new
                {
                    type = "FRIEND_REQUEST",
                    title = "Friend request",
                    message = $"{senderName} has sent you a friend request.",
                    timestamp = timestamp,
                    isRead = false,
                    referenceId = uid
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "sent request successfully" });
            }
            catch (Exception ex)
            {
                return Results.Problem(title: "Gửi lời mời thất bại", detail: ex.Message, statusCode: 500);
            }
        });

        // ---------------------------------------------------------
        // 13. CHẤP NHẬN YÊU CẦU KẾT BẠN
        // ---------------------------------------------------------
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

                batch.Delete(db.Collection("users").Document(uid).Collection("friend_requests").Document(body.TargetUid));
                batch.Delete(db.Collection("users").Document(body.TargetUid).Collection("sent_requests").Document(uid));

                batch.Set(db.Collection("users").Document(uid).Collection("friends").Document(body.TargetUid), new { timestamp });
                batch.Set(db.Collection("users").Document(body.TargetUid).Collection("friends").Document(uid), new { timestamp });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Đã trở thành bạn bè" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 14. TỪ CHỐI / XÓA BẠN BÈ
        // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // 15. GỢI Ý KẾT BẠN
        // ---------------------------------------------------------
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
                return Results.Problem(title: "Tải gợi ý kết bạn thất bại", detail: ex.Message, statusCode: 500);
            }
        });

        // ---------------------------------------------------------
        // 16. LẤY CHI TIẾT 1 BÀI VIẾT
        // ---------------------------------------------------------
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

                var likeSnap = await postRef.Collection("likes").Document(uid).GetSnapshotAsync();
                postDict["isLiked"] = likeSnap.Exists;

                return Results.Ok(postDict);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });
    }
}