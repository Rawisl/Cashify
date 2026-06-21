package com.example.cashify.ui.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        MaterialButton startButton = findViewById(R.id.btnOnboardingStart);
        TextView loginText = findViewById(R.id.txtOnboardingLogin);

        styleLoginText(loginText);

        // Nút Start -> Đi tới màn hình Đăng ký
        startButton.setOnClickListener(v -> navigateTo(RegisterActivity.class));

        // Chữ Sign In -> Đi tới màn hình Đăng nhập
        loginText.setOnClickListener(v -> navigateTo(LoginActivity.class));
    }

    private void styleLoginText(TextView textView) {
        String prompt = getString(R.string.onboarding_prompt);
        String action = getString(R.string.action_sign_in);
        String fullText = prompt + action;

        SpannableString spannable = new SpannableString(fullText);
        int start = prompt.length();
        int end = fullText.length();

        if (start >= 0 && end <= fullText.length()) {
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannable);
    }

    /**
     * Hàm điều hướng chung, có xử lý đóng màn hình hiện tại (finish)
     * để người dùng không thể back lại trang Onboarding sau khi đã đi tiếp.
     */
    private void navigateTo(Class<?> destinationActivity) {
        startActivity(new Intent(this, destinationActivity));
        //Xóa dòng finish() đi nếu muốn người dùng có thể bấm nút Back để xem lại Onboarding
        // finish();
    }
}