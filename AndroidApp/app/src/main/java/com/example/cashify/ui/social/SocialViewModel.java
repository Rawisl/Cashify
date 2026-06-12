package com.example.cashify.ui.social;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * SocialViewModel — Giống WorkspaceViewModel nhưng cho Social.
 * Sau này sẽ thêm LiveData cho posts, friends, profile data.
 */
public class SocialViewModel extends ViewModel {

    // ── Trạng thái ────────────────────────────────────────────────
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void resetActionStatus() {
        _errorMessage.setValue(null);
    }

    // ── Profile ───────────────────────────────────────────────────
    private final MutableLiveData<com.google.firebase.firestore.DocumentSnapshot> _profile
            = new MutableLiveData<>();
    public LiveData<com.google.firebase.firestore.DocumentSnapshot> getProfile() { return _profile; }

    // ── Friend count ──────────────────────────────────────────────
    private final MutableLiveData<Integer> _friendCount = new MutableLiveData<>(0);
    public LiveData<Integer> getFriendCount() { return _friendCount; }

    // ── Load ──────────────────────────────────────────────────────
    public void loadProfile(String uid) {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 1. Load tên + avatar
        db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) _profile.postValue(doc);
                });

        // 2. Đếm bạn bè — collection "friends" nằm dưới user
        db.collection("users").document(uid).collection("friends")
                .addSnapshotListener((snapshot, e) -> {
                    int count = (snapshot != null) ? snapshot.size() : 0;
                    _friendCount.postValue(count);
                });
    }
    // Thêm LiveData để lắng nghe trạng thái xóa
    private final androidx.lifecycle.MutableLiveData<Boolean> isDeleteSuccess = new androidx.lifecycle.MutableLiveData<>();
    public androidx.lifecycle.LiveData<Boolean> getIsDeleteSuccess() { return isDeleteSuccess; }

    // Hàm gọi API xóa bài
    public void deletePost(String postId, String token) {
        com.example.cashify.utils.ApiClient.getClient().create(com.example.cashify.utils.ApiService.class)
                .deletePost(token, new com.example.cashify.utils.ApiService.DeletePostRequest(postId))
                .enqueue(new retrofit2.Callback<Object>() {
                    @Override
                    public void onResponse(@androidx.annotation.NonNull retrofit2.Call<Object> call, @androidx.annotation.NonNull retrofit2.Response<Object> response) {
                        if (response.isSuccessful()) {
                            isDeleteSuccess.setValue(true);
                        } else {
                            isDeleteSuccess.setValue(false);
                        }
                    }
                    @Override
                    public void onFailure(@androidx.annotation.NonNull retrofit2.Call<Object> call, @androidx.annotation.NonNull Throwable t) {
                        isDeleteSuccess.setValue(false);
                    }
                });
    }
    // ── TODO: Thêm LiveData cho newsfeed posts ─────────────────────
    // private final MutableLiveData<List<Post>> _posts = new MutableLiveData<>();
    // public LiveData<List<Post>> getPosts() { return _posts; }

    // ── TODO: Thêm LiveData cho profile ───────────────────────────
    // private final MutableLiveData<User> _profile = new MutableLiveData<>();
    // public LiveData<User> getProfile() { return _profile; }
}