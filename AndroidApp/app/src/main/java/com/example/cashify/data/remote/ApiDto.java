package com.example.cashify.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Wrapper class chứa toàn bộ DTOs (Data Transfer Objects) giao tiếp với Backend
public class ApiDto {

    public static class CloudinarySignatureResponse {
        public String signature;
        public long timestamp;
        public String apiKey;
        public String cloudName;
        public String folder;
    }

    public static class ScanRequest {
        public String OcrText;
        public ScanRequest(String ocrText) { this.OcrText = ocrText; }
    }

    public static class TransactionRequest {
        public String Id;
        public long Amount;
        public String Note;
        public long Timestamp;
        public String PaymentMethod;
        public int Type;
        public String WorkspaceId;
        public String FirestoreCategoryId;
        public int CategoryId;
    }

    public static class WorkspaceCreateRequest {
        public String Name, Type, IconName;
        public WorkspaceCreateRequest(String name, String type, String icon) { Name = name; Type = type; IconName = icon; }
    }

    public static class WorkspaceCreateResponse {
        public String workspaceId;
        public String message;
    }

    public static class WorkspaceActionRequest {
        @SerializedName("workspaceId")   public String WorkspaceId;
        @SerializedName("newOwnerId")    public String NewOwnerId;
        @SerializedName("targetUid")     public String TargetUid;
        @SerializedName("transactionId") public String TransactionId;
        @SerializedName("categoryId")    public String CategoryId;
        @SerializedName("messageId")     public String MessageId;
    }

    public static class EditCategoryRequest {
        public String WorkspaceId, CategoryId, Name, IconName, ColorCode;
        public int Type;

        public EditCategoryRequest(String workspaceId, String categoryId, String name, String iconName, String colorCode, int type) {
            this.WorkspaceId = workspaceId; this.CategoryId = categoryId; this.Name = name;
            this.IconName = iconName; this.ColorCode = colorCode; this.Type = type;
        }
    }

    public static class AddCategoryRequest {
        public String WorkspaceId, Name, IconName, ColorCode;
        public int Type;
    }

    public static class FriendActionRequest {
        public String TargetUid;
        public FriendActionRequest(String targetUid) { this.TargetUid = targetUid; }
    }

    public static class DirectFriendMessageRequest {
        public String ReceiverId, Text, ImageUrl;

        public DirectFriendMessageRequest(String receiverId, String text, String imageUrl) {
            this.ReceiverId = receiverId; this.Text = text; this.ImageUrl = imageUrl != null ? imageUrl : "";
        }
    }

    public static class WorkspaceInviteSendRequest {
        public String WorkspaceId, WorkspaceName;
        public List<String> TargetUids;
        public WorkspaceInviteSendRequest(String wId, String wName, List<String> uids) {
            this.WorkspaceId = wId; this.WorkspaceName = wName; this.TargetUids = uids;
        }
    }

    public static class WorkspaceInviteHandleRequest {
        public String WorkspaceId, InvitationId;
        public WorkspaceInviteHandleRequest(String wId, String iId) {
            this.WorkspaceId = wId; this.InvitationId = iId;
        }
    }

    public static class FeedRequest {
        public int Limit;
        public long LastTimestamp;
        public String Scope;
        public FeedRequest(int limit, long lastTimestamp, String scope) {
            this.Limit = limit; this.LastTimestamp = lastTimestamp; this.Scope = scope;
        }
    }

    public static class CreatePostRequest {
        public String Title, Content, ImageUrl, Type, MilestoneData, Visibility, Audience, AmountText;
        public int Progress;

        public CreatePostRequest() {}

        public CreatePostRequest(String title, String content, String type, String imageUrl, String milestoneData, String audience) {
            this.Title = title;
            this.Content = content;
            this.Type = type;
            this.ImageUrl = imageUrl;
            this.MilestoneData = milestoneData;
            this.Audience = audience;
        }
    }

    public static class EditPostRequest {
        public String PostId, Title, NewContent, NewImageUrl, Visibility, Audience;
    }

    public static class DeletePostRequest {
        public String PostId;
        public DeletePostRequest(String postId) { this.PostId = postId; }
    }

    public static class AchievementSuggestion {
        public String id, title, description, iconText, amountLabel, monthLabel;
        public int progress;
    }

    public static class LikeActionRequest {
        @SerializedName("postId") public String PostId;
        @SerializedName("liked")  public boolean Liked;
        public LikeActionRequest(String postId, boolean liked) { this.PostId = postId; this.Liked = liked; }
    }

    public static class AddCommentRequest {
        @SerializedName("postId")  public String PostId;
        @SerializedName("content") public String Content;
        public AddCommentRequest(String postId, String content) { this.PostId = postId; this.Content = content; }
    }

    public static class EditCommentRequest {
        public String PostId, CommentId, NewContent;
    }

    public static class DeleteCommentRequest {
        public String PostId, CommentId;
        public DeleteCommentRequest(String postId, String commentId) { this.PostId = postId; this.CommentId = commentId; }
    }

    public static class BatchProfileRequest {
        public List<String> Uids;
        public BatchProfileRequest(List<String> uids) { this.Uids = uids; }
    }

    public static class SocialPostDetailResponse {
        @SerializedName("userId")          public String authorId;
        @SerializedName("authorName")      public String authorName;
        @SerializedName("authorAvatarUrl") public String authorAvatarUrl;
        @SerializedName("title")         public String title;
        @SerializedName("content")         public String content;
        @SerializedName("imageUrl")        public String imageUrl;
        @SerializedName("likeCount")       public int likeCount;
        @SerializedName("commentCount")    public int commentCount;
        @SerializedName("timestamp")       public long timestamp;
        @SerializedName("isLiked")         public boolean likedByMe;
        @SerializedName("type")            public String type;
        @SerializedName("milestoneData")   public String milestoneData;
    }

    public static class WorkspaceMessageSendRequest {
        public String WorkspaceId, Text, ImageUrl;
        public WorkspaceMessageSendRequest(String workspaceId, String text, String imageUrl) {
            this.WorkspaceId = workspaceId; this.Text = text; this.ImageUrl = imageUrl != null ? imageUrl : "";
        }
    }

    public static class AutoMilestoneRequest {
        public long LimitAmount, SpentAmount;
        public String PeriodType, PeriodLabel;
        public AutoMilestoneRequest(long limit, long spent, String type, String label) {
            this.LimitAmount = limit; this.SpentAmount = spent; this.PeriodType = type; this.PeriodLabel = label;
        }
    }
}