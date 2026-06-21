package com.example.cashify.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * A custom ImageView that displays an avatar within a rounded rectangle,
 * featuring a dynamically calculated outer border and clipped inner bounds.
 */
public class AvatarImageView extends AppCompatImageView {

    // =========================================================================
    // CONFIGURATION CONSTANTS
    // =========================================================================
    private static final float BORDER_THICKNESS_DP = 2.8f;
    private static final float CORNER_RADIUS_RATIO = 0.32f;
    private static final int DEFAULT_BORDER_COLOR = Color.BLACK;

    // =========================================================================
    // DRAWING TOOLS (Pre-allocated to avoid object creation during onDraw)
    // =========================================================================
    private final Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF outerRect = new RectF();
    private final RectF innerRect = new RectF();

    private float contentInset;

    public AvatarImageView(Context context) {
        super(context);
        init(context);
    }

    public AvatarImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AvatarImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        contentInset = BORDER_THICKNESS_DP * density;

        outerPaint.setStyle(Paint.Style.FILL);
        outerPaint.setColor(DEFAULT_BORDER_COLOR);

        setScaleType(ScaleType.CENTER_CROP);

        // Apply padding so the base ImageView doesn't render content over the border area
        int padding = Math.round(contentInset);
        setPadding(padding, padding, padding, padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Define bounding boxes
        outerRect.set(0f, 0f, getWidth(), getHeight());
        innerRect.set(contentInset, contentInset, getWidth() - contentInset, getHeight() - contentInset);

        // 2. Calculate dynamic corner radii
        float outerRadius = Math.min(getWidth(), getHeight()) * CORNER_RADIUS_RATIO;
        float innerRadius = Math.max(0f, outerRadius - contentInset);

        // 3. Render the solid outer border
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, outerPaint);

        // 4. Clip the canvas to prevent the image from spilling out of the inner bounds
        int saveCount = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        // 5. Delegate the actual image rendering to the parent class
        super.onDraw(canvas);

        // 6. Restore canvas state
        canvas.restoreToCount(saveCount);
    }
}