package com.example.cashify.data.remote;

import android.util.Log;

import com.example.cashify.data.model.DirectConversation;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiService;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tầng Remote Data Source (Singleton Pattern) xử lý mọi giao tiếp với Server
public class FirebaseManager {
    private static final String TAG = "CASHIFY";
    private static final String ERR_NOT_LOGGED_IN = "Not logged in!";

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) instance = new FirebaseManager();
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(String uid);
        void onError(String message);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    // ============================================================
    // AUTHENTICATION
    // ============================================================

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> callback.onSuccess(r.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void registerWithEmail(String email, String password, String name, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", email);
                    userMap.put("uid", uid);
                    userMap.put("displayName", name);
                    db.collection("users").document(uid).set(userMap);
                    callback.onSuccess(uid);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void loginWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", r.getUser().getEmail());
                    userMap.put("uid", uid);
                    db.collection("users").document(uid).set(userMap);
                    callback.onSuccess(uid);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "getCurrentUserId: Not logged in!");
            return null;
        }
        return user.getUid();
    }

    public void logout() {
        auth.signOut();
    }

    // ============================================================
    // PERSONAL DATA SYNC
    // ============================================================

    public void syncLocalToCloud(String workspaceId, String collection, String docId, Map<String, Object> data, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onError(ERR_NOT_LOGGED_IN);
            return;
        }

        com.google.firebase.firestore.DocumentReference docRef = (workspaceId == null || workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection(collection).document(docId)
                : db.collection("workspaces").document(workspaceId).collection(collection).document(docId);

        docRef.set(data)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    public void listenToChanges(String workspaceId, String collectionName, DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        com.google.firebase.firestore.CollectionReference colRef = (workspaceId == null || workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection(collectionName)
                : db.collection("workspaces").document(workspaceId).collection(collectionName);

        colRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            if (value != null) callback.onSuccess(value.getDocuments());
        });
    }

    public void deleteAllTransactionsFromCloud(String workspaceId, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        com.google.firebase.firestore.CollectionReference colRef = (workspaceId == null || workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection("transactions")
                : db.collection("workspaces").document(workspaceId).collection("transactions");

        colRef.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                callback.onSuccess(null);
                return;
            }
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snap.getDocuments()) batch.delete(doc.getReference());
            batch.commit()
                    .addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteDocumentFromCloud(String workspaceId, String collection, String docId, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onError(ERR_NOT_LOGGED_IN);
            return;
        }

        com.google.firebase.firestore.DocumentReference docRef = (workspaceId == null || workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection(collection).document(docId)
                : db.collection("workspaces").document(workspaceId).collection(collection).document(docId);

        docRef.delete()
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    // ============================================================
    // SOCIAL & WORKSPACE
    // ============================================================

    public void searchUserByEmail(String email, DataCallback<String> callback) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) callback.onSuccess(snap.getDocuments().get(0).getString("uid"));
                    else callback.onError("User not found");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinWorkspace(String workspaceId, String userId, DataCallback<Void> callback) {
        db.collection("workspaces").document(workspaceId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void listenToWorkspaceInvitations(DataCallback<List<com.example.cashify.data.model.WorkspaceInvitation>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        db.collection("users").document(uid).collection("workspace_invitations")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onError(e.getMessage());
                        return;
                    }
                    List<com.example.cashify.data.model.WorkspaceInvitation> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            com.example.cashify.data.model.WorkspaceInvitation invite = doc.toObject(com.example.cashify.data.model.WorkspaceInvitation.class);
                            if (invite != null) {
                                invite.setId(doc.getId());
                                list.add(invite);
                            }
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public void sendWorkspaceInvites(String workspaceId, String workspaceName, List<String> targetUids, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.WorkspaceInviteSendRequest req = new ApiDto.WorkspaceInviteSendRequest(workspaceId, workspaceName, targetUids);

            apiService.sendWorkspaceInvites(token, req).enqueue(new retrofit2.Callback<Object>() {
                @Override public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Server rejected: " + response.code());
                }
                @Override public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError(t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void acceptWorkspaceInvitation(com.example.cashify.data.model.WorkspaceInvitation invitation, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.WorkspaceInviteHandleRequest req = new ApiDto.WorkspaceInviteHandleRequest(invitation.getWorkspaceId(), invitation.getId());

            apiService.acceptWorkspaceInvite(token, req).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        try {
                            org.json.JSONObject jsonObject = new org.json.JSONObject(response.errorBody().string());
                            callback.onError(jsonObject.getString("message"));
                        } catch (Exception e) {
                            callback.onError("Server rejected: " + response.code());
                        }
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError(t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void declineWorkspaceInvitation(String invitationId, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.WorkspaceInviteHandleRequest req = new ApiDto.WorkspaceInviteHandleRequest(null, invitationId);

            apiService.declineWorkspaceInvite(token, req).enqueue(new retrofit2.Callback<Object>() {
                @Override public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Server rejected: " + response.code());
                }
                @Override public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError(t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // FCM
    // ============================================================

    public void getFcmToken(DataCallback<String> callback) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String uid = getCurrentUserId();
                    if (uid != null) db.collection("users").document(uid).update("fcmToken", token);
                    callback.onSuccess(token);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPaymentReminder(String targetUid, String title, String body, DataCallback<Void> callback) {
        Map<String, Object> req = new HashMap<>();
        req.put("toUid", targetUid);
        req.put("title", title);
        req.put("body", body);
        req.put("timestamp", FieldValue.serverTimestamp());
        db.collection("notification_requests").add(req)
                .addOnSuccessListener(ref -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // USER DATA
    // ============================================================

    public void getAllTransactionsFromCloud(String workspaceId, DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        com.google.firebase.firestore.CollectionReference colRef = (workspaceId == null || workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection("transactions")
                : db.collection("workspaces").document(workspaceId).collection("transactions");

        colRef.get()
                .addOnSuccessListener(q -> callback.onSuccess(q.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllBudgetsFromCloud(DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        db.collection("users").document(uid).collection("budgets").get()
                .addOnSuccessListener(q -> callback.onSuccess(q.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllCategoriesFromCloud(DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        db.collection("users").document(uid).collection("categories").get()
                .addOnSuccessListener(q -> callback.onSuccess(q.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllUsers(DataCallback<List<User>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) {
                        User user = doc.toObject(User.class);
                        if (user != null && !myUid.equals(user.getUid())) users.add(user);
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // FRIEND REQUEST SYSTEM
    // ============================================================

    public void getFriendIds(String uid, DataCallback<List<String>> callback) {
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getIncomingRequestIds(DataCallback<List<String>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        db.collection("users").document(myUid).collection("friend_requests").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSentRequestIds(DataCallback<List<String>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        db.collection("users").document(myUid).collection("sent_requests").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendFriendRequest(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        WriteBatch batch = db.batch();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("fromUid", myUid);
        requestData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(db.collection("users").document(targetUid).collection("friend_requests").document(myUid), requestData);

        Map<String, Object> sentData = new HashMap<>();
        sentData.put("toUid", targetUid);
        sentData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(db.collection("users").document(myUid).collection("sent_requests").document(targetUid), sentData);

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void cancelFriendRequest(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(targetUid).collection("friend_requests").document(myUid));
        batch.delete(db.collection("users").document(myUid).collection("sent_requests").document(targetUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void acceptFriendRequest(String requesterUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(myUid).collection("friends").document(requesterUid), new HashMap<>());
        batch.set(db.collection("users").document(requesterUid).collection("friends").document(myUid), new HashMap<>());
        batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(requesterUid));
        batch.delete(db.collection("users").document(requesterUid).collection("sent_requests").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void declineFriendRequest(String requesterUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(requesterUid));
        batch.delete(db.collection("users").document(requesterUid).collection("sent_requests").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void unfriend(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(myUid).collection("friends").document(targetUid));
        batch.delete(db.collection("users").document(targetUid).collection("friends").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // WORKSPACE & API CALLS
    // ============================================================

    public void sendWorkspaceMessage(String workspaceId, String text, String imageUrl, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            ApiDto.WorkspaceMessageSendRequest request = new ApiDto.WorkspaceMessageSendRequest(workspaceId, text, imageUrl);

            apiService.sendWorkspaceMessage(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void createSharedWorkspace(String workspaceName, String type, String iconName, List<String> memberIds, DataCallback<String> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.WorkspaceCreateRequest request = new ApiDto.WorkspaceCreateRequest(workspaceName, type, iconName);

            apiService.createWorkspace(token, request).enqueue(new retrofit2.Callback<ApiDto.WorkspaceCreateResponse>() {
                @Override
                public void onResponse(retrofit2.Call<ApiDto.WorkspaceCreateResponse> call, retrofit2.Response<ApiDto.WorkspaceCreateResponse> response) {
                    if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().workspaceId);
                    else callback.onError("Server rejected: " + response.code());
                }
                @Override
                public void onFailure(retrofit2.Call<ApiDto.WorkspaceCreateResponse> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void leaveWorkspace(String workspaceId, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        callGenericWorkspaceApi(req, "leave", callback);
    }

    public void transferOwnership(String workspaceId, String newOwnerId, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        req.NewOwnerId = newOwnerId;
        callGenericWorkspaceApi(req, "transfer", callback);
    }

    public void kickMember(String workspaceId, String targetUid, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        req.TargetUid = targetUid;
        callGenericWorkspaceApi(req, "kick", callback);
    }

    public void deleteWorkspaceTransaction(String workspaceId, String transactionId, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        req.TransactionId = transactionId;
        callGenericWorkspaceApi(req, "deleteTransaction", callback);
    }

    public void addCategory(String workspaceId, String name, String iconName, String colorCode, int type, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(ERR_NOT_LOGGED_IN);
            return;
        }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            ApiDto.AddCategoryRequest request = new ApiDto.AddCategoryRequest();
            request.WorkspaceId = workspaceId;
            request.Name = name;
            request.IconName = iconName;
            request.ColorCode = colorCode;
            request.Type = type;

            apiService.addCategory(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError(extractApiError(response, "Server rejected (Code: " + response.code() + ")"));
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }
    public void deleteCategory(String workspaceId, String categoryId, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        req.CategoryId = categoryId;
        callGenericWorkspaceApi(req, "deleteCategory", callback);
    }

    public void editCategory(String workspaceId, String categoryId, String name, String iconName, String colorCode, int type, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.EditCategoryRequest request = new ApiDto.EditCategoryRequest(workspaceId, categoryId, name, iconName, colorCode, type);

            apiService.editCategory(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError(extractApiError(response, "Server rejected (Code: " + response.code() + ")"));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void restoreCategory(String workspaceId, String categoryId, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
            req.WorkspaceId = workspaceId;
            req.CategoryId = categoryId;

            apiService.restoreCategory(token, req).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError(extractApiError(response, "Server rejected (Code: " + response.code() + ")"));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void recallMessage(String workspaceId, String messageId, DataCallback<Void> callback) {
        ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
        req.WorkspaceId = workspaceId;
        req.MessageId = messageId;
        callGenericWorkspaceApi(req, "recallMessage", callback);
    }

    // Proxy cho các API chung của Workspace
    private void callGenericWorkspaceApi(ApiDto.WorkspaceActionRequest requestData, String action, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            retrofit2.Call<Object> call;

            switch (action) {
                case "leave": call = apiService.leaveWorkspace(token, requestData); break;
                case "transfer": call = apiService.transferOwnership(token, requestData); break;
                case "kick": call = apiService.kickMember(token, requestData); break;
                case "deleteTransaction": call = apiService.deleteWorkspaceTransaction(token, requestData); break;
                case "deleteCategory": call = apiService.deleteCategory(token, requestData); break;
                case "recallMessage": call = apiService.recallMessage(token, requestData); break;
                default: callback.onError("Invalid action"); return;
            }

            call.enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Denied (Code: " + response.code() + ")");
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    // ============================================================
    // SOCIAL APIs
    // ============================================================
    public void processFriendAction(String targetUid, String actionType, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.FriendActionRequest request = new ApiDto.FriendActionRequest(targetUid);

            apiService.processFriendAction(actionType, token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) { callback.onSuccess(null); return; }
                    callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void sendDirectFriendMessage(String receiverId, String text, String imageUrl, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Sửa thành ApiDto
            ApiDto.DirectFriendMessageRequest request = new ApiDto.DirectFriendMessageRequest(receiverId, text, imageUrl);

            apiService.sendDirectFriendMessage(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void recallDirectFriendMessage(String friendUid, String messageId, DataCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            apiService.recallFriendMessage(token, friendUid, messageId).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void getFriendSuggestions(DataCallback<List<User>> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getFriendSuggestions(token).enqueue(new retrofit2.Callback<List<User>>() {
                @Override
                public void onResponse(retrofit2.Call<List<User>> call, retrofit2.Response<List<User>> response) {
                    if (response.isSuccessful()) callback.onSuccess(response.body() != null ? response.body() : new ArrayList<>());
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<List<User>> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void getFriendMessageChats(DataCallback<List<User>> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getFriendMessageChats(token).enqueue(new retrofit2.Callback<List<User>>() {
                @Override
                public void onResponse(retrofit2.Call<List<User>> call, retrofit2.Response<List<User>> response) {
                    if (response.isSuccessful()) callback.onSuccess(response.body() != null ? response.body() : new ArrayList<>());
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<List<User>> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void getDirectFriendConversations(DataCallback<List<DirectConversation>> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { callback.onError(ERR_NOT_LOGGED_IN); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getDirectFriendConversations(token).enqueue(new retrofit2.Callback<List<DirectConversation>>() {
                @Override
                public void onResponse(retrofit2.Call<List<DirectConversation>> call, retrofit2.Response<List<DirectConversation>> response) {
                    if (response.isSuccessful()) callback.onSuccess(response.body() != null ? response.body() : new ArrayList<>());
                    else callback.onError(extractApiError(response, "Server error: " + response.code()));
                }
                @Override
                public void onFailure(retrofit2.Call<List<DirectConversation>> call, Throwable t) { callback.onError("Network error: " + t.getMessage()); }
            });
        }).addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    // Thuật toán tạo ID phòng chat 1-1 không bị trùng lặp
    private String generateChatId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? (uid1 + "_" + uid2) : (uid2 + "_" + uid1);
    }

    // Lắng nghe tin nhắn theo thời gian thực (Real-time listener)
    public com.google.firebase.firestore.ListenerRegistration listenToDirectMessages(String friendUid, DataCallback<List<com.example.cashify.data.model.ChatMessage>> callback) {
        String currentUid = getCurrentUserId();
        if (currentUid == null) { callback.onError(ERR_NOT_LOGGED_IN); return null; }

        String chatId = generateChatId(currentUid, friendUid);

        return db.collection("direct_chats").document(chatId).collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e.getMessage()); return; }
                    List<com.example.cashify.data.model.ChatMessage> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                            com.example.cashify.data.model.ChatMessage msg = doc.toObject(com.example.cashify.data.model.ChatMessage.class);
                            msg.setMessageId(doc.getId());
                            list.add(msg);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    // Helper method bóc tách mã lỗi từ API trả về
    private String extractApiError(retrofit2.Response<?> response, String fallback) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
            if (!errorBody.isEmpty()) {
                org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                String message = jsonObject.optString("message");
                if (message.isEmpty()) message = jsonObject.optString("detail");
                if (message.isEmpty()) message = jsonObject.optString("title");
                if (!message.isEmpty()) return message;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    public ListenerRegistration listenToDirectConversations(DataCallback<List<DirectConversation>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onError(ERR_NOT_LOGGED_IN); return null; }

        return db.collection("users").document(uid).collection("friends")
                .addSnapshotListener((friendSnap, e) -> {
                    if (e != null) { callback.onError(e.getMessage()); return; }
                    if (friendSnap == null || friendSnap.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    getDirectFriendConversations(callback);
                });
    }

    // ============================================================
    // UNIFIED NOTIFICATIONS
    // ============================================================
    public void listenToUnreadNotifications(DataCallback<Integer> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        db.collection("users").document(uid).collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { callback.onError(e.getMessage()); return; }
                    callback.onSuccess(snap != null ? snap.size() : 0);
                });
    }

    // ============================================================
    // GAMIFICATION STATS (NIGHT OWL, BIG SPENDER)
    // ============================================================
    public void syncTransactionWithStats(boolean isInsert, boolean isDelete, com.example.cashify.data.model.Transaction t, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            if (callback != null) callback.onError(ERR_NOT_LOGGED_IN);
            return;
        }

        WriteBatch batch = db.batch();
        String workspaceId = (t.workspaceId != null) ? t.workspaceId : "PERSONAL";

        com.google.firebase.firestore.DocumentReference transRef = (workspaceId.equals("PERSONAL"))
                ? db.collection("users").document(uid).collection("transactions").document(String.valueOf(t.id))
                : db.collection("workspaces").document(workspaceId).collection("transactions").document(String.valueOf(t.id));

        if (isDelete) {
            batch.delete(transRef);
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("amount", t.amount);
            data.put("categoryId", t.categoryId);
            data.put("note", t.note);
            data.put("timestamp", t.timestamp);
            data.put("paymentMethod", t.paymentMethod);
            data.put("type", t.type);
            data.put("workspaceId", workspaceId);
            data.put("userId", uid);
            batch.set(transRef, data);
        }

        if (isInsert || isDelete) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy_MM", java.util.Locale.ENGLISH);
            String monthKey = sdf.format(new java.util.Date(t.timestamp));

            Map<String, Object> statsUpdate = new HashMap<>();
            int countChange = isDelete ? -1 : 1;
            long amountChange = isDelete ? -t.amount : t.amount;

            statsUpdate.put("totalTransactions", FieldValue.increment(countChange));
            if (t.type == 1) statsUpdate.put("income_" + monthKey, FieldValue.increment(amountChange));
            else if (t.type == 0) statsUpdate.put("spend_" + monthKey, FieldValue.increment(amountChange));

            // Set achievements logic when inserting
            if (isInsert) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(t.timestamp);
                int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                if (hour >= 0 && hour < 4) statsUpdate.put("nightOwlUnlocked", true);
                if (t.type == 0 && t.amount >= 10000000) statsUpdate.put("bigSpenderUnlocked", true);
            }

            com.google.firebase.firestore.DocumentReference statsRef = (workspaceId.equals("PERSONAL"))
                    ? db.collection("users").document(uid).collection("user_stats").document("summary")
                    : db.collection("workspaces").document(workspaceId).collection("workspace_stats").document("summary");

            batch.set(statsRef, statsUpdate, com.google.firebase.firestore.SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }
}