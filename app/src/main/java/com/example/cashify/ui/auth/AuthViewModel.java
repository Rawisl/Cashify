package com.example.cashify.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.FirebaseManager;

public class AuthViewModel extends ViewModel {
    //Bộ não của cả cụm auth. Nó sẽ gọi qua FirebaseManager để hỏi thông tin đăng nhập, sau đó báo kết quả lại cho Activity để chuyển màn hình.
    private final FirebaseManager firebaseManager;

    // LiveData để Activity quan sát (Observe) trạng thái
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isAuthSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> isAuthSuccess = _isAuthSuccess;

    public AuthViewModel() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    // ============================================================
    // TODO 1: LOGIC ĐĂNG NHẬP (EMAIL/PASSWORD)
    // - Gọi firebaseManager.loginWithEmail(email, password).
    // - Trước khi gọi: set _isLoading = true.
    // - Nếu thành công: set _isAuthSuccess = true.
    // - Nếu thất bại: set _errorMessage = lỗi trả về từ Firebase.
    // - Kết thúc: set _isLoading = false.
    // ============================================================
    public void login(String email, String password) {
        // Viết code vào đây
    }

    // ============================================================
    // TODO 2: LOGIC ĐĂNG KÝ TÀI KHOẢN MỚI
    // - Gọi firebaseManager.register(email, password, displayName).
    // - Sau khi Firebase tạo xong User Auth, phải gọi thêm lệnh
    //   để tạo một Document "User" mới trong Firestore và một
    //   Workspace "PERSONAL" mặc định cho user đó.
    // ============================================================
    public void register(String email, String password, String name) {
        // Viết code vào đây
    }

    // ============================================================
    // TODO 3: ĐĂNG NHẬP BẰNG GOOGLE
    // - Nhận idToken từ GoogleSignInAccount.
    // - Gọi firebaseManager.loginWithGoogle(idToken).
    // ============================================================
    public void loginWithGoogle(String idToken) {
        // Viết code vào đây
    }

    // ============================================================
    // TODO 4: QUÊN MẬT KHẨU
    // - Gọi firebaseManager.sendPasswordResetEmail(email).
    // - Báo thành công để Activity hiển thị Toast thông báo check mail.
    // ============================================================
    public void resetPassword(String email) {
        // Viết code vào đây
    }
}
