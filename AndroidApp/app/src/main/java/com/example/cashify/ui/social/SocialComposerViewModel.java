package com.example.cashify.ui.social;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.data.repository.MediaRepository;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.UploadNotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class SocialComposerViewModel extends AndroidViewModel {

    private final MediaRepository mediaRepository = new MediaRepository();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> postEvent = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<List<ApiDto.AchievementSuggestion>> achievements = new MutableLiveData<>();
    private final MutableLiveData<DocumentSnapshot> userProfile = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getPostEvent() { return postEvent; }
    public LiveData<String> getErrorEvent() { return errorEvent; }
    public LiveData<List<ApiDto.AchievementSuggestion>> getAchievements() { return achievements; }
    public LiveData<DocumentSnapshot> getUserProfile() { return userProfile; }

    public SocialComposerViewModel(@NonNull Application application) {
        super(application);
    }

    public void clearPostEvent() { postEvent.setValue(null); }
    public void clearErrorEvent() { errorEvent.setValue(null); }

    public void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userProfile::postValue);
    }

    public void fetchAvailableAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = "Bearer " + tokenResult.getToken();
            ApiClient.getClient().create(ApiService.class).getAvailableAchievements(token).enqueue(new retrofit2.Callback<List<ApiDto.AchievementSuggestion>>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<List<ApiDto.AchievementSuggestion>> call, @NonNull retrofit2.Response<List<ApiDto.AchievementSuggestion>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        achievements.postValue(response.body());
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<List<ApiDto.AchievementSuggestion>> call, @NonNull Throwable t) {
                    // Fail silently, no need to interrupt UI for achievements
                }
            });
        });
    }

    // ĐÃ THÊM THAM SỐ `title` VÀO ĐÂY ĐỂ ĐỒNG BỘ VỚI FRAGMENT
    public void submitPost(String editPostId, String title, String content, String type,
                           Uri imageUri, String milestoneData, String audienceParam) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            errorEvent.setValue("Authentication required!");
            return;
        }

        isLoading.setValue(true);

        user.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = "Bearer " + tokenResult.getToken();

            // 1. Edit existing post
            if (editPostId != null) {
                editExistingPost(token, editPostId, title, content, audienceParam);
                return;
            }

            // 2. Create new post WITH image
            if (imageUri != null) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    File imageFile = getFileFromUri(getApplication(), imageUri);

                    if (imageFile == null) {
                        isLoading.postValue(false);
                        errorEvent.postValue("Failed to process image.");
                        return;
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        UploadNotificationHelper notif = new UploadNotificationHelper(getApplication());
                        mediaRepository.uploadImage(imageFile, new MediaRepository.UploadCallback() {
                            @Override
                            public void onProgress(int percent) {
                                notif.update(percent);
                            }

                            @Override
                            public void onSuccess(String imageUrl) {
                                notif.done();
                                createNewPost(token, title, content, type, imageUrl, milestoneData, audienceParam);
                                imageFile.delete();
                            }

                            @Override
                            public void onFailure(String error) {
                                notif.error();
                                isLoading.postValue(false);
                                errorEvent.postValue("Media upload failed: " + error);
                                imageFile.delete();
                            }
                        });
                    });
                });
            } else {
                // 3. Create new post WITHOUT image
                createNewPost(token, title, content, type, "", milestoneData, audienceParam);
            }

        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            errorEvent.setValue("Auth verification failed.");
        });
    }

    private void editExistingPost(String token, String postId, String title, String content, String audience) {
        ApiDto.EditPostRequest req = new ApiDto.EditPostRequest();
        req.PostId = postId;
        req.Title = title;
        req.NewContent = content;
        req.Audience = audience;

        ApiClient.getClient().create(ApiService.class).editPost(token, req).enqueue(new retrofit2.Callback<Object>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) postEvent.postValue("Post updated successfully!");
                else errorEvent.postValue("Server error during update.");
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<Object> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                errorEvent.postValue("Network error: " + t.getMessage());
            }
        });
    }

    private void createNewPost(String token, String title, String content, String type, String imageUrl, String milestoneData, String audience) {
        ApiDto.CreatePostRequest req = new ApiDto.CreatePostRequest(title, content, type, imageUrl, milestoneData, audience);

        ApiClient.getClient().create(ApiService.class).createPost(token, req).enqueue(new retrofit2.Callback<Object>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) postEvent.postValue("Post published successfully!");
                else errorEvent.postValue("Failed to publish: " + response.code());
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<Object> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                errorEvent.postValue("Network error: " + t.getMessage());
            }
        });
    }

    private File getFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = new File(context.getCacheDir(), "upload_img_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }
}