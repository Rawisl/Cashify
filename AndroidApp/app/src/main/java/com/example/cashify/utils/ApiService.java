package com.example.cashify.utils;

import com.example.cashify.data.model.User;
import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.data.model.DirectConversation;
import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.InvoiceParser.ParsedInvoice;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- SYSTEM ---
    @GET("/api/v1/cloudinary/sign")
    Call<ApiDto.CloudinarySignatureResponse> getCloudinarySignature(@Header("Authorization") String token);

    @POST("/api/v1/scan-bill")
    Call<ParsedInvoice> scanBill(@Header("Authorization") String token, @Body ApiDto.ScanRequest request);

    // --- WORKSPACE ---
    @POST("/api/v1/workspace/create")
    Call<ApiDto.WorkspaceCreateResponse> createWorkspace(@Header("Authorization") String token, @Body ApiDto.WorkspaceCreateRequest request);

    @POST("/api/v1/workspace/leave")
    Call<Object> leaveWorkspace(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/transfer-owner")
    Call<Object> transferOwnership(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/invite/send")
    Call<Object> sendWorkspaceInvites(@Header("Authorization") String token, @Body ApiDto.WorkspaceInviteSendRequest request);

    @POST("/api/v1/workspace/invite/accept")
    Call<Object> acceptWorkspaceInvite(@Header("Authorization") String token, @Body ApiDto.WorkspaceInviteHandleRequest request);

    @POST("/api/v1/workspace/invite/decline")
    Call<Object> declineWorkspaceInvite(@Header("Authorization") String token, @Body ApiDto.WorkspaceInviteHandleRequest request);

    @POST("/api/v1/workspace/transaction/add")
    Call<Object> addWorkspaceTransaction(@Header("Authorization") String token, @Body ApiDto.TransactionRequest request);

    @POST("/api/v1/workspace/transaction/delete")
    Call<Object> deleteWorkspaceTransaction(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/member/kick")
    Call<Object> kickMember(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/category/add")
    Call<Object> addCategory(@Header("Authorization") String token, @Body ApiDto.AddCategoryRequest request);

    @POST("/api/v1/workspace/category/delete")
    Call<Object> deleteCategory(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/category/edit")
    Call<Object> editCategory(@Header("Authorization") String token, @Body ApiDto.EditCategoryRequest request);

    @POST("/api/v1/workspace/category/restore")
    Call<Object> restoreCategory(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/message/recall")
    Call<Object> recallMessage(@Header("Authorization") String token, @Body ApiDto.WorkspaceActionRequest request);

    @POST("/api/v1/workspace/message/send")
    Call<Object> sendWorkspaceMessage(@Header("Authorization") String token, @Body ApiDto.WorkspaceMessageSendRequest request);

    // --- FRIENDS ---
    @POST("/api/v1/friend/{actionType}")
    Call<Object> processFriendAction(@Path("actionType") String actionType, @Header("Authorization") String token, @Body ApiDto.FriendActionRequest request);

    @GET("/api/v1/friend/suggestions")
    Call<List<User>> getFriendSuggestions(@Header("Authorization") String token);

    @GET("/api/v1/friend/messages/chats")
    Call<List<User>> getFriendMessageChats(@Header("Authorization") String token);

    @GET("/api/v1/friend/messages/conversations")
    Call<List<DirectConversation>> getDirectFriendConversations(@Header("Authorization") String token);

    @GET("/api/v1/friend/messages/{friendUid}")
    Call<List<ChatMessage>> getDirectFriendMessages(@Path("friendUid") String friendUid, @Header("Authorization") String token);

    @POST("/api/v1/friend/messages/send")
    Call<Object> sendDirectFriendMessage(@Header("Authorization") String token, @Body ApiDto.DirectFriendMessageRequest request);

    @PATCH("/api/v1/friend/messages/{friendUid}/{messageId}/recall")
    Call<Object> recallFriendMessage(@Header("Authorization") String token, @Path("friendUid") String friendUid, @Path("messageId") String messageId);

    // --- SOCIAL FEED & POSTS ---
    @POST("/api/v1/post/feed")
    Call<List<Object>> getFeed(@Header("Authorization") String token, @Body ApiDto.FeedRequest request);

    @GET("/api/v1/post/{postId}/comments")
    Call<List<Object>> getComments(@Path("postId") String postId, @Header("Authorization") String auth);

    @POST("/api/v1/post/create")
    Call<Object> createPost(@Header("Authorization") String token, @Body ApiDto.CreatePostRequest request);

    @POST("/api/v1/post/edit")
    Call<Object> editPost(@Header("Authorization") String token, @Body ApiDto.EditPostRequest request);

    @POST("/api/v1/post/delete")
    Call<Object> deletePost(@Header("Authorization") String token, @Body ApiDto.DeletePostRequest request);

    @GET("/api/v1/achievements/available")
    Call<List<ApiDto.AchievementSuggestion>> getAvailableAchievements(@Header("Authorization") String token);

    @POST("/api/v1/post/like")
    Call<Object> toggleLike(@Header("Authorization") String auth, @Body ApiDto.LikeActionRequest body);

    @POST("/api/v1/comment/add")
    Call<Object> addComment(@Header("Authorization") String auth, @Body ApiDto.AddCommentRequest body);

    @POST("/api/v1/comment/edit")
    Call<Object> editComment(@Header("Authorization") String token, @Body ApiDto.EditCommentRequest request);

    @POST("/api/v1/comment/delete")
    Call<Object> deleteComment(@Header("Authorization") String token, @Body ApiDto.DeleteCommentRequest request);

    @POST("/api/v1/user/batch-profiles")
    Call<Object> getBatchProfiles(@Body ApiDto.BatchProfileRequest request);

    @GET("/api/v1/post/{postId}")
    Call<ApiDto.SocialPostDetailResponse> getPostDetail(@Path("postId") String postId, @Header("Authorization") String token);

    @GET("/api/v1/post/wall/{targetUid}")
    Call<List<Object>> getWall(@Header("Authorization") String token, @Path("targetUid") String targetUid, @Query("limit") int limit, @Query("lastTimestamp") long lastTimestamp);

    @POST("/api/v1/post/milestone-auto")
    Call<Object> generateAutoMilestone(@Header("Authorization") String token, @Body ApiDto.AutoMilestoneRequest request);
    @POST("/api/v1/post/hide")
    Call<Object> hidePost(@Header("Authorization") String token, @Body ApiDto.HidePostRequest request);

    @POST("/api/v1/comment/hide")
    Call<Object> hideComment(@Header("Authorization") String token, @Body ApiDto.HideCommentRequest request);
}