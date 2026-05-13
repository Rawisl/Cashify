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

    @POST("/api/v1/workspace/transaction/add")
    Call<Object> addWorkspaceTransaction(@Header("Authorization") String token, @Body TransactionRequest request);

    @POST("/api/v1/workspace/create")
    Call<WorkspaceCreateResponse> createWorkspace(@Header("Authorization") String token, @Body WorkspaceCreateRequest request);

    @POST("/api/v1/workspace/leave")
    Call<Object> leaveWorkspace(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/transfer-owner")
    Call<Object> transferOwnership(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/member/kick")
    Call<Object> kickMember(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/transaction/delete")
    Call<Object> deleteWorkspaceTransaction(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/category/delete")
    Call<Object> deleteCategory(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    @POST("/api/v1/workspace/message/recall")
    Call<Object> recallMessage(@Header("Authorization") String token, @Body WorkspaceActionRequest request);

    // Dùng @Path để linh hoạt gọi /api/v1/friend/request, /accept, /remove
    @POST("/api/v1/friend/{actionType}")
    Call<Object> processFriendAction(@Path("actionType") String actionType, @Header("Authorization") String token, @Body FriendActionRequest request);

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
}