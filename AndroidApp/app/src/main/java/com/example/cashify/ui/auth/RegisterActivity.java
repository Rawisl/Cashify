package com.example.cashify.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;

public class RegisterActivity extends AppCompatActivity {
    //màn hình đăng ký
    private AuthViewModel authViewModel;
    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvHasAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void initViews() {
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvHasAccount = findViewById(R.id.tvHasAccount);

        btnRegister.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            String confirmPass = edtConfirmPassword.getText().toString().trim();

            // Kiểm tra rỗng (Thêm confirmPass vào check cho kỹ)
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                ToastHelper.show(this, "Please fill all fields");
                return;
            }

            // THIẾU Ở ĐÂY: Kiểm tra định dạng Email hợp lệ (có @ và domain)
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(this, "Email is invalid! (Ex: abc@gmail.com)");
                return;
            }

            // Kiểm tra độ dài mật khẩu
            if (pass.length() < 6) {
                ToastHelper.show(this, "Password must be at least 6 characters");
                return;
            }

            // Kiểm tra khớp mật khẩu
            if (!pass.equals(confirmPass)) {
                ToastHelper.show(this, "Confirm password does not match");
                return;
            }

            // Mọi thứ OK -> Gọi ViewModel
            authViewModel.register(email, pass, name);
        });

        // ============================================================
        // TODO 3: NÚT "ĐÃ CÓ TÀI KHOẢN"
        // - Kết thúc Activity (finish()) để quay lại màn hình Login.
        // ============================================================

        tvHasAccount.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 4: QUAN SÁT VIEWMODEL
        // - isLoading: Hiện/ẩn ProgressBar.
        // - errorMessage: Thông báo nếu Email đã tồn tại hoặc lỗi mạng.
        // - isAuthSuccess: Nếu thành công, hiện Toast chúc mừng
        //   và chuyển thẳng vào MainActivity (nhớ gọi finish() cái Register này).
        // ============================================================

        authViewModel.isLoading.observe(this, isLoading -> {
            btnRegister.setEnabled(!isLoading);
            btnRegister.setText(isLoading ? "Processing..." : "Register");
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(this, "Error: " + error);
            }
        });

        authViewModel.infoMessage.observe(this, info -> {
            if (info != null && !info.isEmpty()) {
                ToastHelper.show(this, info);
                finish();
            }
        });

//        Để dành cái này sau phát triển
//        authViewModel.isAuthSuccess.observe(this, isSuccess -> {
//            if (isSuccess) {
//               ToastHelper.show(this, "Register successful!");
//                startActivity(new Intent(this, MainActivity.class));
//                finishAffinity(); // Đóng hết tất cả màn hình Login/Register
//            }
//        });
    }
}
