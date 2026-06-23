package com.example.cashify.ui.settings;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.User;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.concurrent.Executors;

/**
 * SettingsViewModel.java
 * Handles user profile real-time updates, local database cleanup, and secure logout.
 */
public class SettingsViewModel extends ViewModel {

    private static final String TAG = "SettingsViewModel";

    // --- State & Data LiveData ---
    private final MutableLiveData<User> _userData = new MutableLiveData<>();
    public LiveData<User> userData = _userData;

    private final MutableLiveData<Boolean> _isLoggedOut = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoggedOut = _isLoggedOut;

    private final MutableLiveData<ResultStatus> resetStatus = new MutableLiveData<>();
    public LiveData<ResultStatus> getResetStatus() { return resetStatus; }

    // Listener to prevent memory leaks
    private ListenerRegistration userProfileListener;

    public SettingsViewModel() {
        loadUserProfile();
    }

    /**
     * Attaches a real-time listener to the current user's Firestore document.
     * Instantly reflects any profile changes (e.g., name, avatar) made in other screens.
     */
    public void loadUserProfile() {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove existing listener if any, before attaching a new one
        if (userProfileListener != null) {
            userProfileListener.remove();
        }

        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen to user profile changes.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        _userData.postValue(user);
                    }
                });
    }


    /**
     * Deletes all personal transactions from Cloud and Local DB.
     */
    public void resetAllTransactions(Context context) {
        FirebaseManager.getInstance().deleteAllTransactionsFromCloud("PERSONAL", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(context).transactionDao().deleteAllTransactions("PERSONAL");
                    resetStatus.postValue(new ResultStatus(true, ""));
                });
            }

            @Override
            public void onError(String message) {
                resetStatus.postValue(new ResultStatus(false, message));
            }
        });
    }

    public void clearResetStatus() {
        resetStatus.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Crucial: Detach the Firestore listener when the ViewModel dies to prevent memory leaks
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
    }

    // --- Helper Class for Status Observation ---
    public static class ResultStatus {
        public boolean isSuccess;
        public String message;

        public ResultStatus(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }
    }
}