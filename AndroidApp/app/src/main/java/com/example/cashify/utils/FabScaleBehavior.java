package com.example.cashify.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Custom Behavior for FloatingActionButton.
 * Hides the FAB when scrolling down and shows it when scrolling up.
 */
public class FabScaleBehavior extends FloatingActionButton.Behavior {

    public FabScaleBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // 1. Only listen to vertical scroll events
    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull FloatingActionButton child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    // 2. Handle the animation during the scroll event
    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                               @NonNull FloatingActionButton child,
                               @NonNull View target,
                               int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed,
                               int type,
                               @NonNull int[] consumed) {

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);

        // Scroll DOWN (dy > 0) -> Trigger shrink/hide animation
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);
                    // Force INVISIBLE instead of GONE to prevent CoordinatorLayout jumping/stuttering
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        }
        // Scroll UP (dy < 0) -> Trigger pop/show animation
        else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            child.show();
        }
    }
}