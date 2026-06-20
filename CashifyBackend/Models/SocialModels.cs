namespace CashifyBackend.Models;
// --- NHÓM MODEL CỦA SOCIAL ---
public record FeedRequest(List<string> FriendIds, long LastTimestamp, int Limit);
public record CreatePostRequest(string? Title, string? Content, string? ImageUrl, string? Type, string? MilestoneData, string? Audience); 
public record LikeActionRequest(string PostId);
public record AddCommentRequest(string PostId, string Content);
public record DeletePostRequest(string PostId);
public record BatchProfileRequest(List<string> UserIds);
public record DeleteCommentRequest(string PostId, string CommentId);
public record EditPostRequest(string PostId, string? Title, string NewContent, string? NewImageUrl, string? Audience);
public record EditCommentRequest(string PostId, string CommentId, string NewContent);
public record AutoMilestoneRequest(long LimitAmount, long SpentAmount, string PeriodType, string PeriodLabel);
public class AchievementSuggestion
{
    public string Id { get; set; }
    public string Title { get; set; }
    public string Description { get; set; }
    public string IconText { get; set; }
    public string AmountLabel { get; set; }
    public string MonthLabel { get; set; }
    public int Progress { get; set; }
}
// --- NHÓM MODEL CỦA BẠN BÈ ---
public record FriendActionRequest(string? TargetUid);
