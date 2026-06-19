package com.example.cashify.ui.social;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.TimeFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SocialProfileViewModel extends ViewModel {

    private final ApiService apiService = ApiClient.getClient().create(ApiService.class);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration achievementsRegistration; // Quản lý vòng đời listener

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

    private final MutableLiveData<List<FeedItem>> _wallPosts = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<FeedItem>> getWallPosts() { return _wallPosts; }

    private final MutableLiveData<List<ProfileAchievementAdapter.BadgeMeta>> _achievements = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ProfileAchievementAdapter.BadgeMeta>> getAchievements() { return _achievements; }

    private final MutableLiveData<Boolean> _isDeleteSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsDeleteSuccess() { return _isDeleteSuccess; }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    public void loadProfileData(String uid) {
        // Load Profile Info
        db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) _profile.postValue(doc);
        });

        // Load Friend Count
        db.collection("users").document(uid).collection("friends")
                .addSnapshotListener((snapshot, e) -> {
                    int count = (snapshot != null) ? snapshot.size() : 0;
                    _friendCount.postValue(count);
                });
    }

    //Lắng nghe Achievements
    public void loadAchievements(String uid) {
        if (uid == null || uid.isEmpty()) return;

        if (achievementsRegistration != null) achievementsRegistration.remove();

        achievementsRegistration = db.collection("users").document(uid).collection("shared_achievements")
                .orderBy("sharedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        _errorMessage.postValue("Failed to load achievements");
                        return;
                    }

                    List<ProfileAchievementAdapter.BadgeMeta> badges = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        badges.add(getBadgeMetaFromId(doc.getId()));
                    }
                    _achievements.postValue(badges);
                });
    }

    public void loadWallPosts(String uid, String token) {
        _isLoading.setValue(true);
        apiService.getWall(token, uid, 30, 0).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(@NonNull Call<List<Object>> call, @NonNull Response<List<Object>> response) {
                _isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<FeedItem> posts = new ArrayList<>();
                    for (Object obj : response.body()) {
                        if (obj instanceof Map) posts.add(mapPostFromMap((Map<String, Object>) obj));
                    }
                    _wallPosts.setValue(posts);
                } else {
                    _errorMessage.setValue("Failed to load wall posts");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void syncSinglePost(String postId) {
        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getData() != null) {
                FeedItem updatedItem = mapPostFromMap(doc.getData());
                List<FeedItem> current = _wallPosts.getValue();
                if (current != null) {
                    List<FeedItem> newList = new ArrayList<>(current);
                    for (int i = 0; i < newList.size(); i++) {
                        if (newList.get(i).getId().equals(postId)) {
                            newList.set(i, updatedItem);
                            _wallPosts.setValue(newList);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void deletePost(String postId, String token) {
        apiService.deletePost(token, new ApiDto.DeletePostRequest(postId)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {
                    _isDeleteSuccess.setValue(true);
                    List<FeedItem> current = _wallPosts.getValue();
                    if (current != null) {
                        List<FeedItem> newList = new ArrayList<>(current);
                        newList.removeIf(item -> item.getId().equals(postId));
                        _wallPosts.setValue(newList);
                    }
                } else _isDeleteSuccess.setValue(false);
            }
            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                _isDeleteSuccess.setValue(false);
                _errorMessage.setValue("Delete failed: " + t.getMessage());
            }
        });
    }

    public void toggleLike(String postId, String token, boolean isLiked) {
        apiService.toggleLike(token, new ApiDto.LikeActionRequest(postId, isLiked)).enqueue(new Callback<Object>() {
            @Override public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {}
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }

    public void resetDeleteStatus() {
        _isDeleteSuccess.setValue(false);
    }

    // Dọn dẹp listener khi ViewModel bị hủy
    @Override
    protected void onCleared() {
        super.onCleared();
        if (achievementsRegistration != null) achievementsRegistration.remove();
    }

    // =========================================================================
    // MAPPER
    // =========================================================================
    private FeedItem mapPostFromMap(Map<String, Object> map) {
        String id = str(map, "postId");
        String title = str(map, "title");
        String userId = str(map, "userId");
        String content = str(map, "content");
        String imageUrl = str(map, "imageUrl");
        String type = str(map, "type").toLowerCase(Locale.US);
        long timestamp = num(map, "timestamp");
        String name = str(map, "authorName");
        String avatarUrl = str(map, "authorAvatarUrl");

        boolean hasImage = !imageUrl.isEmpty();
        boolean expandable = content.length() > 120;
        FeedItem item = null;

        if (type.contains("milestone") || type.contains("achievement")) {
            String milestoneData = "";
            Object mdObj = map.get("milestoneData");
            if (mdObj instanceof String) {
                milestoneData = (String) mdObj;
            } else if (mdObj instanceof Map) {
                milestoneData = new JSONObject((Map<?, ?>) mdObj).toString();
            }

            String milestoneTitle = "New Milestone";
            String description = content;
            String amount = "";
            String iconText = "🏆";
            String month = "Achievement";
            int progress = 0;

            if (!milestoneData.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(milestoneData);
                    iconText = json.optString("iconText", "🏆");
                    milestoneTitle = json.optString("title", "New Milestone");
                    amount = json.optString("amount", "");
                    month = json.optString("month", "Achievement");
                    progress = json.optInt("progress", 0);

                    String jsonDesc = json.optString("description", "");
                    if (!jsonDesc.isEmpty() && content.isEmpty()) description = jsonDesc;
                } catch (Exception ignored) {}
            }

            item = new FeedItem.MilestonePost(
                    id, userId, name.isEmpty() ? "You" : name, TimeFormatter.format(timestamp),
                    milestoneTitle, description, month, amount, iconText, progress,
                    description.length() > 120, milestoneData, avatarUrl, initials(name)
            );
        } else {
            item = new FeedItem.NormalPost(
                    id, userId, name.isEmpty() ? "You" : name, TimeFormatter.format(timestamp),
                    title, content, hasImage, imageUrl, initials(name), expandable, avatarUrl
            );
        }

        if (item != null) {
            item.setLikeCount((int) num(map, "likeCount"));
            item.setCommentCount((int) num(map, "commentCount"));
            item.setLiked(Boolean.TRUE.equals(map.get("isLiked")));
        }

        return item;
    }

    private ProfileAchievementAdapter.BadgeMeta getBadgeMetaFromId(String achId) {
        if (achId == null) return new ProfileAchievementAdapter.BadgeMeta("Mystery Badge", "🎁", "#E2ECFF");
        if (achId.startsWith("ach_trans_")) return new ProfileAchievementAdapter.BadgeMeta("Hardworking Bee", "🐝", "#FFFDF7");
        if (achId.startsWith("ach_streak_")) return new ProfileAchievementAdapter.BadgeMeta("Iron Discipline", "🔥", "#FFE1E5");
        if (achId.startsWith("ach_night_owl")) return new ProfileAchievementAdapter.BadgeMeta("Night Owl", "🦉", "#E8E2FF");
        if (achId.startsWith("ach_big_spender")) return new ProfileAchievementAdapter.BadgeMeta("Big Whale", "🐋", "#E2ECFF");
        if (achId.startsWith("recap_surplus_")) return new ProfileAchievementAdapter.BadgeMeta("Healthy Finances", "💰", "#E2F5DA");
        if (achId.startsWith("ws_the_carry_")) return new ProfileAchievementAdapter.BadgeMeta("The Carry", "🦸‍♂️", "#FFF0C9");
        if (achId.startsWith("ws_biggest_spender_")) return new ProfileAchievementAdapter.BadgeMeta("Biggest Spender", "🛍️", "#FFE1E5");
        return new ProfileAchievementAdapter.BadgeMeta("Mystery Badge", "🎁", "#E2ECFF");
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key); return v instanceof String ? (String) v : "";
    }
    private long num(Map<String, Object> map, String key) {
        Object v = map.get(key); return v instanceof Number ? ((Number) v).longValue() : 0L;
    }
    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        String f = parts[0].substring(0, 1);
        String s = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (f + s).toUpperCase(Locale.getDefault());
    }
}