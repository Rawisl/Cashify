package com.example.cashify.ui.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.example.cashify.R;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.main.MainActivity;

import java.util.Random;

public class SplashActivity extends Activity {

    private static final long SPLASH_DELAY_MS = 1400L;
    private ObjectAnimator mascotBounceAnimator;

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (FirebaseManager.getInstance().getAuth().getCurrentUser() != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        if (mascotBounceAnimator != null) {
            mascotBounceAnimator.cancel();
        }
        super.onDestroy();
    }
}
