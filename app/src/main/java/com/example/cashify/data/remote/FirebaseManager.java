package com.example.cashify.data.remote;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Để gọi API Firebase
public class FirebaseManager {
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
    // TODO 1: TÀI KHOẢN (AUTHENTICATION)
    // - Viết hàm loginWithEmail(email, password)
    // - Viết hàm loginWithGoogle(idToken)
    // - Viết hàm getCurrentUserId() để lấy UID làm mỏ neo dữ liệu
    // ============================================================

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void registerWithEmail(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    // Tạo ngay một Document chứa thông tin User trên Firestore
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", email);
                    userMap.put("uid", uid);
                    db.collection("users").document(uid).set(userMap);

                    callback.onSuccess(uid);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void loginWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    // Lưu thông tin user Google vào Firestore nếu chưa có
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", authResult.getUser().getEmail());
                    userMap.put("uid", uid);
                    db.collection("users").document(uid).set(userMap); // set tự động ghi đè hoặc tạo mới

                    callback.onSuccess(uid);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void logout() {
        auth.signOut();
    }

    // ============================================================
    // TODO 2: ĐỒNG BỘ CÁ NHÂN (PERSONAL DATA)
    // - Viết hàm syncLocalToCloud(): Đẩy data từ SQLite lên Firestore
    // - Viết hàm listenToPersonalChanges(): Lắng nghe data cá nhân real-time
    // ============================================================

    public String getCurrentUserUid() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // Hàm đẩy 1 Giao dịch từ SQLite lên Firestore
    public void syncLocalToCloud(String collection, String docId, Map<String, Object> data, DataCallback<Void> callback) {
        String uid = getCurrentUserUid();

        if (uid == null) {
            if (callback != null) callback.onError("Not logged in!");
            return;
        }

        db.collection("users").document(uid)
                .collection(collection).document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d("FIREBASE", "Synchronous success: " + collection);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                    Log.e("FIREBASE", "Synchronous failed: " + e.getMessage());
                });
    }

    // Lắng nghe dữ liệu thay đổi trên Cloud để tải về máy
    public void listenToPersonalChanges(String collectionName, DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        db.collection("users").document(uid).collection(collectionName)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        callback.onSuccess(value.getDocuments());
                    }
                });
    }

    public void deleteAllTransactionsFromCloud(DataCallback<Void> callback) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("transactions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // TODO 3: CỘNG ĐỒNG (SOCIAL & SHARED WORKSPACE)
    // - Viết logic Tìm kiếm User qua Email
    // - Viết hàm createSharedWorkspace(): Tạo quỹ chung
    // - Viết hàm joinWorkspace(): Thêm thành viên vào quỹ
    // ============================================================

    public void searchUserByEmail(String email, DataCallback<String> callback) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Trả về UID của người tìm thấy
                        String foundUid = queryDocumentSnapshots.getDocuments().get(0).getString("uid");
                        callback.onSuccess(foundUid);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createSharedWorkspace(String workspaceName, List<String> memberIds, DataCallback<String> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        memberIds.add(uid); // Tự động thêm bản thân mình vào danh sách thành viên

        Map<String, Object> workspace = new HashMap<>();
        workspace.put("name", workspaceName);
        workspace.put("ownerId", uid);
        workspace.put("members", memberIds);

        db.collection("workspaces").add(workspace)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId())) // Trả về ID của Workspace mới
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinWorkspace(String workspaceId, String userId, DataCallback<Void> callback) {
        // Cập nhật mảng 'members' trong Workspace: Thêm userId vào
        db.collection("workspaces").document(workspaceId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // TODO 4: THÔNG BÁO (FCM)
    // - Hàm lấy Firebase Cloud Messaging Token
    // - Hàm gửi yêu cầu "Nhắc nợ" đến UID cụ thể
    // ============================================================

    public void getFcmToken(DataCallback<String> callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    // Lưu token vào Firestore để sau này server biết đường gửi tới máy này
                    String uid = getCurrentUserId();
                    if (uid != null) {
                        db.collection("users").document(uid).update("fcmToken", token);
                    }
                    callback.onSuccess(token);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Để gửi thông báo từ máy Client sang máy Client khác, thường phải thông qua "Trạm trung chuyển" (Cloud Functions).
    // Ở đây, ta ghi một yêu cầu (Request) vào Firestore, Cloud Functions sẽ đọc cái này và bắn FCM đi.
    public void sendPaymentReminder(String targetUid, String messageTitle, String messageBody, DataCallback<Void> callback) {
        Map<String, Object> notificationReq = new HashMap<>();
        notificationReq.put("toUid", targetUid);
        notificationReq.put("title", messageTitle);
        notificationReq.put("body", messageBody);
        notificationReq.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notification_requests").add(notificationReq)
                .addOnSuccessListener(documentReference -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ============================================================
    // TODO Bonus: Xử lý dữ liệu của cá nhân
    // ============================================================

    // Hàm lấy toàn bộ giao dịch từ Firestore của User
    public void getAllTransactionsFromCloud(DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Not logged in!");
            return;
        }

        db.collection("users").document(uid).collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllBudgetsFromCloud(DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        db.collection("users").document(uid).collection("budgets").get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(queryDocumentSnapshots.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllCategoriesFromCloud(DataCallback<List<DocumentSnapshot>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        db.collection("users").document(uid).collection("categories").get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(queryDocumentSnapshots.getDocuments()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
