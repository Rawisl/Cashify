package com.example.cashify.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.Toast;

public class ToastHelper {
    private static long lastToastTime = 0;
    private static String lastMessage = "";
    private static final int COOLDOWN_TIME = 2000; // 2000 mili-giây (2 giây)

    // =========================================================
    // HÀM 1: Truyền chữ trực tiếp (String) - Chống spam
    // =========================================================
    public static void show(Context context, String message) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastToastTime < COOLDOWN_TIME && message.equals(lastMessage)) {
            return;
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        lastToastTime = currentTime;
        lastMessage = message;
    }

    // =========================================================
    // HÀM 2: Truyền ID int - R.string
    // =========================================================
    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }

    // =========================================================
    // HÀM 3: TOAST KÈM RUNG MÁY (Dành riêng cho Thành Tựu)
    // =========================================================
    public static void showAchievement(Context context, String achievementName) {
        // 1. Gọi hàm show() ở trên để hiện Toast tiếng Anh + chống spam
        String message = "🏆 Achievement Unlocked: " + achievementName;
        show(context, message);

        // 2. Kích hoạt động cơ Rung (Haptic Feedback)
        try {
            Vibrator vibrator;
            // Android 12 (API 31) trở lên dùng VibratorManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                // Nhịp rung: [Chờ, Rung, Chờ, Rung] (Tính bằng mili-giây)
                // Rung 100ms -> nghỉ 100ms -> Rung 150ms -> Dứt khoát (Kiểu báo tin vui)
                long[] pattern = {0, 100, 100, 150};

                // Android 8 (API 26) trở lên dùng VibrationEffect
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)); // -1 nghĩa là chỉ rung 1 lần, không lặp lại
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            // Đề phòng máy tính bảng không có cục rung hoặc quên cấp quyền trong Manifest
            android.util.Log.e("CASHIFY_VIBRATE", "Cannot vibrate device: " + e.getMessage());
        }
    }
}