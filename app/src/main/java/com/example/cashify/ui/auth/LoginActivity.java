package com.example.cashify.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class LoginActivity extends AppCompatActivity {
    //màn hình login
    //Vụ Google Login: Nhắc bạn đó phải lấy cái SHA-1 của máy tính bạn đó dán vào Firebase Console thì cái nút Google Login mới chạy được. Không có SHA-1 là nó báo lỗi 12500 hoặc 10 ngay.
    //UI Thread: Vì mình dùng ViewModel và LiveData rồi nên bạn đó không cần lo lắng về Thread nữa, cứ để LiveData nó "bắn" thông báo về là xong.
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_login);

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initViews() {
        // ============================================================
        // TODO 2: XỬ LÝ ĐĂNG NHẬP EMAIL/PASSWORD
        // - Lấy data từ 2 EditText Email và Password.
        // - Bắt sự kiện nút Login: Gọi authViewModel.login(email, password).
        // ============================================================

        // ============================================================
        // TODO 3: XỬ LÝ ĐĂNG NHẬP GOOGLE
        // - Khởi tạo GoogleSignInClient (cần Web Client ID từ Firebase Console).
        // - Bắt sự kiện nút Google Login: Mở Intent chọn tài khoản của Google.
        // - Trong onActivityResult: Lấy idToken và gọi authViewModel.loginWithGoogle(idToken).
        // ============================================================

        // ============================================================
        // TODO 4: ĐIỀU HƯỚNG SANG MÀN HÌNH KHÁC
        // - Nút "Đăng ký ngay": Mở RegisterActivity.
        // - Chữ "Quên mật khẩu?": Mở ForgotPasswordActivity.
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 5: THEO DÕI TRẠNG THÁI TỪ VIEWMODEL
        // - isLoading: Hiện/ẩn ProgressBar và vô hiệu hóa (disable) các nút bấm để tránh user bấm liên tục.
        // - errorMessage: Hiển thị lỗi (ví dụ: Sai mật khẩu) lên giao diện hoặc Toast.
        // - isAuthSuccess: Nếu TRUE -> Chuyển sang MainActivity và gọi finish() để user không quay lại màn hình Login được nữa.
        // ============================================================
    }
}
