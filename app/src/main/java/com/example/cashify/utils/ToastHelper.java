package com.example.cashify.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastHelper {
    private static long lastToastTime = 0;
    private static String lastMessage = "";
    private static final int COOLDOWN_TIME = 2000; // 2000 mili-giây (2 giây)

    // =========================================================
    // HÀM 1: truyền chữ trực tiếp (String)
    // =========================================================
    public static void show(Context context, String message) {
        long currentTime = System.currentTimeMillis();

        // NẾU: Chưa qua 2 giây VÀ nội dung y hệt câu trước đó thì block luôn
        if (currentTime - lastToastTime < COOLDOWN_TIME && message.equals(lastMessage)) {
            return;
        }

        // Qua được ải thì cho hiện Toast bình thường
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        // Lưu lại câu hiện tại để lát còn kiểm tra thằng spam tiếp theo
        lastToastTime = currentTime;
        lastMessage = message;
    }

    // =========================================================
    // HÀM 2: Truyền ID int - R.string
    // =========================================================
    public static void show(Context context, int stringResId) {
        // Hàm này tự động dịch cái ID ra thành chữ, rồi trả kết quả ngược lên cho HÀM 1 ở trên xử lý tiếp
        show(context, context.getString(stringResId));
    }
}
