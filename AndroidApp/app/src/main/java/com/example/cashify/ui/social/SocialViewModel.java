package com.example.cashify.ui.social;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SocialViewModel
 * Manages states and business logic for the Social Profile and Newsfeed interactions.
 */
public class SocialViewModel extends ViewModel {

    private final ApiService apiService = ApiClient.getClient().create(ApiService.class);

    // =========================================================================
    // STATE LIVEDATA
    // =========================================================================

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    private final MutableLiveData<DocumentSnapshot> _profile = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> getProfile() { return _profile; }

    private final MutableLiveData<Integer> _friendCount = new MutableLiveData<>(0);
    public LiveData<Integer> getFriendCount() { return _friendCount; }

    private final MutableLiveData<Boolean> isDeleteSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsDeleteSuccess() { return isDeleteSuccess; }

    // =========================================================================
    // DATA FETCHING (FIRESTORE)
    // =========================================================================

    public void loadProfile(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Monitor Profile Changes (Name, Avatar, Bio)
        db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) _profile.postValue(doc);
                });

        // 2. Monitor Friend Count
        db.collection("users").document(uid).collection("friends")
                .addSnapshotListener((snapshot, e) -> {
                    int count = (snapshot != null) ? snapshot.size() : 0;
                    _friendCount.postValue(count);
                });
    }

    // =========================================================================
    // ACTIONS (API)
    // =========================================================================

    public void deletePost(String postId, String token) {
        ApiClient.getClient().create(ApiService.class)
                .deletePost(token, new ApiDto.DeletePostRequest(postId))
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                        isDeleteSuccess.setValue(response.isSuccessful());
                    }

                    @Override
                    public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                        isDeleteSuccess.setValue(false);
                        _errorMessage.setValue("Failed to delete post: " + t.getMessage());
                    }
                });
    }

    // =========================================================================
    // STATE RESETS
    // =========================================================================

    public void resetActionStatus() {
        _errorMessage.setValue(null);
    }

    public void resetDeleteStatus() {
        isDeleteSuccess.setValue(false);
    }

    public void toggleLike(String postId, String token, boolean isLiked) {
        apiService.toggleLike(token, new ApiDto.LikeActionRequest(postId, isLiked)).enqueue(new Callback<Object>() {
            @Override public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {}
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }
}