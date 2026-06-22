package com.example.cashify.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ForgotPasswordActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmailReset;
    private Button btnSendReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        ImageView btnBack = findViewById(R.id.btnBack);

        btnSendReset.setOnClickListener(v -> {
            String email = edtEmailReset.getText().toString().trim();

            if (email.isEmpty()) {
                ToastHelper.show(this, "Please enter your email address");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastHelper.show(this, "Invalid email address");
                return;
            }

            authViewModel.resetPassword(email);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        authViewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                btnSendReset.setEnabled(!isLoading);
                btnSendReset.setText(isLoading ? "Sending..." : "Send Instructions");
            }
        });

        authViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(this, "Error: " + error);

                //Xóa lỗi sau khi hiện để chống lỗi xoay màn hình
                authViewModel.clearErrorMessage();
            }
        });

        authViewModel.isResetMailSent.observe(this, isSent -> {
            if (isSent != null && isSent) {
                ToastHelper.show(this, "Check your email to reset your password");

                //Reset state trước khi thoát
                authViewModel.clearResetMailStatus();
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