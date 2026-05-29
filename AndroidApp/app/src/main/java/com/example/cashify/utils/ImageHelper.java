package com.example.cashify.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.cashify.R;

import androidx.core.content.res.ResourcesCompat;

public class ImageHelper {

    public static void loadAvatar(Object url, ImageView target) {
        loadAvatar(url, target, null);
    }

    public static void loadAvatar(Object url, ImageView target, String identity) {
        if (target == null || target.getContext() == null) return;

        Drawable fallback = createInitialsAvatar(target.getContext(), identity, target.getWidth(), target.getHeight());
        if (isEmptyAvatarUrl(url)) {
            Glide.with(target.getContext()).clear(target);
            target.setImageDrawable(fallback);
            return;
        }

        Glide.with(target.getContext())
                .load(url)
                .placeholder(fallback)
                .error(fallback)
                .transform(new CenterCrop())
                .into(target);
    }

    public static Drawable createInitialsAvatar(Context context, String identity) {
        return createInitialsAvatar(context, identity, 0, 0);
    }

    public static void loadRectImage(String url, ImageView target) {
        if (target == null || target.getContext() == null) return;
        Glide.with(target.getContext()).load(url).centerCrop().into(target);
    }

    private static Drawable createInitialsAvatar(Context context, String identity, int viewWidth, int viewHeight) {
        int size = Math.max(Math.max(viewWidth, viewHeight), dp(context, 64));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        AvatarPalette palette = paletteFor(identity);
        RectF rect = new RectF(0f, 0f, size, size);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(palette.background);
        canvas.drawRect(rect, paint);

        String initials = initials(identity);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        Typeface interBold = ResourcesCompat.getFont(context, R.font.inter_bold);
        paint.setTypeface(interBold != null ? interBold : Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * (initials.length() > 1 ? 0.34f : 0.40f));

        Rect bounds = new Rect();
        paint.getTextBounds(initials, 0, initials.length(), bounds);
        canvas.drawText(initials, size / 2f, size / 2f - bounds.exactCenterY(), paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static boolean isEmptyAvatarUrl(Object url) {
        if (url == null) return true;
        if (url instanceof String) return ((String) url).trim().isEmpty();
        return false;
    }

    private static String initials(String identity) {
        if (identity == null || identity.trim().isEmpty()) return "HI";
        String clean = identity.trim();
        int at = clean.indexOf('@');
        if (at > 0) clean = clean.substring(0, at);

        String[] parts = clean.split("\\s+");
        String first = firstCodePoint(parts[0]);
        String second = parts.length > 1 ? firstCodePoint(parts[parts.length - 1]) : "";
        String result = (first + second).toUpperCase(java.util.Locale.getDefault());
        return result.isEmpty() ? "HI" : result;
    }

    private static String firstCodePoint(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Character.toChars(value.codePointAt(0)));
    }

    private static AvatarPalette paletteFor(String identity) {
        AvatarPalette[] palettes = {
                new AvatarPalette(Color.rgb(255, 123, 174)),
                new AvatarPalette(Color.rgb(255, 178, 132)),
                new AvatarPalette(Color.rgb(202, 171, 255)),
                new AvatarPalette(Color.rgb(133, 218, 177)),
                new AvatarPalette(Color.rgb(124, 190, 255)),
                new AvatarPalette(Color.rgb(255, 235, 170)),
                new AvatarPalette(Color.rgb(255, 204, 122)),
                new AvatarPalette(Color.rgb(255, 143, 134)),
                new AvatarPalette(Color.rgb(108, 209, 199))
        };
        int index = Math.abs((identity == null ? "HI" : identity).hashCode()) % palettes.length;
        return palettes[index];
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static class AvatarPalette {
        final int background;

        AvatarPalette(int background) {
            this.background = background;
        }
    }
}
