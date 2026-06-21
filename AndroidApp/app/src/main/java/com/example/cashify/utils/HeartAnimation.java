package com.example.cashify.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class HeartAnimation {

    /**
     * Plays a rubber band animation on any target ImageView.
     * Often used to draw attention to interactive icons (e.g., likes, bookmarks).
     */
    public static void playRubberBand(View target) {
        target.setScaleX(1f);
        target.setScaleY(1f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, "scaleX",
                1f, 1.4f, 0.8f, 1.2f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, "scaleY",
                1f, 0.75f, 1.25f, 0.9f, 1.05f, 1f);
        scaleX.setDuration(500);
        scaleY.setDuration(500);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    /**
     * Toggles the like state, applying color changes, animations, and updating the counter.
     *
     * @param context     Context to retrieve color resources.
     * @param icon        The ImageView representing the heart/like button.
     * @param tvCount     The TextView displaying the like count (can be null).
     * @param isLiked     The current like state.
     * @param count       The current like count.
     * @param activeColor The color resource ID when liked (e.g., R.color.status_red).
     * @return            The new like state (boolean).
     */
    public static boolean toggleLike(Context context, ImageView icon,
                                     TextView tvCount, boolean isLiked,
                                     int count, int activeColor) {
        boolean newState = !isLiked;

        if (newState) {
            icon.setColorFilter(ContextCompat.getColor(context, activeColor));
            playRubberBand(icon);
            if (tvCount != null) tvCount.setText(String.valueOf(count + 1));
        } else {
            icon.clearColorFilter();
            if (tvCount != null) tvCount.setText(String.valueOf(count - 1));
        }

        return newState;
    }
}