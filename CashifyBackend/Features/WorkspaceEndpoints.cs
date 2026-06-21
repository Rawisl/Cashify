using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using CashifyBackend.Models;

namespace CashifyBackend.Features;

public static class WorkspaceEndpoints
{
    public static void MapWorkspaceEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/v1/workspace");

        // CREATE WORKSPACE (WITH DEFAULT DATA AND LOG)
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
                    return Results.BadRequest("Fund name cannot be empty");

                var batch = db.StartBatch();

                var wsRef = db.Collection("workspaces").Document();
                var workspaceData = new Dictionary<string, object>
        {
            { "name", body.Name },
            { "ownerId", uid },
            { "members", new List<string> { uid } },
            { "type", body.Type ?? "Empty" },
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
                    message = "initialized this group fund",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Fund created successfully!", workspaceId = wsRef.Id });
            }
            catch (Exception ex)
            {
                return Results.Problem($"System error: {ex.Message}");
            }
        });

        // LEAVE OR DELETE WORKSPACE
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
                    return Results.BadRequest("Missing WorkspaceId");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Fund not found" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

                var batch = db.StartBatch();

                // SCENARIO 1: OWNER LEAVING
                if (uid == ownerId)
                {
                    if (members.Count > 1)
                        return Results.BadRequest(new { error = "REQUIRE_TRANSFER", message = "Fund still has members. Please transfer ownership before leaving!" });

                    // If owner is the only member, delete the workspace entirely
                    string[] subCollections = { "transactions", "categories", "logs", "messages" };
                    foreach (var sub in subCollections)
                    {
                        var subDocs = await wsRef.Collection(sub).GetSnapshotAsync();
                        foreach (var doc in subDocs.Documents)
                            batch.Delete(doc.Reference);
                    }
                    batch.Delete(wsRef);
                }
                // SCENARIO 2: REGULAR MEMBER LEAVING
                else
                {
                    members.Remove(uid);
                    batch.Update(wsRef, "members", members);
                    var logRef = wsRef.Collection("logs").Document();
                    batch.Set(logRef, new { actionType = "LEAVE_WORKSPACE", message = "left the fund", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
                }

                await batch.CommitAsync();
                return Results.Ok(new { message = "Operation successful" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }

        });

        // TRANSFER OWNERSHIP
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
                    return Results.BadRequest("Missing Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Fund not found!" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                // Only the current owner can transfer ownership
                if (uid != ownerId)
                    return Results.StatusCode(403);
                if (!members.Contains(body.NewOwnerId))
                    return Results.BadRequest(new { error = "Recipient not found in Fund" });

                var batch = db.StartBatch();
                batch.Update(wsRef, "ownerId", body.NewOwnerId);

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new { actionType = "TRANSFER_OWNER", message = "transferred group ownership", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Ownership transferred successfully" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // ADD OR EDIT TRANSACTION (WITH GROUP GAMIFICATION)
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
                    return Results.BadRequest(new { error = "Invalid budget amount" });
                if (string.IsNullOrEmpty(body.WorkspaceId) || body.WorkspaceId == "PERSONAL")
                    return Results.BadRequest(new { error = "This API is strictly for Group funds" });


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();
                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Group not found" });

                var members = wsSnap.GetValue<List<string>>("members");
                var ownerId = wsSnap.GetValue<string>("ownerId");

                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

                var transId = !string.IsNullOrEmpty(body.Id) ? body.Id : Guid.NewGuid().ToString();
                var transRef = wsRef.Collection("transactions").Document(transId);

                // Check edit permissions if updating
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

                // ==========================================
                // GAMIFICATION: TRACK "THE CARRY" AND "BIGGEST SPENDER"
                // ==========================================
                var statsRef = wsRef.Collection("workspace_stats").Document("summary");
                var statsSnap = await statsRef.GetSnapshotAsync();

                var statsUpdate = new Dictionary<string, object>();

                // Flags to notify client of new achievements
                bool isNewCarry = false;
                bool isNewSpender = false;

                // Only award achievements on new inserts
                if (string.IsNullOrEmpty(body.Id))
                {
                    if (body.Type == 1) // Income
                    {
                        long currentMaxIncome = statsSnap.Exists && statsSnap.ContainsField("maxSingleIncome") ? Convert.ToInt64(statsSnap.GetValue<object>("maxSingleIncome")) : 0;
                        if (body.Amount > currentMaxIncome && body.Amount >= 1000000)
                        {
                            statsUpdate["maxSingleIncome"] = body.Amount;
                            statsUpdate["theCarryId"] = uid;
                            statsUpdate["theCarryName"] = senderName;
                            isNewCarry = true; // Trigger flag
                        }
                    }
                    else if (body.Type == 0) // Expense
                    {
                        long currentMaxSpend = statsSnap.Exists && statsSnap.ContainsField("maxSingleSpend") ? Convert.ToInt64(statsSnap.GetValue<object>("maxSingleSpend")) : 0;
                        if (body.Amount > currentMaxSpend && body.Amount >= 5000000)
                        {
                            statsUpdate["maxSingleSpend"] = body.Amount;
                            statsUpdate["biggestSpenderId"] = uid;
                            statsUpdate["biggestSpenderName"] = senderName;
                            isNewSpender = true; // Trigger flag
                        }
                    }
                }

                if (statsUpdate.Count > 0)
                {
                    batch.Set(statsRef, statsUpdate, SetOptions.MergeAll);
                }


                // ==========================================
                // SEND IN-APP NOTIFICATIONS TO OTHER MEMBERS
                // ==========================================
                string actionName = string.IsNullOrEmpty(body.Id) ? "added" : "updated";
                foreach (var memberId in members)
                {
                    if (memberId != uid) // Skip sending to self
                    {
                        var notifRef = db.Collection("users").Document(memberId).Collection("notifications").Document();
                        batch.Set(notifRef, new
                        {
                            type = "WORKSPACE_TRANS",
                            title = "Group fund fluctuation",
                            message = $"{senderName} has just {actionName} a transaction in the group fund.",
                            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            isRead = false,
                            referenceId = body.WorkspaceId
                        });
                    }
                }

                var logRef = wsRef.Collection("logs").Document();
                string logAction = string.IsNullOrEmpty(body.Id) ? "ADD_TRANSACTION" : "EDIT_TRANSACTION";
                string logMessage = string.IsNullOrEmpty(body.Id) ? $"added a transaction of {body.Amount}" : "updated a transaction";

                batch.Set(logRef, new
                {
                    actionType = logAction,
                    message = logMessage,
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new
                {
                    message = "Group transaction saved successfully",
                    id = transId,
                    isNewCarry = isNewCarry,
                    isNewSpender = isNewSpender
                });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // DELETE TRANSACTION (WITH ANTI-CHEAT GAMIFICATION)
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

                // ==========================================
                // ANTI-CHEAT: CHECK IF DELETED TRANSACTION WAS A RECORD
                // ==========================================
                long deletedAmount = transSnap.ContainsField("amount") ? Convert.ToInt64(transSnap.GetValue<object>("amount")) : 0;
                int deletedType = transSnap.ContainsField("type") ? Convert.ToInt32(transSnap.GetValue<object>("type")) : 0;

                var statsRef = wsRef.Collection("workspace_stats").Document("summary");
                var statsSnap = await statsRef.GetSnapshotAsync();

                bool needRecalculateSpend = false;
                bool needRecalculateIncome = false;

                if (statsSnap.Exists)
                {
                    if (deletedType == 0) // Expense
                    {
                        long currentMaxSpend = statsSnap.ContainsField("maxSingleSpend") ? Convert.ToInt64(statsSnap.GetValue<object>("maxSingleSpend")) : 0;
                        if (deletedAmount == currentMaxSpend)
                            needRecalculateSpend = true;
                    }
                    else if (deletedType == 1) // Income
                    {
                        long currentMaxIncome = statsSnap.ContainsField("maxSingleIncome") ? Convert.ToInt64(statsSnap.GetValue<object>("maxSingleIncome")) : 0;
                        if (deletedAmount == currentMaxIncome)
                            needRecalculateIncome = true;
                    }
                }

                var batch = db.StartBatch();
                batch.Delete(transRef);

                // ==========================================
                // ANTI-CHEAT: FIND NEW RECORD HOLDER IF NEEDED
                // ==========================================
                var statsUpdate = new Dictionary<string, object>();

                if (needRecalculateSpend || needRecalculateIncome)
                {
                    // Re-evaluate all transactions of the same type
                    var allTransSnap = await wsRef.Collection("transactions").WhereEqualTo("type", deletedType).GetSnapshotAsync();

                    long newMax = 0;
                    string newHolderId = "";

                    foreach (var doc in allTransSnap.Documents)
                    {
                        if (doc.Id == body.TransactionId)
                            continue; // Skip the deleted transaction

                        long amt = Convert.ToInt64(doc.GetValue<object>("amount"));
                        if (amt > newMax)
                        {
                            newMax = amt;
                            newHolderId = doc.GetValue<string>("userId");
                        }
                    }

                    // Fetch new record holder's name
                    string newHolderName = "A member";
                    if (!string.IsNullOrEmpty(newHolderId))
                    {
                        var newHolderSnap = await db.Collection("users").Document(newHolderId).GetSnapshotAsync();
                        if (newHolderSnap.Exists && newHolderSnap.ContainsField("displayName"))
                            newHolderName = newHolderSnap.GetValue<string>("displayName");
                    }

                    if (needRecalculateSpend)
                    {
                        if (newMax >= 5000000)
                        {
                            statsUpdate["maxSingleSpend"] = newMax;
                            statsUpdate["biggestSpenderId"] = newHolderId;
                            statsUpdate["biggestSpenderName"] = newHolderName;
                        }
                        else
                        {
                            // Revoke achievement if no one meets the threshold
                            statsUpdate["maxSingleSpend"] = 0;
                            statsUpdate["biggestSpenderId"] = "";
                            statsUpdate["biggestSpenderName"] = "";
                        }
                    }
                    else if (needRecalculateIncome)
                    {
                        if (newMax >= 1000000)
                        {
                            statsUpdate["maxSingleIncome"] = newMax;
                            statsUpdate["theCarryId"] = newHolderId;
                            statsUpdate["theCarryName"] = newHolderName;
                        }
                        else
                        {
                            statsUpdate["maxSingleIncome"] = 0;
                            statsUpdate["theCarryId"] = "";
                            statsUpdate["theCarryName"] = "";
                        }
                    }
                }

                if (statsUpdate.Count > 0)
                {
                    batch.Set(statsRef, statsUpdate, SetOptions.MergeAll);
                }

                // Save audit log
                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new { actionType = "DELETE_TRANSACTION", message = "deleted a transaction", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                // SEND IN-APP NOTIFICATIONS
                foreach (var memberId in members)
                {
                    if (memberId != uid)
                    {
                        var notifRef = db.Collection("users").Document(memberId).Collection("notifications").Document();
                        batch.Set(notifRef, new
                        {
                            type = "WORKSPACE_TRANS",
                            title = "Transaction deleted",
                            message = $"{senderName} has just deleted a transaction from the group fund.",
                            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            isRead = false,
                            referenceId = body.WorkspaceId
                        });
                    }
                }

                await batch.CommitAsync();
                return Results.Ok(new { message = "Transaction deleted successfully" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // SEND WORKSPACE INVITATIONS
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

                    // SEND IN-APP NOTIFICATION
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
                return Results.Ok(new { message = "Invitation sent successfully!" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // ACCEPT INVITATION
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
                    return Results.BadRequest(new { error = "WORKSPACE_DELETED", message = "Group has been deleted or does not exist" });
                }

                var batch = db.StartBatch();
                batch.Update(wsRef, "members", FieldValue.ArrayUnion(uid));
                batch.Delete(inviteRef);

                // Save audit log for joining
                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new
                {
                    actionType = "JOIN_WORKSPACE",
                    message = "joined the fund",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Joined group successfully!" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // DECLINE INVITATION
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

                // Delete the invitation
                var inviteRef = db.Collection("users").Document(uid).Collection("workspace_invitations").Document(body.InvitationId);
                await inviteRef.DeleteAsync();

                return Results.Ok(new { message = "Rejected the invitation." });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // KICK MEMBER
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
                    return Results.BadRequest("Missing WorkspaceId");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Fund not found" });

                var ownerId = wsSnap.GetValue<string>("ownerId");
                var members = wsSnap.GetValue<List<string>>("members");

                if (uid != ownerId)
                    return Results.StatusCode(403);

                if (body.TargetUid == ownerId)
                    return Results.BadRequest(new { error = "You cannot kick yourself." });

                if (members == null || !members.Contains(body.TargetUid))
                    return Results.BadRequest(new { error = "This member is no longer in the fund." });

                var batch = db.StartBatch();
                members.Remove(body.TargetUid);
                batch.Update(wsRef, "members", members);

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new
                {
                    actionType = "KICK_MEMBER",
                    message = "kicked a member",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Member kicked successfully" });
            }
            catch (Exception ex)
            {
                return Results.Problem($"System error: {ex.Message}");
            }
        });

        // ADD NEW CATEGORY
        group.MapPost("/category/add", async (HttpRequest request, AddCategoryRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var token = authHeader.Substring("Bearer ".Length);
                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token)).Uid;

                // Basic validation
                if (string.IsNullOrEmpty(body.WorkspaceId) || string.IsNullOrEmpty(body.Name))
                    return Results.BadRequest("Missing required category information");

                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Fund not found");

                // Security: Only Owner can modify category structure
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

                var batch = db.StartBatch();

                // 1. Initialize new document
                var newCatRef = wsRef.Collection("categories").Document();

                var categoryData = new Dictionary<string, object>
        {
            { "name", body.Name },
            { "iconName", string.IsNullOrEmpty(body.IconName) ? "ic_other" : body.IconName },
            { "colorCode", string.IsNullOrEmpty(body.ColorCode) ? "#313B60" : body.ColorCode },
            { "type", body.Type },
            { "isDefault", 0 },
            { "isDeleted", 0 },
            { "workspaceId", body.WorkspaceId } // Sync with Android model structure
        };

                batch.Set(newCatRef, categoryData);

                // 2. Save audit log
                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new
                {
                    actionType = "ADD_CATEGORY",
                    message = $"created a new category \"{body.Name}\"",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                // 3. Execute batch
                await batch.CommitAsync();

                return Results.Ok(new
                {
                    message = "Category added successfully!",
                    categoryId = newCatRef.Id // Return ID for client usage
                });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // SOFT DELETE CATEGORY
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
                    return Results.BadRequest("Missing Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Fund not found");

                // ONLY OWNER CAN DELETE CATEGORY
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

                var batch = db.StartBatch();
                var catRef = wsRef.Collection("categories").Document(body.CategoryId);

                // SOFT DELETE
                batch.Update(catRef, "isDeleted", 1);

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new { actionType = "DELETE_CATEGORY", message = "hid a category", userId = uid, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Category hidden successfully!" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // EDIT CATEGORY
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
                    return Results.BadRequest("Missing edit information");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Fund not found");

                // Security: Only Owner can edit categories
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

                var catRef = wsRef.Collection("categories").Document(body.CategoryId);
                var batch = db.StartBatch();

                batch.Update(catRef, new Dictionary<string, object>
                {
                    { "name", body.Name },
                    { "iconName", body.IconName },
                    { "colorCode", body.ColorCode },
                    { "type", body.Type }
                });

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new
                {
                    actionType = "EDIT_CATEGORY",
                    message = $"updated category \"{body.Name}\"",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Category updated successfully!" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // RESTORE CATEGORY
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
                    return Results.BadRequest("Missing Data");


                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound("Fund not found!");
                if (uid != wsSnap.GetValue<string>("ownerId"))
                    return Results.StatusCode(403);

                var catRef = wsRef.Collection("categories").Document(body.CategoryId);
                var batch = db.StartBatch();

                batch.Update(catRef, "isDeleted", 0);

                var logRef = wsRef.Collection("logs").Document();
                batch.Set(logRef, new
                {
                    actionType = "RESTORE_CATEGORY",
                    message = "restored a category",
                    userId = uid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                });

                await batch.CommitAsync();
                return Results.Ok(new { message = "Category restored successfully!" });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // SEND WORKSPACE MESSAGE (WITH NOTIFICATION)
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
                (string.IsNullOrWhiteSpace(body.Text) &&
                string.IsNullOrWhiteSpace(body.ImageUrl)))
                    return Results.BadRequest(new { error = "Data lacking" });

                var wsRef = db.Collection("workspaces").Document(body.WorkspaceId);
                var wsSnap = await wsRef.GetSnapshotAsync();

                if (!wsSnap.Exists)
                    return Results.NotFound(new { error = "Fund not found" });

                var members = wsSnap.GetValue<List<string>>("members");
                // Must be a member to chat
                if (members == null || !members.Contains(uid))
                    return Results.StatusCode(403);

                var workspaceName = wsSnap.ContainsField("name") ? wsSnap.GetValue<string>("name") : "Group";

                var userSnap = await db.Collection("users").Document(uid).GetSnapshotAsync();
                string senderName = userSnap.Exists && userSnap.ContainsField("displayName") ? userSnap.GetValue<string>("displayName") : "User";
                string senderAvatar = userSnap.Exists && userSnap.ContainsField("avatarUrl") ? userSnap.GetValue<string>("avatarUrl") : "";

                var batch = db.StartBatch();
                var msgRef = wsRef.Collection("messages").Document();
                var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

                // Save message
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

                // Distribute notifications to other members
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
                return Results.Ok(new { message = "Message sent successfully", messageId = msgRef.Id });
            }
            catch (Exception ex) { return Results.Problem($"System error: {ex.Message}"); }
        });

        // RECALL WORKSPACE MESSAGE
        group.MapPost("/message/recall", async (HttpRequest request, MessageRecallRequest body, FirestoreDb db) =>
        {
            try
            {
                var authHeader = request.Headers["Authorization"].FirstOrDefault();
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                    return Results.Unauthorized();

                var uid = (await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(authHeader.Substring("Bearer ".Length))).Uid;

                if (string.IsNullOrWhiteSpace(body.WorkspaceId) || string.IsNullOrWhiteSpace(body.MessageId))
                    return Results.BadRequest(new { message = "Invalid information" });

                // 1. Point to the specific message document
                var messageRef = db.Collection("workspaces")
                                   .Document(body.WorkspaceId)
                                   .Collection("messages")
                                   .Document(body.MessageId);

                var messageSnap = await messageRef.GetSnapshotAsync();
                if (!messageSnap.Exists)
                    return Results.NotFound(new { message = "Message not found" });

                // 2. Security Check (Ownership)
                var senderId = messageSnap.ContainsField("senderId") ? messageSnap.GetValue<string>("senderId") : "";
                var workspaceSnap = await db.Collection("workspaces").Document(body.WorkspaceId).GetSnapshotAsync();
                var ownerId = workspaceSnap.ContainsField("ownerId") ? workspaceSnap.GetValue<string>("ownerId") : "";

                // Allow sender OR Owner to recall
                if (senderId != uid && ownerId != uid)
                    return Results.Json(new { message = "You do not have permission to unsend this message" }, statusCode: 403);

                // 3. Clear message content and set recall flag
                var updates = new Dictionary<string, object>
        {
            { "text", "" },
            { "imageUrl", "" },
            { "isRecalled", true }
        };

                await messageRef.UpdateAsync(updates);
                return Results.Ok(new { message = "Group message unsent successfully" });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Recall workspace message failed: {ex}");
                return Results.Problem(title: "Failed to unsend message", detail: ex.Message, statusCode: 500);
            }
        });
    }
}