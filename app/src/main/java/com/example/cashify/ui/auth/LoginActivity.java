package com.example.cashify.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {
    //màn hình login
    //Vụ Google Login: Nhắc bạn đó phải lấy cái SHA-1 của máy tính bạn đó dán vào Firebase Console thì cái nút Google Login mới chạy được. Không có SHA-1 là nó báo lỗi 12500 hoặc 10 ngay.
    //UI Thread: Vì mình dùng ViewModel và LiveData rồi nên bạn đó không cần lo lắng về Thread nữa, cứ để LiveData nó "bắn" thông báo về là xong.
    private AuthViewModel authViewModel;
    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister, btnGoogleLogin;
    private TextView tvForgotPassword;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_login);
        setContentView(R.layout.activity_login);

        initViewModel();
        setupGoogleSignIn();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupGoogleSignIn() {
        // Cấu hình yêu cầu lấy Email và ID Token từ Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("195049395718-bagas4hvn2onafmdvd0dqdntsj81o9ef.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Đăng ký bộ lắng nghe kết quả trả về từ màn hình chọn tài khoản Google
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Đăng nhập Google thành công -> Lấy Token ném qua ViewModel
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                authViewModel.loginWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            ToastHelper.show(this, "Google sign in failed: " + e.getMessage());
                        }
                    } else {
                        // User bấm nút Back hủy đăng nhập, có thể tắt Loading nếu đang bật
                        authViewModel.isLoading.observe(this, isLoading -> { /* Tắt loading ở đây nếu cần */ });
                    }
                });
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // ============================================================
        // TODO 2: XỬ LÝ ĐĂNG NHẬP EMAIL/PASSWORD
        // - Lấy data từ 2 EditText Email và Password.
        // - Bắt sự kiện nút Login: Gọi authViewModel.login(email, password).
        // ============================================================

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                ToastHelper.show(this, "Please fill all fields");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(this, "Email is invalid! (VD: abc@gmail.com)");
                return;
            }

            authViewModel.login(email, pass);
        });

        // ============================================================
        // TODO 3: XỬ LÝ ĐĂNG NHẬP GOOGLE
        // - Khởi tạo GoogleSignInClient (cần Web Client ID từ Firebase Console).
        // - Bắt sự kiện nút Google Login: Mở Intent chọn tài khoản của Google.
        // - Trong onActivityResult: Lấy idToken và gọi authViewModel.loginWithGoogle(idToken).
        // ============================================================

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // ============================================================
        // TODO 4: ĐIỀU HƯỚNG SANG MÀN HÌNH KHÁC
        // - Nút "Đăng ký ngay": Mở RegisterActivity.
        // - Chữ "Quên mật khẩu?": Mở ForgotPasswordActivity.
        // ============================================================

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 5: THEO DÕI TRẠNG THÁI TỪ VIEWMODEL
        // - isLoading: Hiện/ẩn ProgressBar và vô hiệu hóa (disable) các nút bấm để tránh user bấm liên tục.
        // - errorMessage: Hiển thị lỗi (ví dụ: Sai mật khẩu) lên giao diện hoặc Toast.
        // - isAuthSuccess: Nếu TRUE -> Chuyển sang MainActivity và gọi finish() để user không quay lại màn hình Login được nữa.
        // ============================================================

        authViewModel.isLoading.observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
            btnGoogleLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Processing..." : "Login");
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(this, "Error: " + error);
            }
        });

        authViewModel.infoMessage.observe(this, info -> {
            if (info != null && !info.isEmpty()) {
                ToastHelper.show(this, info);
            }
        });

        authViewModel.isAuthSuccess.observe(this, isSuccess -> {
            if (isSuccess) {
                ToastHelper.show(this, "Login successful!");
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}
