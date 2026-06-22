package com.example.cashify.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class RegisterActivity extends AppCompatActivity {

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
            // Null-safe text extraction
            String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
            String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            String pass = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
            String confirmPass = edtConfirmPassword.getText() != null ? edtConfirmPassword.getText().toString().trim() : "";

            // Basic validation
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                ToastHelper.show(this, "Please fill in all fields.");
                return;
            }

            // Email format validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(this, "Invalid email format (e.g., example@gmail.com).");
                return;
            }

            // Password strength and matching validation
            if (pass.length() < 6) {
                ToastHelper.show(this, "Password must be at least 6 characters.");
                return;
            }

            if (!pass.equals(confirmPass)) {
                ToastHelper.show(this, "Passwords do not match.");
                return;
            }

            // All validations passed, delegate execution to ViewModel
            authViewModel.register(email, pass, name);
        });

        // Terminate Activity to return to the Login screen
        tvHasAccount.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        authViewModel.isLoading.observe(this, isLoading -> {
            btnRegister.setEnabled(!isLoading);
            btnRegister.setText(isLoading ? "Processing..." : "Sign Up");
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(this, error);
            }
        });

        authViewModel.infoMessage.observe(this, info -> {
            if (info != null && !info.isEmpty()) {
                ToastHelper.show(this, info);
                finish();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}