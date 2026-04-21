package com.example.cashify.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class ForgotPasswordActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_forgot_password);

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initViews() {
        // ============================================================
        // TODO 2: BẮT SỰ KIỆN NÚT "GỬI YÊU CẦU"
        // - Lấy Email từ EditText.
        // - Kiểm tra định dạng Email (không để trống, đúng chuẩn @...).
        // - Nếu OK: Gọi authViewModel.resetPassword(email).
        // ============================================================

        // ============================================================
        // TODO 3: NÚT QUAY LẠI (BACK)
        // - Kết thúc Activity (finish()) để quay về màn hình Login.
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 4: QUAN SÁT KẾT QUẢ TỪ VIEWMODEL
        // - isLoading: Hiện/ẩn cái xoay vòng (ProgressBar) để user biết app đang chạy.
        // - errorMessage: Nếu Firebase chửi (Email không tồn tại...), hiện Toast báo lỗi.
        // - isAuthSuccess: Nếu thành công, hiện thông báo: "Vui lòng kiểm tra email để đặt lại mật khẩu"
        //   và tự động finish() Activity sau vài giây.
        // ============================================================
    }
}
