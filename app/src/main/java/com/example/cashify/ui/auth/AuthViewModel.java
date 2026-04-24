package com.example.cashify.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    //Bộ não của cả cụm auth. Nó sẽ gọi qua FirebaseManager để hỏi thông tin đăng nhập, sau đó báo kết quả lại cho Activity để chuyển màn hình.
    private final FirebaseManager firebaseManager;

    // LiveData để Activity quan sát (Observe) trạng thái
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _infoMessage = new MutableLiveData<>();
    public LiveData<String> infoMessage = _infoMessage;

    private final MutableLiveData<Boolean> _isAuthSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> isAuthSuccess = _isAuthSuccess;

    private final MutableLiveData<Boolean> _isResetMailSent = new MutableLiveData<>(false);
    public LiveData<Boolean> isResetMailSent = _isResetMailSent;

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
        _isLoading.setValue(true);

        // Dùng AuthCallback thay cho addOnSuccessListener
        firebaseManager.loginWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                if (user != null) {
                    // ÉP FIREBASE TẢI LẠI DỮ LIỆU MỚI NHẤT (Đề phòng user vừa mới click link trong mail xong)
                    user.reload().addOnCompleteListener(task -> {
                        _isLoading.setValue(false);

                        // KIỂM TRA HÀNG REAL: Đã xác thực mail chưa?
                        if (user.isEmailVerified()) {
                            _isAuthSuccess.setValue(true); // Pass! Cho vào nhà
                        } else {
                            firebaseManager.logout(); // Chưa xác thực -> Đăng xuất ngay lập tức
                            _errorMessage.setValue("Account is not verified! Please check your email.");
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // TODO 2: LOGIC ĐĂNG KÝ TÀI KHOẢN MỚI
    // - Gọi firebaseManager.register(email, password, displayName).
    // - Sau khi Firebase tạo xong User Auth, phải gọi thêm lệnh
    //   để tạo một Document "User" mới trong Firestore và một
    //   Workspace "PERSONAL" mặc định cho user đó.
    // ============================================================
    public void register(String email, String password, String name) {
        _isLoading.setValue(true);

        // Lưu ý: Tên hàm bên FirebaseManager giờ là registerWithEmail
        firebaseManager.registerWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                if (user != null) {
                    // LỆNH BẮN EMAIL XÁC THỰC ĐẾN NGƯỜI DÙNG
                    user.sendEmailVerification().addOnCompleteListener(task -> {
                        _isLoading.setValue(false);
                        firebaseManager.logout();

                        // Báo cho UI hiện chữ thành công
                        _infoMessage.setValue("Register successful! Please check your email to verify.");
                    });
                }
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // TODO 3: ĐĂNG NHẬP BẰNG GOOGLE
    // - Nhận idToken từ GoogleSignInAccount.
    // - Gọi firebaseManager.loginWithGoogle(idToken).
    // ============================================================
    public void loginWithGoogle(String idToken) {
        _isLoading.setValue(true);

        firebaseManager.loginWithGoogle(idToken, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                _isLoading.setValue(false);
                _isAuthSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // TODO 4: QUÊN MẬT KHẨU
    // - Gọi firebaseManager.sendPasswordResetEmail(email).
    // - Báo thành công để Activity hiển thị Toast thông báo check mail.
    // ============================================================
    public void resetPassword(String email) {
        _isLoading.setValue(true);

        // Gọi trực tiếp getAuth() từ FirebaseManager
        firebaseManager.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    _isLoading.setValue(false);
                    _isResetMailSent.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(e.getMessage());
                });
    }
}
