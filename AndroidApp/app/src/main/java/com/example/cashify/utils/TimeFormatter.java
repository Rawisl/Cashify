package com.example.cashify.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TimeFormatter.java
 * Chuyển timestamp (ms) → chuỗi thân thiện tiếng Việt.
 *
 *  < 1 phút   → "Vừa xong"
 *  < 60 phút  → "X phút trước"
 *  < 24 giờ   → "X giờ trước"
 *  >= 24 giờ  → "HH:mm dd/MM"
 */
public final class TimeFormatter {

    private static final long MINUTE = 60_000L;
    private static final long HOUR   = 60 * MINUTE;
    private static final long DAY    = 24 * HOUR;

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

    public static String format(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;

        if (diff < MINUTE)       return "Just now";
        if (diff < HOUR)         return (diff / MINUTE) + " minutes ago";
        if (diff < DAY)          return (diff / HOUR)   + " hours ago";
        return SDF.format(new Date(timestamp));
    }

    private TimeFormatter() {}
}