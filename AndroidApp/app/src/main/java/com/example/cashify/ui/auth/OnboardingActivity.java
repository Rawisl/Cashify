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

        startButton.setOnClickListener(v -> openLogin());
        loginText.setOnClickListener(v -> openLogin());
    }

    private void styleLoginText(TextView textView) {
        String text = "Đã có tài khoản? Đăng nhập";
        SpannableString spannable = new SpannableString(text);
        int start = text.indexOf("Đăng nhập");
        if (start >= 0) {
            int end = start + "Đăng nhập".length();
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

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }
}
