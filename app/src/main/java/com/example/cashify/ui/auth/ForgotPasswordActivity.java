package com.example.cashify.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;

public class ForgotPasswordActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private EditText edtEmailReset;
    private Button btnSendReset;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_forgot_password);
        setContentView(R.layout.activity_forgot_password);

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initViews() {
        edtEmailReset = findViewById(R.id.edtEmailReset);
        btnSendReset = findViewById(R.id.btnSendReset);
        btnBack = findViewById(R.id.btnBack);

        // ============================================================
        // TODO 2: BẮT SỰ KIỆN NÚT "GỬI YÊU CẦU"
        // - Lấy Email từ EditText.
        // - Kiểm tra định dạng Email (không để trống, đúng chuẩn @...).
        // - Nếu OK: Gọi authViewModel.resetPassword(email).
        // ============================================================

        btnSendReset.setOnClickListener(v -> {
            String email = edtEmailReset.getText().toString().trim();

            // Kiểm tra rỗng
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra định dạng Email hợp lệ (có @, domain...)
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email is invalid", Toast.LENGTH_SHORT).show();
                return;
            }

            // Nếu OK: Gọi ViewModel xử lý
            authViewModel.resetPassword(email);
        });

        // ============================================================
        // TODO 3: NÚT QUAY LẠI (BACK)
        // - Kết thúc Activity (finish()) để quay về màn hình Login.
        // ============================================================

        btnBack.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 4: QUAN SÁT KẾT QUẢ TỪ VIEWMODEL
        // - isLoading: Hiện/ẩn cái xoay vòng (ProgressBar) để user biết app đang chạy.
        // - errorMessage: Nếu Firebase chửi (Email không tồn tại...), hiện Toast báo lỗi.
        // - isAuthSuccess: Nếu thành công, hiện thông báo: "Vui lòng kiểm tra email để đặt lại mật khẩu"
        //   và tự động finish() Activity sau vài giây.
        // ============================================================

        // Trạng thái Loading
        authViewModel.isLoading.observe(this, isLoading -> {
            btnSendReset.setEnabled(!isLoading);
            btnSendReset.setText(isLoading ? "Sending..." : "Send Reset");
        });

        // Trạng thái Lỗi
        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Trạng thái Thành công (Lưu ý: Gọi đúng biến isResetMailSent)
        authViewModel.isResetMailSent.observe(this, isSent -> {
            if (isSent) {
                Toast.makeText(this, "Check your email to reset password", Toast.LENGTH_LONG).show();
                // Tự động đóng màn hình sau khi gửi thành công để về lại Login
                finish();
            }
        });
    }
}
