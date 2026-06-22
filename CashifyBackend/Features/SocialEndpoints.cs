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

                // Kiểm tra quyền Admin
                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = userSnap.ContainsField("role") && userSnap.GetValue<string>("role") == "ADMIN";

                // Lấy danh sách các bài viết đã bị ẩn
                var hiddenSnap = await db.Collection("users").Document(uid).Collection("hidden_posts").GetSnapshotAsync();
                var hiddenPostIds = new HashSet<string>(hiddenSnap.Documents.Select(d => d.Id));
                Query query = db.Collection("posts");

                // Giới hạn feed theo danh sách bạn bè nếu không phải Admin
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

                // Gắn trạng thái like của user đang xem cho từng bài viết
                var likeCheckTasks = snapshot.Documents.Select(async doc =>
                {
                    var postDict = doc.ToDictionary();
                    var likeSnap = await db.Collection("posts").Document(doc.Id).Collection("likes").Document(uid).GetSnapshotAsync();
                    postDict["isLiked"] = likeSnap.Exists;
                    return postDict;
                });

                var posts = (await Task.WhenAll(likeCheckTasks))
                                            .Where(p => !hiddenPostIds.Contains(p["postId"].ToString())) // BỘ LỌC
                                            .ToList();
                return Results.Ok(posts);
            }
            catch (Exception ex) { return Results.Problem($"Failed to fetch feed: {ex.Message}"); }
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

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                //Lấy danh sách các Bình Luận đã bị ẩn của User này
                var hiddenSnap = await db.Collection("users").Document(uid).Collection("hidden_comments").GetSnapshotAsync();
                //Vì lúc lưu ta dùng key là "{postId}_{commentId}", nên giờ phải tách ra lấy commentId
                var hiddenCommentIds = new HashSet<string>(
                    hiddenSnap.Documents
                              .Where(d => d.Id.StartsWith($"{postId}_"))
                              .Select(d => d.Id.Split('_')[1])
                );

                // Tải toàn bộ bình luận của bài viết
                var commentsRef = db.Collection("posts").Document(postId).Collection("comments");
                var snapshot = await commentsRef.OrderBy("timestamp").GetSnapshotAsync();

                //Bỏ qua những bình luận nằm trong danh sách đen
                var comments = snapshot.Documents
                                       .Where(doc => !hiddenCommentIds.Contains(doc.Id))
                                       .Select(doc => doc.ToDictionary())
                                       .ToList();

                return Results.Ok(comments);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 3. TẢI BÀI TRÊN TƯỜNG CÁ NHÂN
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

                // Lấy danh sách các bài viết đã bị ẩn
                var hiddenSnap = await db.Collection("users").Document(currentViewerUid).Collection("hidden_posts").GetSnapshotAsync();
                var hiddenPostIds = new HashSet<string>(hiddenSnap.Documents.Select(d => d.Id));

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

                var posts = (await Task.WhenAll(likeCheckTasks))
                                            .Where(p => !hiddenPostIds.Contains(p["postId"].ToString())) // BỘ LỌC
                                            .ToList();
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

                if (string.IsNullOrEmpty(body.Title) && string.IsNullOrEmpty(body.Content) && string.IsNullOrEmpty(body.ImageUrl) && string.IsNullOrEmpty(body.MilestoneData))
                    return Results.BadRequest("Title or content cannot be empty");

                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                var authorName = userSnap.Exists && userSnap.ContainsField("displayName")
                    ? userSnap.GetValue<string>("displayName")
                    : "Cashify User";

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
             { "audience", body.Audience ?? "FRIENDS" },
             { "title", body.Title ?? "" },
             { "content", body.Content ?? "" },
             { "imageUrl", body.ImageUrl ?? "" },
             { "milestoneData", body.MilestoneData },
             { "likeCount", 0 },
             { "commentCount", 0 },
             { "timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() },
             { "isEdited", false }
         };

                // Đánh dấu thành tựu đã được chia sẻ nếu đây là bài post Milestone
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
                    catch (Exception) { /* Bỏ qua lỗi parse JSON */ }
                }

                await postRef.SetAsync(postData);
                return Results.Ok(new { message = "Post created successfully", postId });
            }
            catch (Exception ex) { return Results.Problem($"Failed to create post: {ex.Message}"); }
        });

        // Tải các thành tựu chưa chia sẻ của User
        group.MapGet("/achievements/available", async (HttpRequest request, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;
                var availableList = new List<AchievementSuggestion>();

                // Lọc các thành tựu đã chia sẻ
                var sharedSnap = await db.Collection("users").Document(uid).Collection("shared_achievements").GetSnapshotAsync();
                var sharedIds = new HashSet<string>(sharedSnap.Documents.Select(d => d.Id));

                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                int streakDays = userSnap.ContainsField("streakDays") ? Convert.ToInt32(userSnap.GetValue<object>("streakDays")) : 0;

                var statsSnap = await db.Collection("users").Document(uid).Collection("user_stats").Document("summary").GetSnapshotAsync();

                var now = DateTimeOffset.UtcNow;
                string monthKey = $"{now.Year}_{now.Month:D2}";
                int totalTrans = 0;
                long totalIncomeThisMonth = 0;
                long totalSpendThisMonth = 0;

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
                // THÀNH TỰU CÁ NHÂN
                // ==========================================

                // Milestone số lượng giao dịch
                int[] transMilestones = { 10, 50, 100, 500 };
                foreach (var m in transMilestones)
                {
                    string achId = $"ach_trans_{m}";
                    if (totalTrans >= m && !sharedIds.Contains(achId))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = achId,
                            Title = "Hardworking Bee",
                            Description = $"Recorded {m} transactions on Cashify!",
                            IconText = "🐝",
                            MonthLabel = "Lifetime",
                            AmountLabel = $"{m} Entries",
                            Progress = 100
                        });
                        break;
                    }
                }

                // Milestone chuỗi ngày liên tiếp
                int[] streakMilestones = { 3, 7, 15, 30, 100 };
                foreach (var s in streakMilestones)
                {
                    string achId = $"ach_streak_{s}";
                    if (streakDays >= s && !sharedIds.Contains(achId))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = achId,
                            Title = "Iron Discipline",
                            Description = $"You've tracked expenses for {s} consecutive days!",
                            IconText = "🔥",
                            MonthLabel = "Streak",
                            AmountLabel = $"{s} Days",
                            Progress = 100
                        });
                        break;
                    }
                }

                if (statsSnap.Exists)
                {
                    // Thành tựu Cú Đêm
                    bool isNightOwl = statsSnap.ContainsField("nightOwlUnlocked") && Convert.ToBoolean(statsSnap.GetValue<object>("nightOwlUnlocked"));
                    if (isNightOwl && !sharedIds.Contains("ach_night_owl"))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = "ach_night_owl",
                            Title = "Night Owl",
                            Description = "Tracking expenses at 2 AM is a lifestyle.",
                            IconText = "🦉",
                            MonthLabel = "Lifetime",
                            AmountLabel = "Night Owl",
                            Progress = 0
                        });
                    }

                    // Thành tựu Big Spender
                    bool isBigSpender = statsSnap.ContainsField("bigSpenderUnlocked") && Convert.ToBoolean(statsSnap.GetValue<object>("bigSpenderUnlocked"));
                    if (isBigSpender && !sharedIds.Contains("ach_big_spender"))
                    {
                        availableList.Add(new AchievementSuggestion
                        {
                            Id = "ach_big_spender",
                            Title = "Big Whale",
                            Description = "You spent more in one go than most do in a month!",
                            IconText = "🐋",
                            MonthLabel = "Lifetime",
                            AmountLabel = "> 10M VND",
                            Progress = 0
                        });
                    }
                }

                // Thành tựu Tiết kiệm tháng
                long surplus = totalIncomeThisMonth - totalSpendThisMonth;
                string surplusAchId = $"recap_surplus_{now.Year}_{now.Month}";
                if (surplus > 0 && !sharedIds.Contains(surplusAchId))
                {
                    int savedPercent = 0;
                    if (totalIncomeThisMonth > 0)
                    {
                        savedPercent = (int)((surplus * 100.0) / totalIncomeThisMonth);
                    }

                    availableList.Add(new AchievementSuggestion
                    {
                        Id = surplusAchId,
                        Title = "Healthy Finances",
                        Description = $"You saved {savedPercent}% of your income this month. Keep it up!",
                        IconText = "💰",
                        MonthLabel = $"{now.Month}/{now.Year}",
                        AmountLabel = $"Surplus: {surplus:N0}đ",
                        Progress = savedPercent > 0 ? savedPercent : 0
                    });
                }

                // ==========================================
                // THÀNH TỰU NHÓM
                // ==========================================
                var workspacesSnap = await db.Collection("workspaces").WhereArrayContains("members", uid).GetSnapshotAsync();

                foreach (var wsDoc in workspacesSnap.Documents)
                {
                    string wsId = wsDoc.Id;
                    string wsName = wsDoc.ContainsField("name") ? wsDoc.GetValue<string>("name") : "Group";
                    var wsStatsSnap = await wsDoc.Reference.Collection("workspace_stats").Document("summary").GetSnapshotAsync();

                    if (wsStatsSnap.Exists)
                    {
                        string carryId = wsStatsSnap.ContainsField("theCarryId") ? wsStatsSnap.GetValue<string>("theCarryId") : "";
                        long maxIncome = wsStatsSnap.ContainsField("maxSingleIncome") ? Convert.ToInt64(wsStatsSnap.GetValue<object>("maxSingleIncome")) : 0;
                        string dynamicCarryId = $"ws_the_carry_{wsId}_{maxIncome}";

                        if (uid == carryId && maxIncome > 0 && !sharedIds.Contains(dynamicCarryId))
                        {
                            availableList.Add(new AchievementSuggestion
                            {
                                Id = dynamicCarryId,
                                Title = "The Carry",
                                Description = "You are the MVP! The largest single contributor.",
                                IconText = "🦸‍♂️",
                                MonthLabel = wsName,
                                AmountLabel = $"{maxIncome:N0}đ",
                                Progress = 0
                            });
                        }

                        string spenderId = wsStatsSnap.ContainsField("biggestSpenderId") ? wsStatsSnap.GetValue<string>("biggestSpenderId") : "";
                        long maxSpend = wsStatsSnap.ContainsField("maxSingleSpend") ? Convert.ToInt64(wsStatsSnap.GetValue<object>("maxSingleSpend")) : 0;
                        string dynamicSpenderId = $"ws_biggest_spender_{wsId}_{maxSpend}";

                        if (uid == spenderId && maxSpend > 0 && !sharedIds.Contains(dynamicSpenderId))
                        {
                            availableList.Add(new AchievementSuggestion
                            {
                                Id = dynamicSpenderId,
                                Title = "Biggest Spender",
                                Description = "You hold the spending record!",
                                IconText = "🛍️",
                                MonthLabel = wsName,
                                AmountLabel = $"{maxSpend:N0}đ",
                                Progress = 0
                            });
                        }
                    }
                }

                return Results.Ok(availableList);
            }
            catch (Exception ex) { return Results.Problem($"Error fetching achievements: {ex.Message}"); }
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
                        throw new Exception("Post not found");

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

                return Results.Ok(new { message = "Like/Unlike action successful" });
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });

        // ---------------------------------------------------------
        // 6. SỬA BÀI VIẾT
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
                    return Results.NotFound("Post not found");

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                if (postSnap.GetValue<string>("userId") != uid && !isAdmin)
                    return Results.StatusCode(403);

                var updates = new Dictionary<string, object>
                {
                    { "title", body.Title ?? "" },
                    { "content", body.NewContent ?? "" },
                    { "isEdited", true }
                };
                if (body.NewImageUrl != null)
                    updates["imageUrl"] = body.NewImageUrl;

                // Chỉ update Audience nếu client gửi lên
                if (body.Audience != null)
                    updates["audience"] = body.Audience;

                await postRef.UpdateAsync(updates);
                return Results.Ok(new { message = "Post updated successfully" });
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
                    return Results.NotFound("Post not found");

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

                return Results.Ok(new { message = "Post and related data deleted successfully" });
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
                    ? userSnap.GetValue<string>("displayName") : "Cashify User";
                var authorAvatarUrl = userSnap.Exists && userSnap.ContainsField("avatarUrl")
                    ? userSnap.GetValue<string>("avatarUrl") : "";

                await db.RunTransactionAsync(async transaction =>
                {
                    DocumentSnapshot postSnap = await transaction.GetSnapshotAsync(postRef);
                    if (!postSnap.Exists)
                        throw new Exception("Post not found");

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

                return Results.Ok(new { message = "Comment added successfully", commentId });
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
                    return Results.NotFound("Comment not found");

                var currentUserSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                bool isAdmin = currentUserSnap.ContainsField("role") && currentUserSnap.GetValue<string>("role") == "ADMIN";

                if (commentSnap.GetValue<string>("userId") != uid && !isAdmin)
                    return Results.StatusCode(403);

                await commentRef.UpdateAsync(new Dictionary<string, object>
                {
                    { "content", body.NewContent ?? "" },
                    { "isEdited", true }
                });

                return Results.Ok(new { message = "Comment updated successfully" });
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
                        throw new Exception("Data not found");

                    var postOwnerId = postSnap.GetValue<string>("userId");
                    var commentOwnerId = commentSnap.GetValue<string>("userId");

                    if (uid != commentOwnerId && uid != postOwnerId && !isAdmin)
                        throw new Exception("Permission denied");

                    transaction.Delete(commentRef);

                    long currentComments = postSnap.GetValue<long>("commentCount");
                    transaction.Update(postRef, "commentCount", Math.Max(0, currentComments - 1));
                });

                return Results.Ok(new { message = "Comment deleted successfully" });
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
                            displayName = userSnap.ContainsField("displayName") ? userSnap.GetValue<string>("displayName") : "Cashify User",
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
                    return Results.BadRequest("Invalid UID");

                var currentUserRef = db.Collection("users").Document(uid);
                var targetUserRef = db.Collection("users").Document(body.TargetUid);
                var targetUserSnap = await targetUserRef.GetSnapshotAsync();

                if (!targetUserSnap.Exists)
                    return Results.NotFound(new { message = "Target user not found" });

                var existingFriendSnap = await currentUserRef.Collection("friends").Document(body.TargetUid).GetSnapshotAsync();
                if (existingFriendSnap.Exists)
                    return Results.Conflict(new { message = "User is already your friend" });

                var existingSentRequestSnap = await currentUserRef.Collection("sent_requests").Document(body.TargetUid).GetSnapshotAsync();
                if (existingSentRequestSnap.Exists)
                    return Results.Conflict(new { message = "Friend request already sent" });

                var incomingRequestSnap = await currentUserRef.Collection("friend_requests").Document(body.TargetUid).GetSnapshotAsync();
                if (incomingRequestSnap.Exists)
                    return Results.Conflict(new { message = "User has already sent you a request" });

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
                return Results.Ok(new { message = "Friend request sent successfully" });
            }
            catch (Exception ex)
            {
                return Results.Problem(title: "Failed to send friend request", detail: ex.Message, statusCode: 500);
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
                    return Results.BadRequest("Invalid UID");

                var batch = db.StartBatch();
                var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

                batch.Delete(db.Collection("users").Document(uid).Collection("friend_requests").Document(body.TargetUid));
                batch.Delete(db.Collection("users").Document(body.TargetUid).Collection("sent_requests").Document(uid));

                batch.Set(db.Collection("users").Document(uid).Collection("friends").Document(body.TargetUid), new { timestamp });
                batch.Set(db.Collection("users").Document(body.TargetUid).Collection("friends").Document(uid), new { timestamp });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Friend request accepted" });
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
                    return Results.BadRequest("Invalid UID");

                var batch = db.StartBatch();
                string[] collections = { "friends", "friend_requests", "sent_requests" };
                foreach (var col in collections)
                {
                    batch.Delete(db.Collection("users").Document(uid).Collection(col).Document(body.TargetUid));
                    batch.Delete(db.Collection("users").Document(body.TargetUid).Collection(col).Document(uid));
                }

                await batch.CommitAsync();
                return Results.Ok(new { message = "Action successful" });
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
                return Results.Problem(title: "Failed to load friend suggestions", detail: ex.Message, statusCode: 500);
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
                    return Results.NotFound(new { message = "Post not found" });

                var postDict = postSnap.ToDictionary();

                var likeSnap = await postRef.Collection("likes").Document(uid).GetSnapshotAsync();
                postDict["isLiked"] = likeSnap.Exists;

                return Results.Ok(postDict);
            }
            catch (Exception ex) { return Results.Problem(ex.Message); }
        });
        // ---------------------------------------------------------
        // 17. ẨN BÀI VIẾT (HIDE POST)
        // ---------------------------------------------------------
        group.MapPost("/post/hide", async (HttpRequest request, HidePostRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                if (string.IsNullOrEmpty(body.PostId))
                    return Results.BadRequest("Post ID cannot be empty");

                // Thêm vào sub-collection "hidden_posts" của User
                var hideRef = db.Collection("users").Document(uid).Collection("hidden_posts").Document(body.PostId);
                await hideRef.SetAsync(new { hiddenAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                return Results.Ok(new { message = "Post hidden successfully" });
            }
            catch (Exception ex) { return Results.Problem($"Failed to hide post: {ex.Message}"); }
        });

        // ---------------------------------------------------------
        // 18. ẨN BÌNH LUẬN (HIDE COMMENT)
        // ---------------------------------------------------------
        group.MapPost("/comment/hide", async (HttpRequest request, HideCommentRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                if (string.IsNullOrEmpty(body.PostId) || string.IsNullOrEmpty(body.CommentId))
                    return Results.BadRequest("Post ID and Comment ID cannot be empty");

                // Thêm vào sub-collection "hidden_comments" của User
                //gộp key là {postId}_{commentId} để dễ check sau này
                string hiddenKey = $"{body.PostId}_{body.CommentId}";
                var hideRef = db.Collection("users").Document(uid).Collection("hidden_comments").Document(hiddenKey);
                await hideRef.SetAsync(new { hiddenAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                return Results.Ok(new { message = "Comment hidden successfully" });
            }
            catch (Exception ex) { return Results.Problem($"Failed to hide comment: {ex.Message}"); }
        });
    }
}