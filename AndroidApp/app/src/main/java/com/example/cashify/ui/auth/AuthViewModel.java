package com.example.cashify.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends ViewModel {

    // Core dependency for authentication operations
    private final FirebaseManager firebaseManager;

    // Internal MutableLiveData (Writable) -> External LiveData (Read-only)
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _infoMessage = new MutableLiveData<>();
    public LiveData<String> infoMessage = _infoMessage;

    private final MutableLiveData<Boolean> _isAuthSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> isAuthSuccess = _isAuthSuccess;

    private final MutableLiveData<Boolean> _isResetMailSent = new MutableLiveData<>(false);
    public LiveData<Boolean> isResetMailSent = _isResetMailSent;

    public AuthViewModel() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    // Resets the loading state (e.g., when a user cancels the Google Sign-In flow)
    public void resetLoadingState() {
        _isLoading.setValue(false);
    }

    // ============================================================
    // EMAIL / PASSWORD LOGIN
    // ============================================================
    public void login(String email, String password) {
        _isLoading.setValue(true);

        firebaseManager.loginWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                if (user != null) {
                    // Force reload to fetch the latest email verification status
                    user.reload().addOnCompleteListener(task -> {
                        if (user.isEmailVerified()) {
                            // Valid user -> Sync profile to Firestore before allowing entry
                            syncUserWithFirestore(user);
                        } else {
                            _isLoading.setValue(false);
                            firebaseManager.logout(); // Instantly log out unverified users
                            _errorMessage.setValue("Account is not verified! Please check your email.");
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // ACCOUNT REGISTRATION
    // ============================================================
    public void register(String email, String password, String name) {
        _isLoading.setValue(true);

        firebaseManager.registerWithEmail(email, password, name, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                if (user != null) {
                    // Trigger email verification automatically upon registration
                    user.sendEmailVerification().addOnCompleteListener(task -> {
                        _isLoading.setValue(false);
                        firebaseManager.logout();
                        _infoMessage.setValue("Registration successful! Please check your email to verify.");
                    });
                }
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // GOOGLE SIGN IN
    // ============================================================
    public void loginWithGoogle(String idToken) {
        _isLoading.setValue(true);

        firebaseManager.loginWithGoogle(idToken, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String uid) {
                FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                // Sync profile to Firestore before allowing entry
                syncUserWithFirestore(user);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    // ============================================================
    // PASSWORD RESET
    // ============================================================
    public void resetPassword(String email) {
        _isLoading.setValue(true);

        firebaseManager.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    _isLoading.setValue(false);
                    _isResetMailSent.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(e.getMessage());
                });
    }

    // ============================================================
    // FIRESTORE SYNCHRONIZATION
    // ============================================================
    private void syncUserWithFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            _isAuthSuccess.postValue(true);
            _isLoading.postValue(false);
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", firebaseUser.getUid());
        userMap.put("email", firebaseUser.getEmail());

        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            userMap.put("displayName", firebaseUser.getDisplayName());
        }
        if (firebaseUser.getPhotoUrl() != null) {
            String originalUrl = firebaseUser.getPhotoUrl().toString();
            // Enhance Google avatar resolution by swapping URL parameters
            if (originalUrl.contains("s96-c")) {
                originalUrl = originalUrl.replace("s96-c", "s400-c");
            }
            userMap.put("avatarUrl", originalUrl);
        }

        // Note: Ideally handled by a UserRepository. Kept here to minimize structural breaking.
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .set(userMap, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    // Trigger UI navigation only after database sync is complete
                    _isAuthSuccess.postValue(true);
                    _isLoading.postValue(false);
                });
    }

    // Xóa lỗi để không bị nổ Toast nhiều lần
    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    // Reset lại trạng thái gửi mail
    public void clearResetMailStatus() {
        _isResetMailSent.setValue(false);
    }
}