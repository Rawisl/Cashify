package com.example.cashify.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        notificationHelper.showNotification(
                "Expense Reminder",
                "Have you added any transactions today?",
                NotificationHelper.CHANNEL_ID_REMINDER,
                NotificationHelper.NOTIFICATION_ID_REMINDER
        );
        return Result.success();
    }
}