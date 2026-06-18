package com.example.cashify.ui.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.feed.CommunityFeedAdapter;
import com.example.cashify.ui.feed.FeedItem;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.ui.notifications.InvitationsActivity;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SocialProfileFragment extends Fragment {

    private static final String TAG = "SocialProfileFragment";
    private static final String DEFAULT_USERNAME = "Cashify User";

    private ImageView imgAvatar;
    private TextView tvDisplayName, tvBio, tvFriendCount, tvTrophyCount;
    private TextView tvPostCount, tvJoinedDate, tvPinnedAchievement, tvStreakCount;
    private RecyclerView rvMyPosts, rvAchievementsProfile;
    private View layoutEmptyState;

    private SocialViewModel socialViewModel;
    private CommunityFeedAdapter myPostsAdapter;
    private ProfileAchievementAdapter badgeAdapter;
    private String currentUserId = "";
    private ListenerRegistration achievementsRegistration;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        initToolbar(view);
        initViewModel();
        setupActions(view);
        setupRecyclerView();
        observeViewModel();

        loadMyPosts();
        listenToAchievementsRealtime();
    }

    @Override
    public void onDestroyView() {
        if (achievementsRegistration != null) {
            achievementsRegistration.remove();
            achievementsRegistration = null;
        }
        super.onDestroyView();
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private void bindViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvBio = view.findViewById(R.id.tvBio);
        tvFriendCount = view.findViewById(R.id.tvFriendCount);
        tvTrophyCount = view.findViewById(R.id.tvTrophyCount);
        tvPostCount = view.findViewById(R.id.tvPostCount);
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate);
        tvPinnedAchievement = view.findViewById(R.id.tvPinnedAchievement);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        rvAchievementsProfile = view.findViewById(R.id.rvAchievementsProfile);
    }

    private void initToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialProfile);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() == null) return;
            androidx.drawerlayout.widget.DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });
    }

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            socialViewModel.loadProfile(currentUserId);
        }
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        view.findViewById(R.id.btnShareProfile).setOnClickListener(v -> shareProfile());
        view.findViewById(R.id.txtProfileLink).setOnClickListener(v -> copyProfileLink());
        view.findViewById(R.id.btnProfileNotifications).setOnClickListener(v -> startActivity(new Intent(requireContext(), InvitationsActivity.class)));

        View.OnClickListener createPostListener = v -> openCreatePost("thoughts");
        view.findViewById(R.id.fabProfileCreatePost).setOnClickListener(createPostListener);
        view.findViewById(R.id.btnStartGrowing).setOnClickListener(createPostListener);
        view.findViewById(R.id.actionFirstEntry).setOnClickListener(createPostListener);
        view.findViewById(R.id.actionSetMilestone).setOnClickListener(v -> openCreatePost("milestone"));
    }

    private void setupRecyclerView() {
        myPostsAdapter = new CommunityFeedAdapter(
                item -> {
                    Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                    intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                    startActivity(intent);
                },
                this::showPostBottomSheet
        );

        // CẬP NHẬT: THÊM LOGIC ĐỂ CHO PHÉP LIKE BÀI VIẾT TRÊN TƯỜNG NHÀ
        myPostsAdapter.setOnLikeClickListener((postId, isLiked, callback) -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    socialViewModel.toggleLike(postId, token, isLiked);
                    callback.onResult(true);
                }).addOnFailureListener(e -> callback.onResult(false));
            } else {
                callback.onResult(false);
            }
        });

        rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyPosts.setNestedScrollingEnabled(false);
        rvMyPosts.setHasFixedSize(false);
        rvMyPosts.setAdapter(myPostsAdapter);

        badgeAdapter = new ProfileAchievementAdapter();
        rvAchievementsProfile.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvAchievementsProfile.setAdapter(badgeAdapter);
    }

    private void observeViewModel() {
        socialViewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            if (doc == null) return;
            String displayName = firstNonEmpty(doc.getString("displayName"), doc.getString("username"), DEFAULT_USERNAME);
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"), doc.getString("about"), "Ready to share your finance journey.");
            String avatarUrl = doc.getString("avatarUrl");

            tvDisplayName.setText(displayName);
            tvBio.setText(bio);
            tvJoinedDate.setText(joinedLabel(doc));
            tvStreakCount.setText(Math.max(0, numberField(doc, "streakDays", 0)) + " days");
            ImageHelper.loadAvatar(avatarUrl, imgAvatar, firstNonEmpty(displayName, currentUserId));
        });

        socialViewModel.getFriendCount().observe(getViewLifecycleOwner(), count ->
                tvFriendCount.setText(Math.max(0, count) + " friends"));

        socialViewModel.getIsDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                socialViewModel.resetDeleteStatus();
                loadMyPosts(); // Refresh wall after deletion
            }
        });
    }

    // =========================================================================
    // DATA FETCHING & PARSING
    // =========================================================================

    private void loadMyPosts() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            showEmptyState(true);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyState(true);
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            apiService.getWall(token, currentUserId, 30, 0).enqueue(new Callback<List<Object>>() {
                @Override
                public void onResponse(@NonNull Call<List<Object>> call, @NonNull Response<List<Object>> response) {
                    if (!isAdded()) return;
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "API error: " + response.code());
                        showEmptyState(true);
                        return;
                    }
                    bindProfilePosts(response.body());
                }

                @Override
                public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    Log.e(TAG, "Network error: " + t.getMessage());
                    showEmptyState(true);
                }
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Token error: " + e.getMessage());
            showEmptyState(true);
        });
    }

    @SuppressWarnings("unchecked")
    private void bindProfilePosts(List<Object> raw) {
        List<FeedItem> posts = new ArrayList<>();
        int achievementCount = 0;
        FeedItem firstAchievement = null;

        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) obj;
            FeedItem item = mapPostFromMap(map);
            posts.add(item);

            if (item instanceof FeedItem.MilestonePost) {
                achievementCount++;
                if (firstAchievement == null) firstAchievement = item;
            }
        }

        myPostsAdapter.submitList(posts);
        tvPostCount.setText(posts.size() + " posts");
        tvTrophyCount.setText(achievementCount + " achievements");
        bindPinnedAchievement(firstAchievement, posts.size());
        showEmptyState(posts.isEmpty());
    }

    private FeedItem mapPostFromMap(Map<String, Object> map) {
        String id = str(map, "postId");
        // CẬP NHẬT: LẤY TRƯỜNG title TỪ DỮ LIỆU CỦA API Backend
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
            String milestoneData = str(map, "milestoneData");
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
                    if (!jsonDesc.isEmpty() && content.isEmpty()) {
                        description = jsonDesc;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                }
            }

            item = new FeedItem.MilestonePost(
                    id, userId,
                    name.isEmpty() ? "You" : name,
                    formatTime(timestamp),
                    milestoneTitle, description, month, amount, iconText, progress,
                    description.length() > 120, milestoneData, avatarUrl, initials(name)
            );
        } else {
            // CẬP NHẬT: NHÉT BIẾN title VÀO TRONG CONSTRUCTOR
            item = new FeedItem.NormalPost(
                    id, userId,
                    name.isEmpty() ? "You" : name,
                    formatTime(timestamp),
                    title,
                    content, hasImage, imageUrl, initials(name), expandable, avatarUrl
            );
        }

        item.setLikeCount((int) num(map, "likeCount"));
        item.setCommentCount((int) num(map, "commentCount"));
        item.setLiked(Boolean.TRUE.equals(map.get("isLiked")));

        return item;
    }

    private void listenToAchievementsRealtime() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        achievementsRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("shared_achievements")
                .orderBy("sharedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    tvTrophyCount.setText(snapshots.size() + " Trophies");

                    List<ProfileAchievementAdapter.BadgeMeta> badges = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        badges.add(getBadgeMetaFromId(doc.getId()));
                    }
                    badgeAdapter.updateData(badges);
                });
    }

    // =========================================================================
    // INTERACTIONS
    // =========================================================================

    private void showPostBottomSheet(FeedItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);
        boolean isOwner = !currentUserId.isEmpty() && currentUserId.equals(item.getUserId());

        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (isOwner) {
            btnEditPost.setVisibility(View.VISIBLE);
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            btnDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    socialViewModel.deletePost(item.getId(), token);
                });
            });

            btnEditPost.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra("ACTION_EDIT_POST", true);
                intent.putExtra("edit_post_id", item.getId());

                // CẬP NHẬT: Truyền Title và Content sang màn Edit cho đúng DTO mới
                if (item instanceof FeedItem.NormalPost) {
                    intent.putExtra("edit_post_title", ((FeedItem.NormalPost) item).title);
                    intent.putExtra("edit_post_content", ((FeedItem.NormalPost) item).description); // Đổi .text thành .description
                } else if (item instanceof FeedItem.MilestonePost) {
                    intent.putExtra("edit_post_content", ((FeedItem.MilestonePost) item).description);
                    intent.putExtra("edit_milestone_data", ((FeedItem.MilestonePost) item).milestoneJson);
                }
                startActivity(intent);
            });
        } else {
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            btnDeletePost.setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.VISIBLE);

            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Post hidden", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Post reported", Toast.LENGTH_SHORT).show();
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void shareProfile() {
        String name = tvDisplayName.getText().toString().trim();
        String text = (name.isEmpty() ? "Cashify Profile" : name)
                + "\n" + profileLink()
                + "\nCheck out my financial journey on Cashify.";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share profile"));
    }

    private void copyProfileLink() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cashify profile", profileLink()));
            Toast.makeText(requireContext(), "Profile link copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCreatePost(String categoryKey) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCreatePostScreen(categoryKey);
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void bindPinnedAchievement(@Nullable FeedItem achievement, int postCount) {
        if (achievement instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) achievement;
            tvPinnedAchievement.setText(milestone.title + " · " + milestone.amount);
            return;
        }
        tvPinnedAchievement.setText(postCount > 0
                ? "Started building your personal financial house."
                : "Share your first milestone to pin a highlighted achievement here.");
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String profileLink() {
        return currentUserId == null || currentUserId.trim().isEmpty()
                ? "cashify.vn/profile"
                : "cashify.vn/profile/" + currentUserId;
    }

    private String joinedLabel(DocumentSnapshot doc) {
        long createdAt = numberField(doc, "createdAt", 0);
        if (createdAt <= 0) createdAt = numberField(doc, "joinedAt", 0);
        if (createdAt <= 0) return "Cashify Member";
        return "Joined " + new SimpleDateFormat("MM/yyyy", Locale.ENGLISH).format(new Date(createdAt));
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private long numberField(DocumentSnapshot doc, String field, long fallback) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : "";
    }

    private long num(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "Just now";
        return TimeFormatter.format(timestamp);
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.getDefault());
    }

    private ProfileAchievementAdapter.BadgeMeta getBadgeMetaFromId(String achId) {
        if (achId.startsWith("ach_trans_")) {
            return new ProfileAchievementAdapter.BadgeMeta("Hardworking Bee", "🐝", "#FFFDF7");
        }
        if (achId.startsWith("recap_surplus_")) {
            return new ProfileAchievementAdapter.BadgeMeta("Surplus", "💰", "#E2F5DA");
        }
        return new ProfileAchievementAdapter.BadgeMeta("Mystery Badge", "🎁", "#E2ECFF");
    }
}