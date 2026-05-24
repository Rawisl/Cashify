package com.example.cashify.utils;

import com.example.cashify.utils.InvoiceParser.ParsedInvoice;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

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

    @POST("/api/v1/friend/messages/send")
    Call<Object> sendDirectFriendMessage(@Header("Authorization") String token, @Body DirectFriendMessageRequest request);

    //SOCIAL API

    //thao tác CƠ BẢN trên newsfeed
    //load feed
    @POST("/api/v1/post/feed")
    Call<java.util.List<Object>> getFeed(@Header("Authorization") String token, @Body FeedRequest request);

    //load bài viết trên trang cá nhân riêng
    @GET("/api/v1/post/wall/{targetUid}")
    Call<java.util.List<Object>> getWall(@Path("targetUid") String targetUid, int limit, long lastTimestamp, @Header("Authorization") String token);

    //load comment của bài viết
    @GET("/api/v1/post/{postId}/comments")
    Call<java.util.List<Object>> getComments(@Path("postId") String postId, @Header("Authorization") String token);

    //tạo/sửa/xóa post
    @POST("/api/v1/post/create")
    Call<Object> createPost(@Header("Authorization") String token, @Body CreatePostRequest request);

    @POST("/api/v1/post/edit")
    Call<Object> editPost(@Header("Authorization") String token, @Body EditPostRequest request);

    @POST("/api/v1/post/delete")
    Call<Object> deletePost(@Header("Authorization") String token, @Body DeletePostRequest request);

    //các tương tác trên post
    @POST("/api/v1/post/like")
    Call<Object> toggleLike(@Header("Authorization") String token, @Body LikeActionRequest request);

    @POST("/api/v1/comment/add")
    Call<Object> addComment(@Header("Authorization") String token, @Body AddCommentRequest request);

    @POST("/api/v1/comment/edit")
    Call<Object> editComment(@Header("Authorization") String token, @Body EditCommentRequest request);

    @POST("/api/v1/comment/delete")
    Call<Object> deleteComment(@Header("Authorization") String token, @Body DeleteCommentRequest request);

    //api hỗ trợ load profile
    @POST("/api/v1/user/batch-profiles")
    Call<Object> getBatchProfiles(@Body BatchProfileRequest request);

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
    class EditCategoryRequest {
        public String WorkspaceId, CategoryId, Name, IconName, ColorCode;
        public int Type;
        public EditCategoryRequest(String wId, String cId, String name, String icon, String color, int type) {
            WorkspaceId = wId; CategoryId = cId; Name = name; IconName = icon; ColorCode = color; Type = type;
        }
    }

    class FeedRequest {
        public java.util.List<String> FriendIds;
        public long LastTimestamp;
        public int Limit;
        public FeedRequest(java.util.List<String> fIds, long lTs, int lim) { FriendIds = fIds; LastTimestamp = lTs; Limit = lim; }
    }

    class CreatePostRequest {
        public String Content, ImageUrl, Type;
        public Object MilestoneData;
        public CreatePostRequest(String c, String img, String type, Object m) { Content = c; ImageUrl = img; Type = type; MilestoneData = m; }
    }

    class EditPostRequest {
        public String PostId, NewContent, NewImageUrl;
        public EditPostRequest(String pid, String nc, String nimg) { PostId = pid; NewContent = nc; NewImageUrl = nimg; }
    }

    class DeletePostRequest {
        public String PostId;
        public DeletePostRequest(String id) { PostId = id; }
    }

    class LikeActionRequest {
        public String PostId;
        public LikeActionRequest(String id) { PostId = id; }
    }

    class AddCommentRequest {
        public String PostId, Content;
        public AddCommentRequest(String p, String c) { PostId = p; Content = c; }
    }

    class EditCommentRequest {
        public String PostId, CommentId, NewContent;
        public EditCommentRequest(String p, String c, String nc) { PostId = p; CommentId = c; NewContent = nc; }
    }

    class DeleteCommentRequest {
        public String PostId, CommentId;
        public DeleteCommentRequest(String p, String c) { PostId = p; CommentId = c; }
    }

    class BatchProfileRequest {
        public java.util.List<String> UserIds;
        public BatchProfileRequest(java.util.List<String> ids) { UserIds = ids; }
    }
}
