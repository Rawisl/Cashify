package com.example.cashify.data.remote;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

    // ============================================================
    // TODO 1: TÀI KHOẢN (AUTHENTICATION)
    // - Viết hàm loginWithEmail(email, password)
    // - Viết hàm loginWithGoogle(idToken)
    // - Viết hàm getCurrentUserId() để lấy UID làm mỏ neo dữ liệu
    // ============================================================

    // ============================================================
    // TODO 2: ĐỒNG BỘ CÁ NHÂN (PERSONAL DATA)
    // - Viết hàm syncLocalToCloud(): Đẩy data từ SQLite lên Firestore
    // - Viết hàm listenToPersonalChanges(): Lắng nghe data cá nhân real-time
    // ============================================================

    // ============================================================
    // TODO 3: CỘNG ĐỒNG (SOCIAL & SHARED WORKSPACE)
    // - Viết logic Tìm kiếm User qua Email
    // - Viết hàm createSharedWorkspace(): Tạo quỹ chung
    // - Viết hàm joinWorkspace(): Thêm thành viên vào quỹ
    // ============================================================

    // ============================================================
    // TODO 4: THÔNG BÁO (FCM)
    // - Hàm lấy Firebase Cloud Messaging Token
    // - Hàm gửi yêu cầu "Nhắc nợ" đến UID cụ thể
    // ============================================================

    // --- Interface để báo kết quả về cho bên gọi (ViewModel/Repository) ---
    public interface AuthCallback {
        void onSuccess(String uid);
        void onError(String message);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
