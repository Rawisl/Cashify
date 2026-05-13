package com.example.cashify.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cashify.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class FirebaseMessagingServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Thông báo";
        String body = "";

        // Ưu tiên lấy từ Notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null ? remoteMessage.getNotification().getTitle() : title;
            body = remoteMessage.getNotification().getBody() != null ? remoteMessage.getNotification().getBody() : body;
        }
        // Lấy từ Data payload nếu có
        else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().containsKey("title") ? remoteMessage.getData().get("title") : title;
            body = remoteMessage.getData().containsKey("body") ? remoteMessage.getData().get("body") : body;
        }

        NotificationHelper notificationHelper = new NotificationHelper(this);
        // Tạo random ID để các thông báo không ghi đè lên nhau
        int notificationId = new Random().nextInt(3000) + 2000;

        notificationHelper.showNotification(title, body, NotificationHelper.CHANNEL_ID_PUSH, notificationId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "Refreshed token: " + token);

        // Vì FirebaseManager của bạn đã có hàm cập nhật token, ta chỉ việc gọi lại nó
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