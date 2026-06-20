package com.example.cashify.ui.settings;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UpdateUserViewModel extends ViewModel {

    // =========================================================================
    // STATE MANAGEMENT LIVEDATA
    // =========================================================================
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _statusMessage = new MutableLiveData<>();
    public LiveData<String> statusMessage = _statusMessage;

    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> updateSuccess = _updateSuccess;

    // =========================================================================
    // 1. UPDATE DISPLAY NAME
    // - Synchronizes the display name across both Firestore and Firebase Auth Profile.
    // =========================================================================
    public void updateDisplayName(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || newName == null || newName.trim().isEmpty()) {
            _statusMessage.postValue("Invalid user or display name.");
            return;
        }

        _isLoading.postValue(true);
        String uid = user.getUid();

        // Step 1: Update Firestore Database
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("displayName", newName.trim())
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Sync with Firebase Auth Profile
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName.trim())
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                _isLoading.postValue(false);
                                if (task.isSuccessful()) {
                                    _updateSuccess.postValue(true);
                                    _statusMessage.postValue("Display name updated successfully.");
                                } else {
                                    _statusMessage.postValue("Failed to sync Auth profile.");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _statusMessage.postValue("Failed to update Firestore: " + e.getMessage());
                });
    }

    // =========================================================================
    // 2. UPDATE AVATAR (UPLOAD TO STORAGE)
    // - Note: It is highly recommended to compress the image Uri (e.g., via Luban)
    //   in the Activity/Fragment BEFORE passing it to this method to save bandwidth.
    // =========================================================================
    public void updateAvatar(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || imageUri == null) {
            _statusMessage.postValue("Invalid user or image data.");
            return;
        }

        _isLoading.postValue(true);
        String uid = user.getUid();

        // Define Storage path: avatars/{uid}.jpg
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("avatars/" + uid + ".jpg");

        // Step 1: Upload Image to Firebase Storage
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Step 2: Retrieve the public Download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Step 3: Update URL in Firestore
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update("avatarUrl", downloadUrl)
                                .addOnSuccessListener(aVoid -> {

                                    // Step 4: Sync with Firebase Auth Profile
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setPhotoUri(uri)
                                            .build();
                                    user.updateProfile(profileUpdates);

                                    _isLoading.postValue(false);
                                    _updateSuccess.postValue(true);
                                    _statusMessage.postValue("Avatar updated successfully.");
                                })
                                .addOnFailureListener(e -> {
                                    _isLoading.postValue(false);
                                    _statusMessage.postValue("Failed to update Firestore record.");
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _statusMessage.postValue("Failed to upload image: " + e.getMessage());
                });
    }

    // =========================================================================
    // 3. CHANGE PASSWORD
    // - Requires Re-authentication with the current password for security.
    // - Strictly modifies the Firebase Auth credential, NEVER stores passwords in Firestore.
    // =========================================================================
    public void changePassword(String oldPass, String newPass) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            _statusMessage.postValue("Authentication error. Please log in again.");
            return;
        }

        if (newPass == null || newPass.length() < 6) {
            _statusMessage.postValue("New password must be at least 6 characters.");
            return;
        }

        _isLoading.postValue(true);

        // Step 1: Re-authenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Update to new password
                    user.updatePassword(newPass)
                            .addOnSuccessListener(aVoid1 -> {
                                _isLoading.postValue(false);
                                _updateSuccess.postValue(true);
                                _statusMessage.postValue("Password changed successfully.");
                            })
                            .addOnFailureListener(e -> {
                                _isLoading.postValue(false);
                                _statusMessage.postValue("Failed to update password: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    _isLoading.postValue(false);
                    _statusMessage.postValue("Incorrect old password. Please try again.");
                });
    }
}