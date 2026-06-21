package com.example.cashify.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility chuyển timestamp (ms) thành chuỗi thời gian tương đối hiển thị trên UI.
 *
 * < 1 phút   -> "Just now"
 * < 60 phút  -> "X minute(s) ago"
 * < 24 giờ   -> "X hour(s) ago"
 * >= 24 giờ  -> "HH:mm dd/MM"
 */
public final class TimeFormatter {

    private static final long MINUTE = 60_000L;
    private static final long HOUR   = 60 * MINUTE;
    private static final long DAY    = 24 * HOUR;

    public static String format(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;

        if (diff < MINUTE) {
            return "Just now";
        }

        if (diff < HOUR) {
            long minutes = diff / MINUTE;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        if (diff < DAY) {
            long hours = diff / HOUR;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        // Khởi tạo SDF cục bộ tại đây để tránh lỗi Thread-Safety gây crash app
        // khi gọi liên tục từ RecyclerView Adapter ở nhiều luồng khác nhau.
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.ENGLISH);
        return sdf.format(new Date(timestamp));
    }

    // Chặn việc dùng từ khóa 'new' để khởi tạo object thừa thãi
    private TimeFormatter() {}
}