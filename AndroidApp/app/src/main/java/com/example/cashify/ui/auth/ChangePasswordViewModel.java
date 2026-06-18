package com.example.cashify.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public class ChangePasswordViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public LiveData<String> successMessage = _successMessage;

    private final MutableLiveData<Boolean> _isGoogleUser = new MutableLiveData<>(false);
    public LiveData<Boolean> isGoogleUser = _isGoogleUser;

    private final MutableLiveData<Boolean> _hasEmailProvider = new MutableLiveData<>(false);
    public LiveData<Boolean> hasEmailProvider = _hasEmailProvider;

    public void checkUserProviders() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        boolean isGoogle = false;
        boolean hasEmail = false;

        for (UserInfo info : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) isGoogle = true;
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) hasEmail = true;
        }

        _isGoogleUser.setValue(isGoogle);
        _hasEmailProvider.setValue(hasEmail);
    }

    public void changePassword(String currentPw, String newPw) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            _errorMessage.setValue("Authentication error. Please log in again.");
            return;
        }

        _isLoading.setValue(true);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPw);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(newPw)
                        .addOnSuccessListener(v2 -> {
                            _isLoading.setValue(false);
                            _successMessage.setValue("PASSWORD_CHANGED");
                        })
                        .addOnFailureListener(e -> {
                            _isLoading.setValue(false);
                            _errorMessage.setValue(e.getLocalizedMessage());
                        })
                )
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue("WRONG_CURRENT_PASSWORD");
                });
    }

    public void linkEmailPassword(String newPw) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            _errorMessage.setValue("Authentication error. Please log in again.");
            return;
        }

        _isLoading.setValue(true);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), newPw);

        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    _isLoading.setValue(false);
                    _successMessage.setValue("PASSWORD_LINKED");
                    checkUserProviders(); // Refresh UI state automatically
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(e.getLocalizedMessage());
                });
    }

    public void clearMessages() {
        _errorMessage.setValue(null);
        _successMessage.setValue(null);
    }
}