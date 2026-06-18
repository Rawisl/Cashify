package com.example.cashify.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class FeedSkeletonCardView extends View {

    // =========================================================================
    // CONSTANTS & RESOURCES
    // =========================================================================
    private static final int CARD_COLOR = Color.WHITE;
    private static final int BASE_COLOR = Color.rgb(226, 231, 238);
    private static final int HIGHLIGHT_COLOR = Color.rgb(242, 245, 249);

    // Pre-allocated arrays to avoid GC thrashing in onDraw
    private static final int[] SHIMMER_COLORS = {BASE_COLOR, HIGHLIGHT_COLOR, BASE_COLOR};
    private static final float[] SHIMMER_POSITIONS = {0f, 0.5f, 1f};

    // =========================================================================
    // DRAWING TOOLS (Pre-allocated)
    // =========================================================================
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix shaderMatrix = new Matrix();

    private LinearGradient shimmerGradient;
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
        cardPaint.setColor(CARD_COLOR);
        placeholderPaint.setColor(BASE_COLOR);
        // Force hardware acceleration for this specific view to ensure silky smooth 60/120fps
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    // =========================================================================
    // LIFECYCLE & ANIMATION
    // =========================================================================

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

    // =========================================================================
    // MEASUREMENT & SHADER INITIALIZATION
    // =========================================================================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            // Create the gradient ONLY ONCE when the view size is determined.
            // Width of the shimmer sweep is 55% of the total width on each side (total 1.1x width)
            float sweepWidth = w * 0.55f;
            shimmerGradient = new LinearGradient(
                    0, 0, sweepWidth * 2, h,
                    SHIMMER_COLORS,
                    SHIMMER_POSITIONS,
                    Shader.TileMode.CLAMP
            );
            placeholderPaint.setShader(shimmerGradient);
        }
    }

    // =========================================================================
    // DRAWING
    // =========================================================================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Shift the shader using Matrix transformation (Zero object allocation!)
        updateShaderMatrix(width);

        // Render Base Card
        drawRound(canvas, 0, 0, width, height, dp(22), cardPaint);

        // Pre-calculate common metrics to minimize primitive math ops
        float pad = dp(16);
        float top = dp(16);
        float avatar = dp(56);

        // Render Header (Avatar & Text lines)
        drawRound(canvas, pad, top, pad + avatar, top + avatar, dp(18), placeholderPaint);
        drawRound(canvas, pad + dp(70), top + dp(5), width - pad - dp(72), top + dp(20), dp(8), placeholderPaint);
        drawRound(canvas, pad + dp(70), top + dp(29), pad + dp(180), top + dp(41), dp(7), placeholderPaint);
        drawRound(canvas, width - pad - dp(42), top + dp(8), width - pad, top + dp(50), dp(18), placeholderPaint);

        // Render Paragraph Text
        float contentTop = top + avatar + dp(18);
        drawRound(canvas, pad, contentTop, width - pad, contentTop + dp(13), dp(7), placeholderPaint);
        drawRound(canvas, pad, contentTop + dp(24), width - pad - dp(48), contentTop + dp(37), dp(7), placeholderPaint);
        drawRound(canvas, pad, contentTop + dp(48), width - pad - dp(112), contentTop + dp(61), dp(7), placeholderPaint);

        // Render Main Image Placeholder
        float imageTop = contentTop + dp(82);
        drawRound(canvas, pad, imageTop, width - pad, imageTop + dp(104), dp(18), placeholderPaint);

        // Render Meta Information
        float metaTop = imageTop + dp(120);
        drawRound(canvas, pad, metaTop, pad + dp(130), metaTop + dp(12), dp(7), placeholderPaint);
        drawRound(canvas, width - pad - dp(82), metaTop, width - pad, metaTop + dp(12), dp(7), placeholderPaint);

        // Render Action Buttons (Like/Comment/Share)
        float actionTop = metaTop + dp(26);
        float gap = dp(8);
        float buttonW = (width - (pad * 2) - (gap * 2)) / 3f;
        drawRound(canvas, pad, actionTop, pad + buttonW, actionTop + dp(36), dp(18), placeholderPaint);
        drawRound(canvas, pad + buttonW + gap, actionTop, pad + (buttonW * 2) + gap, actionTop + dp(36), dp(18), placeholderPaint);
        drawRound(canvas, pad + (buttonW * 2) + (gap * 2), actionTop, width - pad, actionTop + dp(36), dp(18), placeholderPaint);
    }

    /**
     * Translates the existing shader matrix instead of instantiating a new LinearGradient.
     * This eliminates object allocation and GC overhead during the 60fps animation.
     */
    private void updateShaderMatrix(float width) {
        if (shimmerGradient != null) {
            float sweepWidth = width * 0.55f;
            float center = width * shimmerOffset;

            // Move the matrix horizontally based on the animator's offset
            shaderMatrix.setTranslate(center - sweepWidth, 0);
            shimmerGradient.setLocalMatrix(shaderMatrix);
        }
    }

    private void drawRound(Canvas canvas, float left, float top, float right, float bottom, float radius, Paint paint) {
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}