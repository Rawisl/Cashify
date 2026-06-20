package com.example.cashify.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cashify.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

// Xử lý thông báo đẩy (Push Notifications) chạy ngầm từ Firebase Cloud Messaging
public class FirebaseMessagingServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Notification";
        String body = "";

        // Ưu tiên đọc dữ liệu từ Notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null ? remoteMessage.getNotification().getTitle() : title;
            body = remoteMessage.getNotification().getBody() != null ? remoteMessage.getNotification().getBody() : body;
        }
        // Fallback đọc từ Data payload (dùng khi gửi custom data từ backend)
        else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().containsKey("title") ? remoteMessage.getData().get("title") : title;
            body = remoteMessage.getData().containsKey("body") ? remoteMessage.getData().get("body") : body;
        }

        NotificationHelper notificationHelper = new NotificationHelper(this);
        // Tạo random ID để thông báo mới không đè mất thông báo cũ trên khay hệ thống
        int notificationId = new Random().nextInt(3000) + 2000;

        notificationHelper.showNotification(title, body, NotificationHelper.CHANNEL_ID_PUSH, notificationId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "Refreshed token: " + token);

        // Tự động đẩy token mới cấp phát lên Firestore để Backend biết đường bắn thông báo
        FirebaseManager.getInstance().getFcmToken(new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String data) {
                Log.d("FCM_TOKEN", "Token updated to Firestore successfully");
            }

            @Override
            public void onError(String message) {
                Log.e("FCM_TOKEN", "Failed to update token: " + message);
            }
        });
    }
}