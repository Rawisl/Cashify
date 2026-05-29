namespace CashifyBackend.Models;
// --- NHÓM MODEL CỦA SOCIAL ---
public record FeedRequest(List<string> FriendIds, long LastTimestamp, int Limit);
public record CreatePostRequest(string Content, string? ImageUrl, string? Type, string? MilestoneData, string? Audience); 
public record LikeActionRequest(string PostId);
public record AddCommentRequest(string PostId, string Content);
public record DeletePostRequest(string PostId);
public record BatchProfileRequest(List<string> UserIds);
public record DeleteCommentRequest(string PostId, string CommentId);
public record EditPostRequest(string PostId, string NewContent, string? NewImageUrl, string? Audience); 
public record EditCommentRequest(string PostId, string CommentId, string NewContent);
public record AutoMilestoneRequest(long LimitAmount, long SpentAmount, string PeriodType, string PeriodLabel);
// --- NHÓM MODEL CỦA BẠN BÈ ---
public record FriendActionRequest(string? TargetUid);
