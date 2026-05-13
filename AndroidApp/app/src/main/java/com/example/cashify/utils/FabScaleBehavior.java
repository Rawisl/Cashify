package com.example.cashify.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FabScaleBehavior extends FloatingActionButton.Behavior {

    public FabScaleBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // 1. Chỉ lắng nghe sự kiện cuộn dọc (lên/xuống)
    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull FloatingActionButton child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    // 2. Xử lý hoạt ảnh khi ngón tay đang cuộn
    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                               @NonNull FloatingActionButton child,
                               @NonNull View target,
                               int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed,
                               int type,
                               @NonNull int[] consumed) {

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);

        // Cuộn XUỐNG (dy > 0) -> Kích hoạt hiệu ứng thu nhỏ (Shrink/Hide)
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);
                    // Ép nó thành INVISIBLE thay vì GONE để không làm co giật layout
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        }
        // Cuộn LÊN (dy < 0) -> Kích hoạt hiệu ứng nở ra (Pop/Show)
        else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            child.show();
        }
    }
}