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

public class AvatarImageView extends AppCompatImageView {
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
        contentInset = 2.8f * density;

        outerPaint.setStyle(Paint.Style.FILL);
        outerPaint.setColor(Color.BLACK);

        setScaleType(ScaleType.CENTER_CROP);
        setPadding(Math.round(contentInset), Math.round(contentInset),
                Math.round(contentInset), Math.round(contentInset));
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        outerRect.set(0f, 0f, getWidth(), getHeight());
        innerRect.set(contentInset, contentInset, getWidth() - contentInset, getHeight() - contentInset);

        float outerRadius = Math.min(getWidth(), getHeight()) * 0.32f;
        float innerRadius = Math.max(0f, outerRadius - contentInset);

        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, outerPaint);

        int save = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        super.onDraw(canvas);
        canvas.restoreToCount(save);

    }
}
