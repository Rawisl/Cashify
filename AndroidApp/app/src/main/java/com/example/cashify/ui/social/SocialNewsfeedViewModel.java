package com.example.cashify.ui.social;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.TimeFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SocialNewsfeedViewModel extends ViewModel {

    private final ApiService apiService = ApiClient.getClient().create(ApiService.class);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final int FEED_PAGE_SIZE = 10;

    private final java.util.HashMap<String, Boolean> likedCache = new java.util.HashMap<>();
    // =========================================================================
    // STATE LIVEDATA
    // =========================================================================
    private final MutableLiveData<Boolean> _isDeleteSuccess = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsDeleteSuccess() {
        return _isDeleteSuccess;
    }

    private final MutableLiveData<List<DocumentSnapshot>> _topUsers = new MutableLiveData<>();

    public LiveData<List<DocumentSnapshot>> getTopUsers() {
        return _topUsers;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isLoadingMore = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLoadingMore() {
        return _isLoadingMore;
    }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    private final MutableLiveData<DocumentSnapshot> _profile = new MutableLiveData<>();

    public LiveData<DocumentSnapshot> getProfile() {
        return _profile;
    }

    private final MutableLiveData<List<FeedItem>> _feedItems = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<FeedItem>> getFeedItems() {
        return _feedItems;
    }

    private final MutableLiveData<Boolean> _isFeedEmpty = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsFeedEmpty() {
        return _isFeedEmpty;
    }

    private final MutableLiveData<Boolean> _isLastPage = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLastPage() {
        return _isLastPage;
    }

    private DocumentSnapshot lastVisibleFeedDoc = null;
    private final List<String> cachedFriendIds = new ArrayList<>();
    private boolean isAdmin = false;
    private ListenerRegistration realTimeListener;

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
        loadTopUsersForDecoration();
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(_isLoading.getValue()) || Boolean.TRUE.equals(_isLoadingMore.getValue()) || Boolean.TRUE.equals(_isLastPage.getValue()))
            return;
        _isLoadingMore.setValue(true);
        loadFeedPageFromFirebase(false);
    }

    private void loadFriendListAndFeed(boolean isRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
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
                        for (DocumentSnapshot doc : snapshots.getDocuments())
                            cachedFriendIds.add(doc.getId());
                        loadFeedPageFromFirebase(isRefresh);
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

        if (realTimeListener != null && isRefresh) {
            realTimeListener.remove();
        }

        realTimeListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                _isLoading.setValue(false);
                _isLoadingMore.setValue(false);
                return;
            }

            if (!snapshots.isEmpty())
                lastVisibleFeedDoc = snapshots.getDocuments().get(snapshots.size() - 1);
            _isLastPage.setValue(snapshots.size() < FEED_PAGE_SIZE);

            List<FeedItem> parsedItems = new ArrayList<>();
            List<Task<DocumentSnapshot>> likeTasks = new ArrayList<>();
            String currentUid = user.getUid();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                FeedItem item = mapFirebaseDocToFeedItem(doc);
                if (item != null) {
                    parsedItems.add(item);

                    if (!likedCache.containsKey(item.getId())) {
                        Task<DocumentSnapshot> task = db.collection("posts").document(item.getId())
                                .collection("likes").document(currentUid).get();

                        task.addOnSuccessListener(likeDoc -> {
                            likedCache.put(item.getId(), likeDoc.exists());
                        });
                        likeTasks.add(task);
                    }
                }
            }

            Tasks.whenAllComplete(likeTasks).addOnCompleteListener(t -> {
                for (FeedItem item : parsedItems) {
                    item.setLiked(Boolean.TRUE.equals(likedCache.get(item.getId())));
                }

                if (isRefresh) {
                    _feedItems.setValue(parsedItems);
                } else {
                    List<FeedItem> current = _feedItems.getValue();
                    if (current == null) current = new ArrayList<>();
                    Set<String> existingIds = new HashSet<>();
                    for (FeedItem i : current) existingIds.add(i.getId());

                    List<FeedItem> updated = new ArrayList<>(current);
                    for (FeedItem i : parsedItems) {
                        if (!existingIds.contains(i.getId())) updated.add(i);
                    }
                    _feedItems.setValue(updated);
                }

                _isFeedEmpty.setValue(_feedItems.getValue() == null || _feedItems.getValue().isEmpty());
                _isLoading.setValue(false);
                _isLoadingMore.setValue(false);
            });
        });
    }

    public void syncSinglePost(String postId, String token) {
        if (token == null || token.isEmpty()) return;
        apiService.getPostDetail(postId, token).enqueue(new Callback<ApiDto.SocialPostDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiDto.SocialPostDetailResponse> call, @NonNull Response<ApiDto.SocialPostDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiDto.SocialPostDetailResponse data = response.body();

                    if (data.likedByMe) likedCache.put(postId, data.likedByMe);
                    else likedCache.remove(postId);

                    List<FeedItem> current = _feedItems.getValue();
                    if (current != null) {
                        List<FeedItem> newList = new ArrayList<>(current);
                        for (int i = 0; i < newList.size(); i++) {
                            if (newList.get(i).getId().equals(postId)) {
                                FeedItem item = newList.get(i);
                                item.setLikeCount(data.likeCount);
                                item.setCommentCount(data.commentCount);
                                item.setLiked(data.likedByMe);
                                newList.set(i, item);
                                _feedItems.setValue(newList);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiDto.SocialPostDetailResponse> call, @NonNull Throwable t) {
            }
        });
    }

    public void toggleLike(String postId, String token, boolean isLiked) {
        if (isLiked) likedCache.put(postId, isLiked);
        else likedCache.remove(postId);

        List<FeedItem> current = _feedItems.getValue();
        if (current != null) {
            List<FeedItem> newList = new ArrayList<>(current);
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i).getId().equals(postId)) {
                    FeedItem item = newList.get(i);
                    item.setLiked(isLiked);
                    int currentCount = item.getLikeCount();
                    item.setLikeCount(isLiked ? currentCount + 1 : Math.max(0, currentCount - 1));
                    newList.set(i, item);
                    _feedItems.setValue(newList);
                    break;
                }
            }
        }

        apiService.toggleLike(token, new ApiDto.LikeActionRequest(postId, isLiked)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (!response.isSuccessful()) syncSinglePost(postId, token);
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                syncSinglePost(postId, token);
            }
        });
    }

    public void loadTopUsersForDecoration() {
        db.collection("users").limit(5).get()
                .addOnSuccessListener(querySnapshot -> {
                    _topUsers.postValue(querySnapshot.getDocuments());
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

            // Trả về đúng giá trị mặc định giống PostDetailActivity
            String mTitle = title.isEmpty() ? "Milestone" : title;
            String mDescription = !content.isEmpty() ? content : "Reached a new goal";
            String mMonth = "";
            String mAmount = ""; //Nếu không có data, Adapter sẽ tự View.GONE cái goalPanel
            String mIcon = "🏆";
            int mProgress = 0;

            // Chỉ giải mã và áp dụng thông số khi có dữ liệu milestoneData từ Firebase
            if (!milestoneData.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(milestoneData);
                    mIcon = json.optString("iconText", "🏆");
                    mTitle = json.optString("title", mTitle);
                    mMonth = json.optString("month", "");
                    mAmount = json.optString("amount", "");
                    mProgress = json.optInt("progress", 0);
                    mDescription = !content.isEmpty() ? content : "";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Đóng gói vào Constructor
            item = new FeedItem.MilestonePost(
                    id,
                    userId,
                    authorName,
                    TimeFormatter.format(timestamp),
                    mTitle,
                    mDescription,
                    mMonth,
                    mAmount,
                    mIcon,
                    mProgress,
                    mDescription.length() > 120,
                    milestoneData,
                    authorAvatarUrl,
                    initials(authorName)
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
            item.setLiked(false);
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

    public void resetDeleteStatus() {
        _isDeleteSuccess.setValue(false);
    }

    public void deletePost(String postId, String token) {
        apiService.deletePost(token, new ApiDto.DeletePostRequest(postId)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {
                    _isDeleteSuccess.setValue(true);

                    List<FeedItem> current = _feedItems.getValue();
                    if (current != null) {
                        List<FeedItem> newList = new ArrayList<>(current);
                        newList.removeIf(item -> item.getId().equals(postId));
                        _feedItems.setValue(newList);
                    }
                } else {
                    _isDeleteSuccess.setValue(false);
                    _errorMessage.setValue("Failed to delete post: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                _isDeleteSuccess.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (realTimeListener != null) realTimeListener.remove();
    }
}