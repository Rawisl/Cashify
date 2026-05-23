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

    // ── TODO: Thêm LiveData cho newsfeed posts ─────────────────────
    // private final MutableLiveData<List<Post>> _posts = new MutableLiveData<>();
    // public LiveData<List<Post>> getPosts() { return _posts; }

    // ── TODO: Thêm LiveData cho profile ───────────────────────────
    // private final MutableLiveData<User> _profile = new MutableLiveData<>();
    // public LiveData<User> getProfile() { return _profile; }
}