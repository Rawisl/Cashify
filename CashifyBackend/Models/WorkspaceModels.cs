namespace CashifyBackend.Models;
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
public record WorkspaceInviteSendRequest(string? WorkspaceId, string? WorkspaceName, List<string>? TargetUids);
public record WorkspaceInviteHandleRequest(string? WorkspaceId, string? InvitationId);
