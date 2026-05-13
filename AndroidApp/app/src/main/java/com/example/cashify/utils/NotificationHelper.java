package com.example.cashify.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.cashify.R;
import com.example.cashify.ui.main.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_REMINDER = "expense_reminder_channel";
    public static final String CHANNEL_ID_PUSH = "push_notification_channel";
    public static final int NOTIFICATION_ID_REMINDER = 1001;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel cho Local Reminder
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    "Expense Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            reminderChannel.setDescription("Daily Expense Reminder's Channel");

            // Channel cho Push Notification (FCM)
            NotificationChannel pushChannel = new NotificationChannel(
                    CHANNEL_ID_PUSH,
                    "System Notification",
                    NotificationManager.IMPORTANCE_HIGH
            );
            pushChannel.setDescription("Important Events & Debt Reminders Chanel");

            notificationManager.createNotificationChannel(reminderChannel);
            notificationManager.createNotificationChannel(pushChannel);
        }
    }

    public void showNotification(String title, String message, String channelId, int notificationId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                // TODO: Hãy đảm bảo bạn có icon ic_notification trong thư mục res/drawable
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(channelId.equals(CHANNEL_ID_PUSH) ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }
}