package com.example.cashify.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

public class HeartAnimation {

    /**
     * Rubber band animation — dùng cho bất kỳ ImageView nào.
     */
    public static void playRubberBand(ImageView target) {
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
     * Toggle like — đổi màu + animation + cập nhật count.
     *
     * @param context     context để lấy color
     * @param icon        ImageView icon
     * @param tvCount     TextView số lượng (null nếu không cần)
     * @param isLiked     trạng thái hiện tại
     * @param count       số lượng hiện tại
     * @param activeColor color khi liked (vd: R.color.status_red)
     * @return            trạng thái isLiked mới
     */
    public static boolean toggleLike(Context context, ImageView icon,
                                     TextView tvCount, boolean isLiked,
                                     int count, int activeColor) {
        boolean newState = !isLiked;

        if (newState) {
            icon.setColorFilter(context.getColor(activeColor));
            playRubberBand(icon);
            if (tvCount != null) tvCount.setText(String.valueOf(count + 1));
        } else {
            icon.clearColorFilter();
            if (tvCount != null) tvCount.setText(String.valueOf(count - 1));
        }

        return newState;
    }
}