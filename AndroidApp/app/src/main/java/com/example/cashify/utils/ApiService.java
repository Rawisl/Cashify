package com.example.cashify.utils;

import com.example.cashify.utils.InvoiceParser.ParsedInvoice;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("/api/v1/cloudinary/sign")
    Call<CloudinarySignatureResponse> getCloudinarySignature(@Header("Authorization") String token);

    @POST("/api/v1/scan-bill")
    Call<ParsedInvoice> scanBill(@Header("Authorization") String token, @Body ScanRequest request);

    //WORKSPACE API
    //Thao tác CƠ BẢN của người dùng
    @POST("/api/v1/workspace/create")
    Call<WorkspaceCreateResponse> createWorkspace(@Header("Authorization") String token, @Body WorkspaceCreateRequest request);

    @POST("/api/v1/workspace/leave")
    Call<Object> leaveWorkspace(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/transfer-owner")
    Call<Object> transferOwnership(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    //thao tác với LỜI MỜI
    @POST("/api/v1/workspace/invite/send")
    Call<Object> sendWorkspaceInvites(@Header("Authorization") String token, @Body WorkspaceInviteSendRequest request);

    @POST("/api/v1/workspace/invite/accept")
    Call<Object> acceptWorkspaceInvite(@Header("Authorization") String token, @Body WorkspaceInviteHandleRequest request);

    @POST("/api/v1/workspace/invite/decline")
    Call<Object> declineWorkspaceInvite(@Header("Authorization") String token, @Body WorkspaceInviteHandleRequest request);

    //Thao tác trên GIAO DỊCH QUỸ
    @POST("/api/v1/workspace/transaction/add")
    Call<Object> addWorkspaceTransaction(@Header("Authorization") String token, @Body TransactionRequest request);

    @POST("/api/v1/workspace/transaction/delete")
    Call<Object> deleteWorkspaceTransaction(@Header("Authorization") String token, @Body WorkspaceActionRequest request);
    @POST("/api/v1/workspace/member/kick")
    Call<Object> kickMember(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    //Thao tác trên DANH MỤC QUỸ
    @POST("/api/v1/workspace/category/delete")
    Call<Object> deleteCategory(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/category/edit")
    Call<Object> editCategory(@Header("Authorization") String token, @Body EditCategoryRequest request);

    @POST("/api/v1/workspace/category/restore")
    Call<Object> restoreCategory(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    //thao tác trên TIN NHẮN
    @POST("/api/v1/workspace/message/recall")
    Call<Object> recallMessage(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    //FRIENDS API

    //thao tác CƠ BẢN trên FRIENDS (gửi, từ chối, đồng ý lời mời,...)
    // Dùng @Path để linh hoạt gọi /api/v1/friend/request, /accept, /remove
    @POST("/api/v1/friend/{actionType}")
    Call<Object> processFriendAction(@Path("actionType") String actionType, @Header("Authorization") String token, @Body FriendActionRequest request);

    @GET("/api/v1/friend/suggestions")
    Call<java.util.List<com.example.cashify.data.model.User>> getFriendSuggestions(@Header("Authorization") String token);

    //thao tác TIN NHẮN FRIENDS
    @GET("/api/v1/friend/messages/chats")
    Call<java.util.List<com.example.cashify.data.model.User>> getFriendMessageChats(@Header("Authorization") String token);

    @GET("/api/v1/friend/messages/conversations")
    Call<java.util.List<com.example.cashify.data.model.DirectConversation>> getDirectFriendConversations(@Header("Authorization") String token);

    @GET("/api/v1/friend/messages/{friendUid}")
    Call<java.util.List<com.example.cashify.data.model.ChatMessage>> getDirectFriendMessages(@Path("friendUid") String friendUid, @Header("Authorization") String token);

    @POST("/api/v1/friend/message/send")
    Call<Object> sendDirectFriendMessage(@Header("Authorization") String token, @Body DirectFriendMessageRequest request);

    //SOCIAL API

    //thao tác CƠ BẢN trên newsfeed
    //load feed
    @POST("/api/v1/post/feed")
    Call<java.util.List<Object>> getFeed(@Header("Authorization") String token, @Body FeedRequest request);

    //load comment của bài viết
    @GET("/api/v1/post/{postId}/comments")
    Call<List<Object>> getComments(
            @Path("postId") String postId,
            @Header("Authorization") String auth
    );

    //tạo/sửa/xóa post
    @POST("/api/v1/post/create")
    Call<Object> createPost(@Header("Authorization") String token, @Body CreatePostRequest request);

    @POST("/api/v1/post/edit")
    Call<Object> editPost(@Header("Authorization") String token, @Body EditPostRequest request);

    @POST("/api/v1/post/delete")
    Call<Object> deletePost(@Header("Authorization") String token, @Body DeletePostRequest request);

    //các tương tác trên post
    @POST("/api/v1/post/like")
    Call<Object> toggleLike(
            @Header("Authorization") String auth,
            @Body LikeActionRequest body
    );

    @POST("/api/v1/comment/add")
    Call<Object> addComment(
            @Header("Authorization") String auth,
            @Body AddCommentRequest body
    );

    @POST("/api/v1/comment/edit")
    Call<Object> editComment(@Header("Authorization") String token, @Body EditCommentRequest request);

    @POST("/api/v1/comment/delete")
    Call<Object> deleteComment(@Header("Authorization") String token, @Body DeleteCommentRequest request);

    //api hỗ trợ load profile
    @POST("/api/v1/user/batch-profiles")
    Call<Object> getBatchProfiles(@Body BatchProfileRequest request);

    @GET("/api/v1/post/{postId}")
    Call<SocialPostDetailResponse> getPostDetail(
            @Path("postId") String postId,
            @Header("Authorization") String token
    );

    @GET("/api/v1/post/{postId}/comments")
    Call<java.util.List<SocialCommentResponse>> getPostComments(@Path("postId") String postId, @Header("Authorization") String token);

    @POST("/api/v1/social/posts/{postId}/like")
    Call<SocialReactionResponse> setPostLike(@Path("postId") String postId, @Header("Authorization") String token, @Body SocialLikeRequest request);

    @POST("/api/v1/social/posts/{postId}/comments")
    Call<SocialCommentResponse> addPostComment(@Path("postId") String postId, @Header("Authorization") String token, @Body SocialCommentRequest request);

    @POST("/api/v1/social/posts/{postId}/share")
    Call<SocialReactionResponse> sharePost(@Path("postId") String postId, @Header("Authorization") String token);

    //load bài viết trên trang cá nhân riêng

    @GET("/api/v1/post/wall/{targetUid}")
    Call<List<Object>> getWall(
            @Header("Authorization") String token,
            @Path("targetUid") String targetUid,
            @Query("limit") int limit,
            @Query("lastTimestamp") long lastTimestamp
    );

    // --- CÁC CLASS MODEL DÙNG ĐỂ HỨNG DATA ---

    class CloudinarySignatureResponse {
        public String signature;
        public long timestamp;
        public String apiKey;
        public String cloudName;
        public String folder;
    }

    class ScanRequest {
        public String OcrText;
        public ScanRequest(String ocrText) { this.OcrText = ocrText; }
    }

    class TransactionRequest {
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

    class WorkspaceCreateRequest {
        public String Name, Type, IconName;
        public WorkspaceCreateRequest(String name, String type, String icon) { Name = name; Type = type; IconName = icon; }
    }

    class WorkspaceCreateResponse {
        public String workspaceId;
        public String message;
    }

    // Gộp chung các Request có cấu trúc na ná nhau cho gọn
    class WorkspaceActionRequest {
        public String WorkspaceId, NewOwnerId, TargetUid, TransactionId, CategoryId, MessageId;
    }

    class EditCategoryRequest {
        public String WorkspaceId;
        public String CategoryId;
        public String Name;
        public String IconName;
        public String ColorCode;
        public int Type;

        public EditCategoryRequest(String workspaceId, String categoryId, String name, String iconName, String colorCode, int type) {
            WorkspaceId = workspaceId;
            CategoryId = categoryId;
            Name = name;
            IconName = iconName;
            ColorCode = colorCode;
            Type = type;
        }
    }

    class FriendActionRequest {
        public String TargetUid;
        public FriendActionRequest(String targetUid) { TargetUid = targetUid; }
    }

    class DirectFriendMessageRequest {
        public String ReceiverId;
        public String Text;
        public DirectFriendMessageRequest(String receiverId, String text) {
            ReceiverId = receiverId;
            Text = text;
        }
    }

    class WorkspaceInviteSendRequest {
        public String WorkspaceId;
        public String WorkspaceName;
        public java.util.List<String> TargetUids;
        public WorkspaceInviteSendRequest(String wId, String wName, java.util.List<String> uids) {
            this.WorkspaceId = wId; this.WorkspaceName = wName; this.TargetUids = uids;
        }
    }

    class WorkspaceInviteHandleRequest {
        public String WorkspaceId;
        public String InvitationId;
        public WorkspaceInviteHandleRequest(String wId, String iId) {
            this.WorkspaceId = wId; this.InvitationId = iId;
        }
    }

    class FeedRequest {
        public int Limit;
        public long LastTimestamp;
        public String Scope;

        public FeedRequest() {}

        public FeedRequest(int limit, long lastTimestamp, String scope) {
            Limit = limit;
            LastTimestamp = lastTimestamp;
            Scope = scope;
        }
    }

    class CreatePostRequest {
        public String Content;
        public String ImageUrl;
        public String Type;
        public String MilestoneData; // Tui thêm lại biến này để đồng bộ với Fragment
        public String Visibility;
        public String Audience;
        public String Title;
        public String AmountText;
        public int Progress;

        // Constructor rỗng (Phòng hờ thư viện JSON cần dùng)
        public CreatePostRequest() {}

        // Constructor 4 tham số để dùng bên Fragment
        public CreatePostRequest(String content, String type, String imageUrl, String milestoneData) {
            this.Content = content;
            this.Type = type;
            this.ImageUrl = imageUrl;
            this.MilestoneData = milestoneData;
        }
    }

    class EditPostRequest {
        public String PostId;
        public String Content;
        public String ImageUrl;
        public String Visibility;
        public String Audience;

        public EditPostRequest() {}
    }

    class DeletePostRequest {
        public String PostId;

        public DeletePostRequest() {}

        public DeletePostRequest(String postId) {
            PostId = postId;
        }
    }

    class LikeActionRequest {
        @SerializedName("postId")
        public String PostId;

        @SerializedName("liked")
        public boolean Liked;

        public LikeActionRequest() {}

        public LikeActionRequest(String postId, boolean liked) {
            PostId = postId;
            Liked = liked;
        }
    }

    class AddCommentRequest {
        @SerializedName("postId")

        public String PostId;
        @SerializedName("content")

        public String Content;

        public AddCommentRequest() {}

        public AddCommentRequest(String postId, String content) {
            this.PostId = postId;
            this.Content = content;
        }
    }

    class EditCommentRequest {
        public String PostId;
        public String CommentId;
        public String Content;

        public EditCommentRequest() {}
    }

    class DeleteCommentRequest {
        public String PostId;
        public String CommentId;

        public DeleteCommentRequest() {}
    }

    class BatchProfileRequest {
        public java.util.List<String> Uids;

        public BatchProfileRequest() {}

        public BatchProfileRequest(java.util.List<String> uids) {
            Uids = uids;
        }
    }

    public static class SocialPostDetailResponse {
        @SerializedName("userId")
        public String authorId;

        @SerializedName("authorName")
        public String authorName;

        @SerializedName("authorAvatarUrl")
        public String authorAvatarUrl;

        @SerializedName("content")
        public String content;

        @SerializedName("imageUrl")
        public String imageUrl;

        @SerializedName("likeCount")
        public int likeCount;

        @SerializedName("commentCount")
        public int commentCount;

        @SerializedName("shareCount")
        public int shareCount;

        @SerializedName("timestamp")
        public long timestamp;

        @SerializedName("isLiked")
        public boolean likedByMe;
    }

    class SocialCommentResponse {
        public String id;
        public String authorId;
        public String authorName;
        public String authorAvatarUrl;
        public String content;
        public long timestamp;
        public int likeCount;
    }

    class SocialLikeRequest {
        public boolean Liked;
        public SocialLikeRequest(boolean liked) { Liked = liked; }
    }

    class SocialCommentRequest {
        public String Content;
        public SocialCommentRequest(String content) { Content = content; }
    }

    class SocialReactionResponse {
        public int likeCount;
        public int commentCount;
        public int shareCount;
        public boolean likedByMe;
    }
}
