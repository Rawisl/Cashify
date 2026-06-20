package com.example.cashify.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.example.cashify.R;

// Utility class quản lý hiển thị tiến trình (Progress) upload ảnh lên thanh thông báo hệ thống
public class UploadNotificationHelper {

    private static final String CHANNEL_ID = "cashify_upload";

    private static final int NOTIF_ID = 1001;

    private final NotificationManager manager;
    private final NotificationCompat.Builder builder;

    public UploadNotificationHelper(Context context) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Bắt buộc phải khai báo Channel từ Android 8.0 (Oreo) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Upload Image", NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null); // Tắt âm để không "ting ting" làm phiền user
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_camera)
                .setContentTitle("Uploading image...")
                .setOngoing(true)       // Khóa thông báo: Chặn user vuốt tắt đi khi đang chạy
                .setOnlyAlertOnce(true) // Chỉ rung/hiện popup thông báo ở lần 0% đầu tiên
                .setProgress(100, 0, false);
    }

    public void update(int percent) {
        if (manager == null) return;
        builder.setContentText(percent + "%")
                .setProgress(100, percent, false);
        manager.notify(NOTIF_ID, builder.build());
    }

    public void done() {
        if (manager == null) return;
        builder.setContentTitle("Upload completed")
                .setContentText("")
                .setProgress(0, 0, false)
                .setOngoing(false); // Hoàn thành thì mở khóa cho phép user quẹt tắt
        manager.notify(NOTIF_ID, builder.build());

        // Tự động dọn rác thanh trạng thái: Tắt thông báo sau 2 giây
        new Handler(Looper.getMainLooper()).postDelayed(() -> manager.cancel(NOTIF_ID), 2000);
    }

    public void error() {
        if (manager == null) return;
        builder.setContentTitle("Upload failed")
                .setContentText("Please try again later")
                .setProgress(0, 0, false)
                .setOngoing(false);
        manager.notify(NOTIF_ID, builder.build());

        // Lỗi thì để hiển thị lâu hơn một chút (4 giây) cho user kịp đọc
        new Handler(Looper.getMainLooper()).postDelayed(() -> manager.cancel(NOTIF_ID), 4000);
    }
}