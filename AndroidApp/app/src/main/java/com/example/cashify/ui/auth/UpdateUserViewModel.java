package com.example.cashify.ui.auth;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.repository.AuthRepositoryImpl;
import com.example.cashify.data.repository.IAuthRepository;
import com.example.cashify.data.repository.MediaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UpdateUserViewModel extends ViewModel {

    private static final String TAG = "UpdateUserViewModel";

    // Inject Repositories
    private final IAuthRepository authRepository = new AuthRepositoryImpl();
    private final MediaRepository mediaRepository = new MediaRepository();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }

    // ĐÃ FIX: Nhận trực tiếp 'File' từ UI, không nhận 'Context' và 'Uri' nữa
    public void updateProfile(String newName, File imageFile) {
        if (!authRepository.isLoggedIn()) {
            message.setValue("Authentication required.");
            return;
        }

        isLoading.setValue(true);

        if (imageFile != null) {
            // STEP 1: Delegate upload logic to Data Layer (MediaRepository)
            mediaRepository.uploadImage(imageFile, new MediaRepository.UploadCallback() {
                @Override
                public void onProgress(int percent) {
                    // Optional: Update progress UI
                }

                @Override
                public void onSuccess(String imageUrl) {
                    Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                    imageFile.delete(); // Clean up cache

                    // STEP 2: Update Firebase profile with the new secure URL
                    executeFirebaseUpdate(newName, Uri.parse(imageUrl));
                }

                @Override
                public void onFailure(String error) {
                    isLoading.postValue(false);
                    message.postValue("Media upload failed: " + error);
                    imageFile.delete(); // Clean up cache on failure
                }
            });
        } else {
            // No image selected, proceed to update name only
            executeFirebaseUpdate(newName, null);
        }
    }

    /**
     * Note: Ideally, this should be moved to a UserRepository.
     * Kept here temporarily to avoid breaking too many structural files at once.
     */
    private void executeFirebaseUpdate(String newName, Uri photoUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || ((newName == null || newName.trim().isEmpty()) && photoUri == null)) {
            isLoading.postValue(false);
            message.postValue("No changes detected or user logged out.");
            return;
        }

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
        Map<String, Object> firestoreUpdates = new HashMap<>();

        if (newName != null && !newName.trim().isEmpty()) {
            profileBuilder.setDisplayName(newName.trim());
            firestoreUpdates.put("displayName", newName.trim());
        }
        if (photoUri != null) {
            profileBuilder.setPhotoUri(photoUri);
            firestoreUpdates.put("avatarUrl", photoUri.toString());
        }

        // Sync updates across Firebase Auth and Firestore concurrently
        user.updateProfile(profileBuilder.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getUid())
                                .set(firestoreUpdates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    isLoading.postValue(false);
                                    message.postValue("Profile updated successfully.");
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.postValue(false);
                                    message.postValue("Profile updated partially. Database sync failed: " + e.getMessage());
                                });
                    } else {
                        isLoading.postValue(false);
                        message.postValue("Failed to update profile: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    public void clearMessage() {
        message.setValue(null);
    }
}