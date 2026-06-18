package com.example.cashify.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ShineAnimationHelper {

    private final View shineView;
    private final View containerView;
    private AnimatorSet shineAnimator;
    private int shineLoopId = 0;

    public ShineAnimationHelper(View shineView, View containerView) {
        this.shineView = shineView;
        this.containerView = containerView;
    }

    public void start() {
        if (shineView == null || containerView == null) return;

        stop(); // Clear any existing animation first
        int loopId = ++shineLoopId;

        shineView.setAlpha(0f);
        shineView.setTranslationX(0f);
        shineView.setTranslationY(0f);
        shineView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        shineView.post(() -> {
            if (shineLoopId != loopId || shineView.getWindowToken() == null) return;

            float cardWidth = Math.max(containerView.getWidth(), 360);
            float cardHeight = Math.max(containerView.getHeight(), 260);
            float shineHeight = Math.max(shineView.getHeight(), cardHeight);

            float startX = -(cardWidth * 0.75f) - shineView.getWidth() - 80f;
            float startY = (cardHeight * 0.65f) + 140f;
            float endX = cardWidth + (shineHeight * 0.45f) + 80f;
            float endY = -cardHeight - 180f;

            shineView.setTranslationX(startX);
            shineView.setTranslationY(startY);

            ObjectAnimator moveX = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_X, startX, endX);
            ObjectAnimator moveY = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_Y, startY, endY);
            ObjectAnimator opacity = ObjectAnimator.ofFloat(shineView, View.ALPHA, 0f, 0f, 0.28f, 0.28f, 0f);

            shineAnimator = new AnimatorSet();
            shineAnimator.playTogether(moveX, moveY, opacity);
            shineAnimator.setDuration(2300L);
            shineAnimator.setStartDelay(50L);
            shineAnimator.setInterpolator(new LinearInterpolator());
            shineAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (shineLoopId == loopId && shineAnimator != null && shineView.getWindowToken() != null) {
                        start(); // Loop animation
                    }
                }
            });
            shineAnimator.start();
        });
    }

    public void stop() {
        shineLoopId++;
        if (shineAnimator != null) {
            shineAnimator.removeAllListeners();
            shineAnimator.cancel();
            shineAnimator = null;
        }
        if (shineView != null) {
            shineView.setAlpha(0f);
            shineView.setTranslationX(0f);
            shineView.setTranslationY(0f);
            shineView.clearAnimation();
        }
    }
}