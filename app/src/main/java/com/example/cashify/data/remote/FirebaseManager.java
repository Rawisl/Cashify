package com.example.cashify.data.remote;

import android.util.Log;

import com.example.cashify.data.model.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "CASHIFY";

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

    public FirebaseAuth getAuth() { return auth; }

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
        if (user == null) { Log.e(TAG, "getCurrentUserId: chưa đăng nhập!"); return null; }
        return user.getUid();
    }

    public void logout() { auth.signOut(); }

    // ============================================================
    // PERSONAL DATA SYNC
    // ============================================================

    public void syncLocalToCloud(String workspaceId, String collection, String docId, Map<String, Object> data, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) { if (callback != null) callback.onError("Chưa đăng nhập!"); return; }

        com.google.firebase.firestore.DocumentReference docRef;
        if (workspaceId == null || workspaceId.equals("PERSONAL")) {
            docRef = db.collection("users").document(uid).collection(collection).document(docId);
        } else {
            docRef = db.collection("workspaces").document(workspaceId).collection(collection).document(docId);
        }

        docRef.set(data)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getMessage()); });
    }

    public void listenToChanges(String workspaceId, String collectionName, DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        com.google.firebase.firestore.CollectionReference colRef;
        if (workspaceId == null || workspaceId.equals("PERSONAL")) {
            colRef = db.collection("users").document(uid).collection(collectionName);
        } else {
            colRef = db.collection("workspaces").document(workspaceId).collection(collectionName);
        }
        colRef.addSnapshotListener((value, error) -> {
            if (error != null) { callback.onError(error.getMessage()); return; }
            if (value != null) callback.onSuccess(value.getDocuments());
        });
    }

    public void deleteAllTransactionsFromCloud(String workspaceId, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        com.google.firebase.firestore.CollectionReference colRef;
        if (workspaceId == null || workspaceId.equals("PERSONAL")) {
            colRef = db.collection("users").document(uid).collection("transactions");
        } else {
            colRef = db.collection("workspaces").document(workspaceId).collection("transactions");
        }
        colRef.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) { callback.onSuccess(null); return; }
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snap.getDocuments()) batch.delete(doc.getReference());
            batch.commit()
                    .addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteDocumentFromCloud(String workspaceId, String collection, String docId, DataCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) { if (callback != null) callback.onError("Chưa đăng nhập!"); return; }

        com.google.firebase.firestore.DocumentReference docRef;
        if (workspaceId == null || workspaceId.equals("PERSONAL")) {
            docRef = db.collection("users").document(uid).collection(collection).document(docId);
        } else {
            docRef = db.collection("workspaces").document(workspaceId).collection(collection).document(docId);
        }

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
                    else callback.onError("Không tìm thấy người dùng");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createSharedWorkspace(String workspaceName, String type, String iconName, List<String> memberIds, DataCallback<String> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        memberIds.add(uid);

        Map<String, Object> workspace = new HashMap<>();
        workspace.put("name", workspaceName);
        workspace.put("ownerId", uid);
        workspace.put("members", memberIds);

        //Thêm 2 dòng này để lưu icon và loại quỹ
        workspace.put("type", type);
        workspace.put("iconName", iconName != null ? iconName : "ic_other");

        db.collection("workspaces").add(workspace)
                .addOnSuccessListener(ref -> {
                    String newWorkspaceId = ref.getId();

                    // =========================================================
                    // TẠO XONG QUỸ LÀ BƠM DATA MẪU NGAY BẰNG WRITEBATCH
                    // =========================================================
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    com.google.firebase.firestore.CollectionReference catRef =
                            db.collection("workspaces").document(newWorkspaceId).collection("categories");

                    String[] defaultNames = {"Food & Dining", "Transport", "Shopping", "Salary", "Bonus"};
                    String[] defaultIcons = {"ic_food", "ic_transport", "ic_shopping", "ic_salary", "ic_bonus"};
                    String[] defaultColors = {"#FFB74D", "#4FC3F7", "#F06292", "#81C784", "#FFF176"};
                    int[] defaultTypes = {0, 0, 0, 1, 1}; // 0 = Chi, 1 = Thu

                    for (int i = 0; i < defaultNames.length; i++) {
                        com.google.firebase.firestore.DocumentReference newCatDoc = catRef.document();
                        Map<String, Object> c = new HashMap<>();
                        c.put("name", defaultNames[i]);
                        c.put("iconName", defaultIcons[i]);
                        c.put("colorCode", defaultColors[i]);
                        c.put("type", defaultTypes[i]);
                        c.put("workspaceId", newWorkspaceId);
                        c.put("isDefault", 1);
                        c.put("isDeleted", 0);
                        batch.set(newCatDoc, c);
                    }

                    com.google.firebase.firestore.DocumentReference logRef =
                            db.collection("workspaces").document(newWorkspaceId).collection("logs").document();

                    Map<String, Object> firstLog = new HashMap<>();
                    firstLog.put("actionType", "CREATE_WORKSPACE"); // Sử dụng LogActionType.CREATE_WORKSPACE
                    firstLog.put("message", "đã khởi tạo quỹ nhóm này");
                    firstLog.put("userId", uid);
                    firstLog.put("timestamp", System.currentTimeMillis()); // Timestamp cho TimeFormatter

                    batch.set(logRef, firstLog);

                    // 4. Commit toàn bộ danh mục và log cùng lúc
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Log.d("FirebaseManager", "Tạo quỹ, danh mục và log thành công!");
                                callback.onSuccess(newWorkspaceId);
                            })
                            .addOnFailureListener(e -> callback.onError("Lỗi khởi tạo dữ liệu: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinWorkspace(String workspaceId, String userId, DataCallback<Void> callback) {
        db.collection("workspaces").document(workspaceId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
        if (uid == null) { callback.onError("Chưa đăng nhập!"); return; }
        com.google.firebase.firestore.CollectionReference colRef;
        if (workspaceId == null || workspaceId.equals("PERSONAL")) {
            colRef = db.collection("users").document(uid).collection("transactions");
        } else {
            colRef = db.collection("workspaces").document(workspaceId).collection("transactions");
        }
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
        Log.e(TAG, "getAllUsers: myUid = " + myUid);
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) {
                        User user = doc.toObject(User.class);
                        if (user != null && !myUid.equals(user.getUid())) users.add(user);
                    }
                    Log.e(TAG, "getAllUsers: " + users.size() + " users");
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllUsers thất bại: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ============================================================
    // FRIEND REQUEST SYSTEM
    // ============================================================

    /**
     * Lấy danh sách UID bạn bè đã kết bạn thành công
     */
    public void getFriendIds(String uid, DataCallback<List<String>> callback) {
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    Log.e(TAG, "getFriendIds: " + ids.size() + " bạn");
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Lấy danh sách UID đang gửi lời mời kết bạn cho mình
     */
    public void getIncomingRequestIds(DataCallback<List<String>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        db.collection("users").document(myUid).collection("friend_requests").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    Log.e(TAG, "getIncomingRequestIds: " + ids.size() + " lời mời đến");
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Lấy danh sách UID mình đã gửi lời mời đi (chưa được accept)
     */
    public void getSentRequestIds(DataCallback<List<String>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        db.collection("users").document(myUid).collection("sent_requests").get()
                .addOnSuccessListener(snap -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap) ids.add(doc.getId());
                    Log.e(TAG, "getSentRequestIds: " + ids.size() + " lời mời đã gửi");
                    callback.onSuccess(ids);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Gửi lời mời kết bạn tới targetUid
     */
    public void sendFriendRequest(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        WriteBatch batch = db.batch();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("fromUid", myUid);
        requestData.put("timestamp", FieldValue.serverTimestamp());

        // Ghi vào inbox của người kia
        batch.set(
                db.collection("users").document(targetUid).collection("friend_requests").document(myUid),
                requestData
        );

        // Track phía mình đã gửi
        Map<String, Object> sentData = new HashMap<>();
        sentData.put("toUid", targetUid);
        sentData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(
                db.collection("users").document(myUid).collection("sent_requests").document(targetUid),
                sentData
        );

        batch.commit()
                .addOnSuccessListener(v -> {
                    Log.e(TAG, "sendFriendRequest tới " + targetUid + " thành công");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sendFriendRequest thất bại: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Huỷ lời mời đã gửi
     */
    public void cancelFriendRequest(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(targetUid).collection("friend_requests").document(myUid));
        batch.delete(db.collection("users").document(myUid).collection("sent_requests").document(targetUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    public void acceptFriendRequest(String requesterUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        WriteBatch batch = db.batch();

        // Thêm bạn 2 chiều
        batch.set(db.collection("users").document(myUid).collection("friends").document(requesterUid), new HashMap<>());
        batch.set(db.collection("users").document(requesterUid).collection("friends").document(myUid), new HashMap<>());

        // Xoá request
        batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(requesterUid));

        // Xoá sent_request của người kia
        batch.delete(db.collection("users").document(requesterUid).collection("sent_requests").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> {
                    Log.e(TAG, "acceptFriendRequest " + requesterUid + " thành công");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "acceptFriendRequest thất bại: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public void declineFriendRequest(String requesterUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(requesterUid));
        batch.delete(db.collection("users").document(requesterUid).collection("sent_requests").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Huỷ kết bạn*/
    public void unfriend(String targetUid, DataCallback<Void> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) { callback.onError("Chưa đăng nhập!"); return; }

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(myUid).collection("friends").document(targetUid));
        batch.delete(db.collection("users").document(targetUid).collection("friends").document(myUid));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}