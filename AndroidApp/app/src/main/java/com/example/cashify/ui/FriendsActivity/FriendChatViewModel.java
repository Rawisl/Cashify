package com.example.cashify.ui.FriendsActivity;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.data.model.User;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.MediaRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FriendChatViewModel extends ViewModel {
    private static final String TAG = "FriendChatViewModel";

    // Data Repositories
    private final MediaRepository mediaRepository = new MediaRepository();

    private final MutableLiveData<User> friendProfile = new MutableLiveData<>();
    public LiveData<User> getFriendProfile() { return friendProfile; }

    // UI States (Chat)
    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> loadErrorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> sendErrorMessage = new MutableLiveData<>();

    // UI States (Media)
    private final MutableLiveData<Boolean> isUploading = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> pendingImages = new MutableLiveData<>(new ArrayList<>());

    // Active connection states
    private boolean isSending = false;
    private ListenerRegistration messageListener;

    // --- Getters for UI observation ---
    public LiveData<List<ChatMessage>> getChatMessages() { return chatMessages; }
    public LiveData<String> getLoadErrorMessage() { return loadErrorMessage; }
    public LiveData<String> getSendErrorMessage() { return sendErrorMessage; }
    public LiveData<Boolean> getIsUploading() { return isUploading; }
    public LiveData<List<String>> getPendingImages() { return pendingImages; }


    // =========================================================================
    // FRIEND PROFILE HANDLING (NEW)
    // =========================================================================

    public void loadFriendProfile(String uid) {
        if (uid == null || uid.isEmpty()) return;

        // Tự động fetch thông tin User từ Firestore thay vì chờ Intent truyền qua
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        friendProfile.postValue(user);
                    }
                })
                .addOnFailureListener(e -> loadErrorMessage.postValue("Failed to load friend profile: " + e.getMessage()));
    }


    // =========================================================================
    // MEDIA HANDLING
    // =========================================================================

    public void uploadChatImage(File imageFile) {
        isUploading.setValue(true);
        mediaRepository.uploadImage(imageFile, new MediaRepository.UploadCallback() {
            @Override
            public void onProgress(int percent) {}

            @Override
            public void onSuccess(String imageUrl) {
                isUploading.postValue(false);
                List<String> current = pendingImages.getValue();
                if (current != null) {
                    current.add(imageUrl);
                    pendingImages.postValue(current);
                }
                imageFile.delete();
            }

            @Override
            public void onFailure(String error) {
                isUploading.postValue(false);
                sendErrorMessage.postValue("Image upload failed: " + error);
                imageFile.delete();
            }
        });
    }

    public void removePendingImage(String imageUrl) {
        List<String> current = pendingImages.getValue();
        if (current != null) {
            current.remove(imageUrl);
            pendingImages.setValue(current);
        }
    }


    // =========================================================================
    // CHAT HANDLING (SEND & RECALL)
    // =========================================================================

    public boolean submitMessages(String friendUid, String text) {
        List<String> urls = pendingImages.getValue();
        if ((text == null || text.trim().isEmpty()) && (urls == null || urls.isEmpty())) {
            return false;
        }

        List<String> urlsToSend = new ArrayList<>(urls != null ? urls : new ArrayList<>());
        pendingImages.setValue(new ArrayList<>());
        sendMessagesSequentially(friendUid, text, urlsToSend, 0);
        return true;
    }

    private void sendMessagesSequentially(String friendUid, String text, List<String> urls, int index) {
        if (index >= urls.size()) {
            if (urls.isEmpty()) {
                sendMessage(friendUid, text, null);
            }
            return;
        }

        String imgUrl = urls.get(index);
        String msgText = (index == 0) ? text : "";
        sendMessage(friendUid, msgText, imgUrl,
                () -> sendMessagesSequentially(friendUid, text, urls, index + 1));
    }

    public void sendMessage(String friendUid, String text, String imageUrl) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty() && (imageUrl == null || imageUrl.isEmpty())) return;
        if (isSending) return;

        isSending = true;
        FirebaseManager.getInstance().sendDirectFriendMessage(
                friendUid, trimmed, imageUrl,
                new FirebaseManager.DataCallback<Void>() {
                    @Override public void onSuccess(Void data) { isSending = false; }
                    @Override public void onError(String message) {
                        isSending = false;
                        sendErrorMessage.postValue(message);
                    }
                });
    }

    public void sendMessage(String friendUid, String text, String imageUrl, Runnable onSuccess) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty() && (imageUrl == null || imageUrl.isEmpty())) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        FirebaseManager.getInstance().sendDirectFriendMessage(
                friendUid, trimmed, imageUrl,
                new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        isSending = false;
                        if (onSuccess != null) onSuccess.run();
                    }
                    @Override
                    public void onError(String message) {
                        isSending = false;
                        sendErrorMessage.postValue(message);
                        if (onSuccess != null) onSuccess.run();
                    }
                });
    }

    public void recallMessage(String friendUid, String messageId) {
        FirebaseManager.getInstance().recallDirectFriendMessage(friendUid, messageId, new FirebaseManager.DataCallback<Void>() {
            @Override public void onSuccess(Void data) {}
            @Override public void onError(String message) {
                sendErrorMessage.postValue("Failed to unsend message: " + message);
            }
        });
    }


    // =========================================================================
    // REAL-TIME LISTENER & LIFECYCLE
    // =========================================================================

    public void startListeningMessages(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return;

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener = FirebaseManager.getInstance().listenToDirectMessages(friendUid, new FirebaseManager.DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                chatMessages.postValue(messages);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load messages: " + message);
                loadErrorMessage.postValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}