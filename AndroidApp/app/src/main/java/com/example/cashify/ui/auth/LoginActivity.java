package com.example.cashify.ui.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ToastHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister, btnGoogleLogin;
    private TextView tvForgotPassword;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private final List<ObjectAnimator> welcomeAnimators = new ArrayList<>();

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
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                authViewModel.loginWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            ToastHelper.show(this, "Google Sign-In failed: " + e.getMessage());
                        }
                    } else {
                        // Reset loading state if user cancels the sign-in flow
                        authViewModel.resetLoadingState();
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

        startWelcomeAnimations();

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            String pass = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";

            if (email.isEmpty() || pass.isEmpty()) {
                ToastHelper.show(this, "Please enter your email and password.");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(this, "Invalid email format (e.g., example@gmail.com).");
                return;
            }

            authViewModel.login(email, pass);
        });

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void observeViewModel() {
        authViewModel.isLoading.observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
            btnGoogleLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Processing..." : "Sign In");
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(this, error);
            }
        });

        authViewModel.infoMessage.observe(this, info -> {
            if (info != null && !info.isEmpty()) {
                ToastHelper.show(this, info);
            }
        });

        authViewModel.isAuthSuccess.observe(this, isSuccess -> {
            if (isSuccess) {
                ToastHelper.show(this, "Sign in successful!");
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }

    // =========================================================================
    // UI ANIMATIONS
    // =========================================================================

    private void startWelcomeAnimations() {
        addFloatAnimation(findViewById(R.id.imgLoginMascot), -10f, 1900L, 0L);
        addFloatAnimation(findViewById(R.id.txtLoginStarOne), -6f, 2100L, 260L);
        addFloatAnimation(findViewById(R.id.txtLoginStarTwo), 7f, 2300L, 420L);
        addFloatAnimation(findViewById(R.id.txtLoginCoinOne), 8f, 2400L, 160L);
        addFloatAnimation(findViewById(R.id.txtLoginCoinTwo), -7f, 2200L, 360L);
        addPulseAnimation(findViewById(R.id.txtLoginHeart), 3000L, 500L);
    }

    private void addFloatAnimation(View view, float distance, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0f, distance, 0f);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        welcomeAnimators.add(animator);
    }

    private void addPulseAnimation(View view, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
        welcomeAnimators.add(scaleX);
        welcomeAnimators.add(scaleY);
    }

    @Override
    protected void onDestroy() {
        for (ObjectAnimator animator : welcomeAnimators) {
            animator.cancel();
        }
        welcomeAnimators.clear();
        super.onDestroy();
    }
}