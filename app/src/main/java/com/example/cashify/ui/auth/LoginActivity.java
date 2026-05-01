package com.example.cashify.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister, btnGoogleLogin;
    private TextView tvForgotPassword;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("195049395718-bagas4hvn2onafmdvd0dqdntsj81o9ef.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                authViewModel.loginWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            ToastHelper.show(this, "Google sign in failed: " + e.getMessage());
                        }
                    } else {
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

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void observeViewModel() {
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
                // 🚀 GỌI HÀM LƯU LÊN FIRESTORE NGAY KHI ĐĂNG NHẬP THÀNH CÔNG
                saveUserToFirestore(FirebaseAuth.getInstance().getCurrentUser());

                ToastHelper.show(this, "Login successful!");
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }

    // ============================================================
    // HÀM NHẬP HỘ KHẨU USER LÊN FIRESTORE (LƯU TÊN & AVATAR)
    // ============================================================
    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("uid", firebaseUser.getUid());
        userMap.put("email", firebaseUser.getEmail());

        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            userMap.put("displayName", firebaseUser.getDisplayName());
        }
        if (firebaseUser.getPhotoUrl() != null) {
            userMap.put("avatarUrl", firebaseUser.getPhotoUrl().toString());
        }

        // Dùng SetOptions.merge() để lưu mà không đè mất data cũ
        db.collection("users").document(firebaseUser.getUid())
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> android.util.Log.d("AUTH", "Information saved successfully!"))
                .addOnFailureListener(e -> android.util.Log.e("AUTH", "Firestore save error: " + e.getMessage()));
    }
}