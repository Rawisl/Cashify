namespace CashifyBackend.Models;
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