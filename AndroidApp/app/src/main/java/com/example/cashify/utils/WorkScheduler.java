package com.example.cashify.utils;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

// Utility class hỗ trợ lên lịch các tác vụ chạy ngầm định kỳ
public class WorkScheduler {

    // Tên định danh duy nhất của tiến trình để WorkManager không tạo ra các tiến trình rác trùng lặp
    private static final String REMINDER_WORK_NAME = "daily_expense_reminder";

    public static void scheduleDailyReminder(Context context) {
        Calendar currentTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        // Cài đặt giờ mục tiêu: 19:10:00 mỗi ngày
        targetTime.set(Calendar.HOUR_OF_DAY, 19);
        targetTime.set(Calendar.MINUTE, 10);
        targetTime.set(Calendar.SECOND, 0);

        // Nếu thời điểm gọi hàm này đã qua 19h10 của ngày hôm nay, ta dời mốc đếm ngược sang 19h10 ngày mai
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Tính khoảng thời gian chờ (delay) từ lúc gọi hàm cho đến lần chạy đầu tiên
        long initialDelay = targetTime.getTimeInMillis() - currentTime.getTimeInMillis();

        // Khởi tạo Request: Chạy lặp lại mỗi 24 tiếng (1 DAY)
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        // Đẩy vào hàng đợi của WorkManager
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // UPDATE: Cập nhật lại lịch trình mới nếu tiến trình này đã từng tồn tại
                reminderRequest
        );
    }
}