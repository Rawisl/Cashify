package com.example.cashify.ui.social;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.ui.social.FeedItem;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.TimeFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SocialNewsfeedViewModel extends ViewModel {

    private final ApiService apiService = ApiClient.getClient().create(ApiService.class);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final int FEED_PAGE_SIZE = 10;

    // =========================================================================
    // STATE LIVEDATA
    // =========================================================================

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isLoadingMore = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoadingMore() { return _isLoadingMore; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    private final MutableLiveData<DocumentSnapshot> _profile = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> getProfile() { return _profile; }

    private final MutableLiveData<Boolean> isDeleteSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsDeleteSuccess() { return isDeleteSuccess; }

    // DỮ LIỆU CỐT LÕI (DANH SÁCH BÀI VIẾT)
    private final MutableLiveData<List<FeedItem>> _feedItems = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<FeedItem>> getFeedItems() { return _feedItems; }

    private final MutableLiveData<Boolean> _isFeedEmpty = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsFeedEmpty() { return _isFeedEmpty; }

    private final MutableLiveData<Boolean> _isLastPage = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLastPage() { return _isLastPage; }

    // =========================================================================
    // PAGINATION CURSORS
    // =========================================================================
    private DocumentSnapshot lastVisibleFeedDoc = null;
    private final List<String> cachedFriendIds = new ArrayList<>();
    private boolean isAdmin = false;

    // =========================================================================
    // LOGIC NGHIỆP VỤ (FIRESTORE & API)
    // =========================================================================

    public void loadProfile(String uid) {
        db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                isAdmin = "ADMIN".equals(doc.getString("role"));
                _profile.postValue(doc);
            }
        });
    }

    public void refreshFeed() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) return;
        _isLoading.setValue(true);
        _isLastPage.setValue(false);
        lastVisibleFeedDoc = null;
        cachedFriendIds.clear();
        loadFriendListAndFeed(true);
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(_isLoading.getValue()) ||
                Boolean.TRUE.equals(_isLoadingMore.getValue()) ||
                Boolean.TRUE.equals(_isLastPage.getValue()) ||
                _feedItems.getValue() == null || _feedItems.getValue().isEmpty()) {
            return;
        }
        _isLoadingMore.setValue(true);
        loadFeedPageFromFirebase(false);
    }

    private void loadFriendListAndFeed(boolean isRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("Authentication required");
            _isLoading.setValue(false);
            return;
        }

        if (isAdmin) {
            cachedFriendIds.clear();
            loadFeedPageFromFirebase(isRefresh);
        } else {
            db.collection("users").document(user.getUid()).collection("friends").get()
                    .addOnSuccessListener(snapshots -> {
                        cachedFriendIds.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) cachedFriendIds.add(doc.getId());
                        loadFeedPageFromFirebase(isRefresh);
                    })
                    .addOnFailureListener(e -> {
                        _isLoading.setValue(false);
                        _errorMessage.setValue("Failed to load friends");
                    });
        }
    }

    private void loadFeedPageFromFirebase(boolean isRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Query query = db.collection("posts");
        if (!isAdmin) {
            List<String> allUserIds = new ArrayList<>(cachedFriendIds);
            allUserIds.add(user.getUid());
            if (allUserIds.size() > 30) allUserIds = allUserIds.subList(0, 30);
            query = query.whereIn("userId", allUserIds);
        }

        query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(FEED_PAGE_SIZE);
        if (!isRefresh && lastVisibleFeedDoc != null) {
            query = query.startAfter(lastVisibleFeedDoc);
        }

        query.get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) lastVisibleFeedDoc = snapshots.getDocuments().get(snapshots.size() - 1);

            List<FeedItem> newItems = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                FeedItem item = mapFirebaseDocToFeedItem(doc);
                if (item != null) newItems.add(item);
            }

            _isLastPage.setValue(snapshots.size() < FEED_PAGE_SIZE);

            if (isRefresh) {
                _feedItems.setValue(newItems);
            } else {
                List<FeedItem> current = _feedItems.getValue();
                if (current == null) current = new ArrayList<>();
                Set<String> existingIds = new HashSet<>();
                for (FeedItem i : current) existingIds.add(i.getId());

                List<FeedItem> updated = new ArrayList<>(current);
                for (FeedItem i : newItems) {
                    if (!existingIds.contains(i.getId())) updated.add(i);
                }
                _feedItems.setValue(updated);
            }

            _isFeedEmpty.setValue(_feedItems.getValue() == null || _feedItems.getValue().isEmpty());
            _isLoading.setValue(false);
            _isLoadingMore.setValue(false);

        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _isLoadingMore.setValue(false);
            _errorMessage.setValue("Feed query error: " + e.getMessage());
        });
    }

    // TÍNH NĂNG ĐẶC BIỆT: Âm thầm Update 1 bài viết để đồng bộ Like/Comment từ màn Detail về Newsfeed
    public void syncSinglePost(String postId) {
        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                FeedItem updatedItem = mapFirebaseDocToFeedItem(doc);
                List<FeedItem> current = _feedItems.getValue();
                if (current != null && updatedItem != null) {
                    List<FeedItem> newList = new ArrayList<>(current);
                    for (int i = 0; i < newList.size(); i++) {
                        if (newList.get(i).getId().equals(postId)) {
                            newList.set(i, updatedItem);
                            _feedItems.setValue(newList);
                            break;
                        }
                    }
                }
            }
        });
    }

    private FeedItem mapFirebaseDocToFeedItem(DocumentSnapshot doc) {
        String id = doc.getId();
        String title = doc.getString("title") != null ? doc.getString("title") : "";
        String content = doc.getString("content") != null ? doc.getString("content") : "";
        String imageUrl = doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "";
        String type = doc.getString("type") != null ? doc.getString("type") : "normal";
        Long timestamp = doc.getLong("timestamp");
        String userId = doc.getString("userId");
        String authorName = doc.getString("authorName");
        String authorAvatarUrl = doc.getString("authorAvatarUrl") != null ? doc.getString("authorAvatarUrl") : "";
        Long likeCount = doc.getLong("likeCount");
        Long commentCount = doc.getLong("commentCount");

        if (timestamp == null) timestamp = 0L;
        if (authorName == null) authorName = "Cashify User";

        boolean hasImage = !imageUrl.isEmpty();
        boolean expandable = content.length() > 120;
        FeedItem item = null;

        if (type.toLowerCase().contains("milestone")) {
            String milestoneData = doc.getString("milestoneData") != null ? doc.getString("milestoneData") : "";
            String milestoneTitle = "New Milestone";
            String milestoneDescription = "";
            String amountText = "";
            String iconText = "🏆";
            int progressValue = 0;

            if (!milestoneData.trim().isEmpty()) {
                try {
                    JSONObject json = new JSONObject(milestoneData);
                    iconText = json.optString("iconText", "🏆");
                    milestoneTitle = json.optString("title", "New Milestone");
                    milestoneDescription = json.optString("description", "");
                    amountText = json.optString("amount", "");
                    progressValue = json.optInt("progress", 0);
                } catch (Exception ignored) {}
            }

            item = new FeedItem.MilestonePost(
                    id, userId, authorName, TimeFormatter.format(timestamp),
                    milestoneTitle, !content.isEmpty() ? content : milestoneDescription,
                    "Milestone", amountText, iconText, progressValue,
                    (!content.isEmpty() ? content : milestoneDescription).length() > 120,
                    milestoneData, authorAvatarUrl, initials(authorName)
            );
        } else {
            item = new FeedItem.NormalPost(
                    id, userId, authorName, TimeFormatter.format(timestamp),
                    title, content, hasImage, imageUrl, initials(authorName), expandable, authorAvatarUrl
            );
        }

        if (item != null) {
            item.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
            item.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
        }
        return item;
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        String f = parts[0].substring(0, 1);
        String s = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (f + s).toUpperCase(Locale.getDefault());
    }

    public void deletePost(String postId, String token) {
        ApiClient.getClient().create(ApiService.class)
                .deletePost(token, new ApiDto.DeletePostRequest(postId))
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                        if (response.isSuccessful()) {
                            isDeleteSuccess.setValue(true);
                            // Xóa bài viết khỏi Feed trên UI ngay lập tức
                            List<FeedItem> current = _feedItems.getValue();
                            if (current != null) {
                                List<FeedItem> newList = new ArrayList<>(current);
                                newList.removeIf(item -> item.getId().equals(postId));
                                _feedItems.setValue(newList);
                            }
                        } else {
                            isDeleteSuccess.setValue(false);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                        isDeleteSuccess.setValue(false);
                        _errorMessage.setValue("Failed to delete post: " + t.getMessage());
                    }
                });
    }

    public void toggleLike(String postId, String token, boolean isLiked) {
        apiService.toggleLike(token, new ApiDto.LikeActionRequest(postId, isLiked)).enqueue(new Callback<Object>() {
            @Override public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {}
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }
}