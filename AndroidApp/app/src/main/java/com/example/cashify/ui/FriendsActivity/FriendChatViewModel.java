package com.example.cashify.ui.FriendsActivity;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FriendChatViewModel extends ViewModel {
    private static final String TAG = "FriendChatViewModel";

    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> loadErrorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> sendErrorMessage = new MutableLiveData<>();
    private boolean sending;

    // Lưu lại cái vòi hút để lát rút ra
    private ListenerRegistration messageListener;

    public LiveData<List<ChatMessage>> getChatMessages() { return chatMessages; }
    public LiveData<String> getLoadErrorMessage() { return loadErrorMessage; }
    public LiveData<String> getSendErrorMessage() { return sendErrorMessage; }

    public void startListeningMessages(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return;

        // Nếu đang cắm vòi hút cũ thì rút ra trước
        if (messageListener != null) {
            messageListener.remove();
        }

        // Bắt đầu lắng nghe Real-time
        messageListener = FirebaseManager.getInstance().listenToDirectMessages(friendUid, new FirebaseManager.DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                // Có tin nhắn mới (hoặc có người thu hồi) là UI tự nảy số ngay lập tức!
                chatMessages.postValue(messages);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Lỗi tải tin nhắn: " + message);
                loadErrorMessage.postValue(message);
            }
        });
    }

    public void sendMessage(String friendUid, String text) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty() || sending) return;

        sending = true;
        // VẪN GỌI QUA C# ĐỂ GHI DỮ LIỆU
        FirebaseManager.getInstance().sendDirectFriendMessage(friendUid, trimmed, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                sending = false;
            }

            @Override
            public void onError(String message) {
                sending = false;
                sendErrorMessage.postValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Cực kỳ quan trọng: Rút vòi hút khi Activity bị hủy để tránh Memory Leak
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}