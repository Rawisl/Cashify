package com.example.cashify.utils;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.cashify.data.model.Notification;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WorkScheduler {
    private static final String REMINDER_WORK_NAME = "daily_expense_reminder";

    public static void scheduleDailyReminder(Context context) {
        Calendar currentTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        targetTime.set(Calendar.HOUR_OF_DAY, 19); // Nhắc vào xx giờ
        targetTime.set(Calendar.MINUTE, 10);
        targetTime.set(Calendar.SECOND, 0);

        // Nếu thời gian hiện tại đã qua xx, dời lịch sang xx ngày mai
        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = targetTime.getTimeInMillis() - currentTime.getTimeInMillis();

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(Notification.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Cập nhật nếu đã tồn tại
                reminderRequest
        );
    }
}