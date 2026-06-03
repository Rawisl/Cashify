package com.example.cashify.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class FeedSkeletonCardView extends View {
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private ValueAnimator shimmerAnimator;
    private float shimmerOffset = -1f;

    public FeedSkeletonCardView(Context context) {
        super(context);
        init();
    }

    public FeedSkeletonCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeedSkeletonCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        cardPaint.setColor(Color.WHITE);
        placeholderPaint.setColor(Color.rgb(226, 231, 238));
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopShimmer();
        super.onDetachedFromWindow();
    }

    private void startShimmer() {
        if (shimmerAnimator != null) return;
        shimmerAnimator = ValueAnimator.ofFloat(-0.65f, 1.65f);
        shimmerAnimator.setDuration(1450L);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        shimmerAnimator.start();
    }

    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        updateShader(width, height);
        drawRound(canvas, 0, 0, width, height, dp(22), cardPaint);

        float pad = dp(16);
        float avatar = dp(56);
        float top = dp(16);
        drawRound(canvas, pad, top, pad + avatar, top + avatar, dp(18), placeholderPaint);
        drawRound(canvas, pad + dp(70), top + dp(5), width - pad - dp(72), top + dp(20), dp(8), placeholderPaint);
        drawRound(canvas, pad + dp(70), top + dp(29), pad + dp(180), top + dp(41), dp(7), placeholderPaint);
        drawRound(canvas, width - pad - dp(42), top + dp(8), width - pad, top + dp(50), dp(18), placeholderPaint);

        float contentTop = top + avatar + dp(18);
        drawRound(canvas, pad, contentTop, width - pad, contentTop + dp(13), dp(7), placeholderPaint);
        drawRound(canvas, pad, contentTop + dp(24), width - pad - dp(48), contentTop + dp(37), dp(7), placeholderPaint);
        drawRound(canvas, pad, contentTop + dp(48), width - pad - dp(112), contentTop + dp(61), dp(7), placeholderPaint);

        float imageTop = contentTop + dp(82);
        drawRound(canvas, pad, imageTop, width - pad, imageTop + dp(104), dp(18), placeholderPaint);

        float metaTop = imageTop + dp(120);
        drawRound(canvas, pad, metaTop, pad + dp(130), metaTop + dp(12), dp(7), placeholderPaint);
        drawRound(canvas, width - pad - dp(82), metaTop, width - pad, metaTop + dp(12), dp(7), placeholderPaint);

        float actionTop = metaTop + dp(26);
        float gap = dp(8);
        float buttonW = (width - (pad * 2) - (gap * 2)) / 3f;
        drawRound(canvas, pad, actionTop, pad + buttonW, actionTop + dp(36), dp(18), placeholderPaint);
        drawRound(canvas, pad + buttonW + gap, actionTop, pad + (buttonW * 2) + gap, actionTop + dp(36), dp(18), placeholderPaint);
        drawRound(canvas, pad + (buttonW * 2) + (gap * 2), actionTop, width - pad, actionTop + dp(36), dp(18), placeholderPaint);
    }

    private void updateShader(float width, float height) {
        float sweepWidth = width * 0.55f;
        float center = width * shimmerOffset;
        placeholderPaint.setShader(new LinearGradient(
                center - sweepWidth,
                0,
                center + sweepWidth,
                height,
                new int[]{
                        Color.rgb(226, 231, 238),
                        Color.rgb(242, 245, 249),
                        Color.rgb(226, 231, 238)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
    }

    private void drawRound(Canvas canvas, float left, float top, float right, float bottom, float radius, Paint paint) {
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
