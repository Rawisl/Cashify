package com.example.cashify.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import com.example.cashify.R;
import android.os.Build;

public class UploadNotificationHelper {

    private static final String CHANNEL_ID = "cashify_upload";
    private static final int NOTIF_ID = 1001;

    private final NotificationManager manager;
    private final NotificationCompat.Builder builder;

    public UploadNotificationHelper(Context context) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Tải ảnh lên", NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_camera) // dùng icon có sẵn trong app
                .setContentTitle("Đang tải ảnh lên")
                .setOngoing(true)      // Không swipe được khi đang upload
                .setOnlyAlertOnce(true) // Không rung/kêu mỗi lần update
                .setProgress(100, 0, false);
    }

    public void update(int percent) {
        builder.setContentText(percent + "%")
                .setProgress(100, percent, false);
        manager.notify(NOTIF_ID, builder.build());
    }

    public void done() {
        builder.setContentTitle("Tải ảnh thành công")
                .setContentText("")
                .setProgress(0, 0, false)
                .setOngoing(false); // Cho swipe được khi xong
        manager.notify(NOTIF_ID, builder.build());

        // Tự xóa notification sau 2 giây
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> manager.cancel(NOTIF_ID), 2000);
    }

    public void error() {
        builder.setContentTitle("Tải ảnh thất bại")
                .setContentText("Nhấn vào ứng dụng để thử lại")
                .setProgress(0, 0, false)
                .setOngoing(false);
        manager.notify(NOTIF_ID, builder.build());

        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> manager.cancel(NOTIF_ID), 4000);
    }
}