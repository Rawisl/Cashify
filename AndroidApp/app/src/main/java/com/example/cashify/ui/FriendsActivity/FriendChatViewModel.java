package com.example.cashify.ui.FriendsActivity;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FriendChatViewModel extends ViewModel {
    private static final String TAG = "FriendChatViewModel";

    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> loadErrorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> sendErrorMessage = new MutableLiveData<>();
    private boolean sending;

    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public LiveData<String> getLoadErrorMessage() {
        return loadErrorMessage;
    }

    public LiveData<String> getSendErrorMessage() {
        return sendErrorMessage;
    }

    public void loadMessages(String friendUid) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Log.w(TAG, "Loading messages failed: current user is not authenticated");
            loadErrorMessage.setValue("Chua dang nhap!");
            return;
        }
        if (friendUid == null || friendUid.isEmpty()) {
            Log.w(TAG, "Loading messages failed: friendUid is missing");
            loadErrorMessage.setValue("Khong the tai tin nhan");
            return;
        }

        FirebaseManager.getInstance().getDirectFriendMessages(friendUid, new FirebaseManager.DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                chatMessages.postValue(messages != null ? messages : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Loading messages failed: " + message);
                loadErrorMessage.postValue(message);
            }
        });
    }

    public void sendMessage(String friendUid, String text) {
        String trimmed = text != null ? text.trim() : "";
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            Log.w(TAG, "Sending message failed: current user is not authenticated");
            sendErrorMessage.setValue("Chua dang nhap!");
            return;
        }
        if (friendUid == null || friendUid.isEmpty()) {
            Log.w(TAG, "Sending message failed: friendUid is missing");
            sendErrorMessage.setValue("Nguoi nhan khong hop le");
            return;
        }
        if (trimmed.isEmpty() || sending) return;

        sending = true;
        FirebaseManager.getInstance().sendDirectFriendMessage(friendUid, trimmed, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                sending = false;
                loadMessages(friendUid);
            }

            @Override
            public void onError(String message) {
                sending = false;
                Log.e(TAG, "Sending message failed: " + message);
                sendErrorMessage.postValue(message);
            }
        });
    }
}
