package com.example.cashify.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class RegisterActivity extends AppCompatActivity {
    //màn hình đăng ký
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_register);

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initViews() {
        // ============================================================
        // TODO 2: BẮT SỰ KIỆN NÚT "ĐĂNG KÝ"
        // - Lấy data từ các EditText: Name, Email, Password, Confirm Password.
        // - Kiểm tra logic (Validation):
        //   + Không bỏ trống trường nào.
        //   + Email đúng định dạng.
        //   + Password tối thiểu 6 ký tự (Quy định của Firebase).
        //   + Password và Confirm Password phải trùng khớp 100%.
        // - Nếu OK: Gọi authViewModel.register(email, password, name).
        // ============================================================

        // ============================================================
        // TODO 3: NÚT "ĐÃ CÓ TÀI KHOẢN"
        // - Kết thúc Activity (finish()) để quay lại màn hình Login.
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 4: QUAN SÁT VIEWMODEL
        // - isLoading: Hiện/ẩn ProgressBar.
        // - errorMessage: Thông báo nếu Email đã tồn tại hoặc lỗi mạng.
        // - isAuthSuccess: Nếu thành công, hiện Toast chúc mừng
        //   và chuyển thẳng vào MainActivity (nhớ gọi finish() cái Register này).
        // ============================================================
    }
}
