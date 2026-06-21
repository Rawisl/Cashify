package com.example.cashify.ui.FriendsActivity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.DirectConversation;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MessagesViewModel extends ViewModel {

    private final MutableLiveData<List<DirectConversation>> conversations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration conversationListener;

    public LiveData<List<DirectConversation>> getConversations() { return conversations; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void startListening() {
        // Prevent multiple real-time listener registrations
        if (conversationListener != null) return;

        loading.setValue(true);
        conversationListener = FirebaseManager.getInstance()
                .listenToDirectConversations(new FirebaseManager.DataCallback<List<DirectConversation>>() {
                    @Override
                    public void onSuccess(List<DirectConversation> data) {
                        conversations.postValue(data != null ? data : new ArrayList<>());
                        loading.postValue(false);
                    }

                    @Override
                    public void onError(String message) {
                        error.postValue(message);
                        loading.postValue(false);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // CRITICAL: Remove Firestore listener to prevent memory leaks when ViewModel is destroyed
        if (conversationListener != null) {
            conversationListener.remove();
            conversationListener = null;
        }
    }
}