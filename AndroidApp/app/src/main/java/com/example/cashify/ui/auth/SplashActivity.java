package com.example.cashify.ui.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.example.cashify.R;
import com.example.cashify.data.repository.AuthRepositoryImpl;
import com.example.cashify.data.repository.IAuthRepository;
import com.example.cashify.ui.main.MainActivity;

import java.util.Random;

public class SplashActivity extends Activity {

    private static final long SPLASH_DELAY_MS = 1400L;

    private ObjectAnimator mascotBounceAnimator;
    private final Handler routingHandler = new Handler(Looper.getMainLooper());
    private Runnable routingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        setupLoadingMessage();
        startSplashAnimations();
        routeAfterLoading();
    }

    private void setupLoadingMessage() {
        TextView loadingText = findViewById(R.id.txtSplashLoading);
        if (loadingText == null) return;

        int[] messages = {
                R.string.splash_loading_default,
                R.string.splash_loading_wallet,
                R.string.splash_loading_transactions,
                R.string.splash_loading_goals,
                R.string.splash_loading_done
        };
        loadingText.setText(messages[new Random().nextInt(messages.length)]);
    }

    private void startSplashAnimations() {
        View mascot = findViewById(R.id.imgSplashMascot);
        if (mascot != null) {
            mascotBounceAnimator = ObjectAnimator.ofFloat(mascot, "translationY", 0f, -8f, 0f);
            mascotBounceAnimator.setDuration(600L);
            mascotBounceAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mascotBounceAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mascotBounceAnimator.start();
        }
    }

    private void routeAfterLoading() {
        routingRunnable = () -> {
            IAuthRepository authRepo = new AuthRepositoryImpl();
            Intent intent;

            if (authRepo.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        };

        routingHandler.postDelayed(routingRunnable, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        // Dọn dẹp animation
        if (mascotBounceAnimator != null) {
            mascotBounceAnimator.cancel();
        }

        // Hủy bộ đếm chuyển màn hình nếu user thoát app sớm để chống rò rỉ bộ nhớ
        if (routingHandler != null && routingRunnable != null) {
            routingHandler.removeCallbacks(routingRunnable);
        }

        super.onDestroy();
    }
}