package com.example.cashify.ui.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import androidx.navigation.Navigation;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;  // GIỮ — dùng trong observeViewModel

// ĐÃ XÓA: import FirebaseFirestore, ListenerRegistration, Query
// (không còn dùng Firestore snapshot listener để load posts nữa)

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;  // THÊM MỚI — dùng trong bindProfilePosts

import retrofit2.Call;       // THÊM MỚI
import retrofit2.Callback;   // THÊM MỚI
import retrofit2.Response;   // THÊM MỚI

public class SocialProfileFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvDisplayName;
    private TextView tvBio;
    private TextView tvFriendCount;
    private TextView tvTrophyCount;
    private TextView tvPostCount;
    private TextView tvJoinedDate;
    private TextView tvPinnedAchievement;
    private TextView tvStreakCount;
    private TextView btnEditProfile;
    private RecyclerView rvMyPosts;
    private View layoutEmptyState;

    private SocialViewModel socialViewModel;
    private CommunityFeedAdapter myPostsAdapter;
    // ĐÃ XÓA: private ListenerRegistration postsRegistration; — không cần nữa
    private String currentUserId = "";
    private boolean viewingOwnProfile = true;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LIFECYCLE — GIỮ NGUYÊN, chỉ bỏ cleanup listener
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
        initViewModel();
        initToolbar(view);
        setupActions(view);
        setupRecyclerView();
        observeViewModel();
        loadMyPosts();
    }

    @Override
    public void onDestroyView() {
        // ĐÃ XÓA: postsRegistration.remove() — API call tự hủy, không cần cleanup
        super.onDestroyView();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CÁC HÀM SETUP — GIỮ NGUYÊN 100%, không đổi gì
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
    }

    private void initToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialProfile);
        TextView tvGreeting = view.findViewById(R.id.tvProfileGreeting);
        if (viewingOwnProfile) {
            if (tvGreeting != null) {
                int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour >= 5 && hour < 12) {
                    greeting = "Good morning ☀️";
                } else if (hour >= 12 && hour < 18) {
                    greeting = "Good afternoon 🌤️";
                } else {
                    greeting = "Good night 🌙";
                }
                tvGreeting.setText(greeting);
                tvGreeting.setVisibility(View.VISIBLE);
            }
            view.findViewById(R.id.btnProfileNotifications).setVisibility(View.VISIBLE);
        } else {
            if (tvGreeting != null) {
                tvGreeting.setVisibility(View.GONE);
            }
            toolbar.setTitle(R.string.social_profile_title);
            view.findViewById(R.id.btnProfileNotifications).setVisibility(View.GONE);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_back);
        }
        toolbar.setNavigationOnClickListener(v -> {
            if (!viewingOwnProfile) {
                Navigation.findNavController(view).popBackStack();
                return;
            }
            if (getActivity() == null) return;
            androidx.drawerlayout.widget.DrawerLayout drawer =
                    getActivity().findViewById(R.id.drawerLayout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });
    }

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String signedInUserId = currentUser != null ? currentUser.getUid() : "";
        String requestedUserId = getArguments() != null ? getArguments().getString("USER_ID") : null;

        currentUserId = firstNonEmpty(requestedUserId, signedInUserId);
        viewingOwnProfile = !signedInUserId.trim().isEmpty() && currentUserId.equals(signedInUserId);

        if (!currentUserId.trim().isEmpty()) {
            socialViewModel.loadProfile(currentUserId);
        }
    }

    private void setupActions(View view) {
        btnEditProfile.setVisibility(viewingOwnProfile ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.fabProfileCreatePost).setVisibility(viewingOwnProfile ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.btnStartGrowing).setVisibility(viewingOwnProfile ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.actionSetMilestone).setVisibility(viewingOwnProfile ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.actionFirstEntry).setVisibility(viewingOwnProfile ? View.VISIBLE : View.GONE);

        btnEditProfile.setOnClickListener(v -> {
            if (viewingOwnProfile) {
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
            }
        });

        view.findViewById(R.id.btnShareProfile).setOnClickListener(v -> shareProfile());
        view.findViewById(R.id.txtProfileLink).setOnClickListener(v -> copyProfileLink());
        view.findViewById(R.id.btnProfileNotifications).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), InvitationsActivity.class)));
        view.findViewById(R.id.fabProfileCreatePost).setOnClickListener(v -> openCreatePost("thoughts"));
        view.findViewById(R.id.btnStartGrowing).setOnClickListener(v -> openCreatePost("thoughts"));
        view.findViewById(R.id.actionSetMilestone).setOnClickListener(v -> openCreatePost("milestone"));
        view.findViewById(R.id.actionFirstEntry).setOnClickListener(v -> openCreatePost("thoughts"));
        view.findViewById(R.id.actionAchievementEarly).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Early Start: post your first post to unlock.", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.actionAchievementTop).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Top 1%: maintain consistent engagement.", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.actionAchievementVault).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Safe Vault: complete a savings milestone.", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.actionAchievementGiver).setOnClickListener(v ->
                Toast.makeText(requireContext(), "The Giver: share a useful financial tip.", Toast.LENGTH_SHORT).show());
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
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cashify profile", profileLink()));
            Toast.makeText(requireContext(), "Profile link copied", Toast.LENGTH_SHORT).show();
        }
    }

    private String profileLink() {
        return currentUserId == null || currentUserId.trim().isEmpty()
                ? "cashify.vn/ho-so"
                : "cashify.vn/ho-so/" + currentUserId;
    }

    private void openCreatePost(String categoryKey) {
        if (!viewingOwnProfile) {
            return;
        }
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCreatePostScreen(categoryKey);
        }
    }

    private void setupRecyclerView() {
        myPostsAdapter = new CommunityFeedAdapter(item -> {
            Intent intent = new Intent(requireContext(), PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
            startActivity(intent);
        });
        rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyPosts.setNestedScrollingEnabled(false);
        rvMyPosts.setHasFixedSize(false);
        rvMyPosts.setAdapter(myPostsAdapter);
    }

    private void observeViewModel() {
        socialViewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            if (doc == null) return;
            String displayName = firstNonEmpty(doc.getString("displayName"),
                    doc.getString("username"), "Cashify User");
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"),
                    doc.getString("about"), "Ready to share your finance journey.");
            String avatarUrl = doc.getString("avatarUrl");
            tvDisplayName.setText(displayName);
            tvBio.setText(bio);
            tvJoinedDate.setText(joinedLabel(doc));
            updateStreakState(0);
            ImageHelper.loadAvatar(avatarUrl, imgAvatar, firstNonEmpty(displayName, currentUserId));
        });

        socialViewModel.getFriendCount().observe(getViewLifecycleOwner(), count ->
                tvFriendCount.setText(Math.max(0, count) + " friends"));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOAD POSTS — THAY HOÀN TOÀN: Firestore snapshot → gọi API wall
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // ĐÃ XÓA toàn bộ hàm loadMyPosts() cũ (dùng addSnapshotListener)
    // THAY BẰNG hàm này: gọi API /post/wall/{uid}
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

            apiService.getWall(token, currentUserId, 30, 0)
                    .enqueue(new Callback<List<Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Object>> call,
                                               @NonNull Response<List<Object>> response) {
                            if (!isAdded()) return;
                            if (!response.isSuccessful() || response.body() == null) {
                                android.util.Log.e("PROFILE", "API lỗi: " + response.code());
                                showEmptyState(true);
                                return;
                            }
                            bindProfilePosts(response.body());
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            android.util.Log.e("PROFILE", "Mạng lỗi: " + t.getMessage());
                            showEmptyState(true);
                        }
                    });

        }).addOnFailureListener(e -> {
            android.util.Log.e("PROFILE", "Token lỗi: " + e.getMessage());
            showEmptyState(true);
        });
    }

    // ĐÃ XÓA: mapPost(DocumentSnapshot doc) — không còn nhận Firestore doc nữa
    // THAY BẰNG 2 hàm dưới đây:

    // Hàm nhận List<Object> từ API response, tạo danh sách FeedItem
    @SuppressWarnings("unchecked")
    private void bindProfilePosts(List<Object> raw) {
        List<FeedItem> posts = new ArrayList<>();
        ProfileAchievementState achievementState = evaluateAchievements(raw);
        FeedItem pinnedAchievement = null;

        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) obj;
            FeedItem item = mapPostFromMap(map);
            posts.add(item);
            if (pinnedAchievement == null && item instanceof FeedItem.MilestonePost
                    && isCompletedMilestone(map)) {
                pinnedAchievement = item;
            }
        }

        myPostsAdapter.submitList(posts);
        tvPostCount.setText(posts.size() + " posts");
        tvTrophyCount.setText(achievementState.earnedCount + " achievements");
        updateStreakState(achievementState.currentStreakDays);
        bindAchievementBadges(achievementState);
        bindPinnedAchievement(pinnedAchievement, posts.size(), achievementState.earnedCount);
        showEmptyState(posts.isEmpty());
    }

    // Hàm map 1 record (Map) từ API → FeedItem
    private FeedItem mapPostFromMap(Map<String, Object> map) {
        String id       = str(map, "postId");
        String content  = str(map, "content");
        String imageUrl = str(map, "imageUrl");
        String type     = str(map, "type").toLowerCase(Locale.US);
        long timestamp  = num(map, "timestamp");
        String name     = str(map, "authorName");
        String avatarUrl = str(map, "authorAvatarUrl");
        boolean hasImage   = !imageUrl.isEmpty();
        boolean expandable = content.length() > 120;

        if (type.contains("milestone") || type.contains("achievement")) {
            long progress = Math.max(0, Math.min(100, num(map, "progress")));
            String title  = firstNonEmpty(str(map, "title"), achievementTitle(content));
            String amount = firstNonEmpty(str(map, "amountText"), progress + "% hoàn thành");
            String month  = firstNonEmpty(str(map, "period"), "Thành tựu");
            String description = title.trim().equalsIgnoreCase(content.trim()) ? "" : content;
            return new FeedItem.MilestonePost(
                    id,
                    "",
                    name.isEmpty() ? "Bạn" : name,
                    formatTime(timestamp),
                    title,
                    description,
                    month,
                    amount,
                    progress + "%",
                    (int) progress,
                    description.length() > 120,
                    null,
                    avatarUrl,
                    initials(name)
            );
        }

        return new FeedItem.NormalPost(
                id,
                name.isEmpty() ? "Bạn" : name,
                formatTime(timestamp),
                content,
                hasImage,
                imageUrl,           // ← truyền URL thật để Glide load
                avatarColor(name),
                initials(name),
                expandable,
                avatarUrl
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CÁC HÀM HELPER — GIỮ NGUYÊN, chỉ thêm str() và num()
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void bindPinnedAchievement(@Nullable FeedItem achievement, int postCount, int earnedCount) {
        if (achievement instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) achievement;
            tvPinnedAchievement.setText(milestone.title + " · " + milestone.amount);
            return;
        }
        tvPinnedAchievement.setText(earnedCount > 0
                ? "Achievement unlocked. Keep building your financial journey."
                : postCount > 0
                ? "Started building your personal financial house."
                : "Share your first milestone to pin a highlighted achievement here.");
    }

    private void updateStreakState(int currentStreakDays) {
        int days = Math.max(0, currentStreakDays);
        boolean active = days >= 2;
        tvStreakCount.setText(days + " days");
        tvStreakCount.setBackgroundResource(active
                ? R.drawable.bg_profile_streak_fire_circle
                : R.drawable.bg_profile_streak_fire_circle_inactive);
        tvStreakCount.setTextColor(androidx.core.content.ContextCompat.getColor(
                requireContext(), active ? R.color.status_red : R.color.item_description));
        tvStreakCount.setAlpha(active ? 1f : 0.65f);
    }

    @SuppressWarnings("unchecked")
    private ProfileAchievementState evaluateAchievements(List<Object> raw) {
        ProfileAchievementState state = new ProfileAchievementState();
        Set<Long> postDays = new HashSet<>();

        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) obj;
            long timestamp = normalizeTimestamp(num(map, "timestamp"));
            if (timestamp > 0) postDays.add(dayKey(timestamp));

            String content = str(map, "content").toLowerCase(Locale.US);
            String type = str(map, "type").toLowerCase(Locale.US);
            String category = str(map, "category").toLowerCase(Locale.US);
            String categoryKey = str(map, "categoryKey").toLowerCase(Locale.US);

            state.hasFirstPost = true;
            if (isCompletedMilestone(map)) state.hasCompletedMilestone = true;
            if (type.contains("share")
                    || category.contains("share")
                    || categoryKey.contains("share")
                    || content.contains("#tip")
                    || content.contains("#share")
                    || content.contains(" tip ")) {
                state.hasSharedTip = true;
            }
        }

        state.currentStreakDays = currentConsecutivePostDays(postDays);
        state.hasTwoDayStreak = state.currentStreakDays >= 2;
        state.earnedCount = (state.hasFirstPost ? 1 : 0)
                + (state.hasTwoDayStreak ? 1 : 0)
                + (state.hasCompletedMilestone ? 1 : 0)
                + (state.hasSharedTip ? 1 : 0);
        return state;
    }

    private void bindAchievementBadges(ProfileAchievementState state) {
        setAchievementVisible(R.id.actionAchievementEarly, state.hasFirstPost);
        setAchievementVisible(R.id.actionAchievementTop, state.hasTwoDayStreak);
        setAchievementVisible(R.id.actionAchievementVault, state.hasCompletedMilestone);
        setAchievementVisible(R.id.actionAchievementGiver, state.hasSharedTip);
    }

    private void setAchievementVisible(int iconId, boolean visible) {
        View root = getView();
        if (root == null) return;
        View icon = root.findViewById(iconId);
        if (icon == null) return;
        View parent = (View) icon.getParent();
        parent.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isCompletedMilestone(Map<String, Object> map) {
        String type = str(map, "type").toLowerCase(Locale.US);
        if (!type.contains("milestone") && !type.contains("achievement")) return false;
        return Math.max(num(map, "progress"), progressFromMilestoneData(str(map, "milestoneData"))) >= 100;
    }

    private long progressFromMilestoneData(String milestoneData) {
        if (milestoneData == null || milestoneData.trim().isEmpty()) return 0;
        try {
            return new org.json.JSONObject(milestoneData).optLong("progress", 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int currentConsecutivePostDays(Set<Long> postDays) {
        if (postDays == null || postDays.isEmpty()) return 0;
        long today = dayKey(System.currentTimeMillis());
        long yesterday = today - 1;
        long cursor;
        if (postDays.contains(today)) {
            cursor = today;
        } else if (postDays.contains(yesterday)) {
            cursor = yesterday;
        } else {
            return 0;
        }

        int streak = 0;
        while (postDays.contains(cursor)) {
            streak++;
            cursor--;
        }
        return streak;
    }

    private long dayKey(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(normalizeTimestamp(timestamp));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() / 86_400_000L;
    }

    private long normalizeTimestamp(long timestamp) {
        return timestamp > 0 && timestamp < 1_000_000_000_000L ? timestamp * 1000L : timestamp;
    }

    private static class ProfileAchievementState {
        boolean hasFirstPost;
        boolean hasTwoDayStreak;
        boolean hasCompletedMilestone;
        boolean hasSharedTip;
        int currentStreakDays;
        int earnedCount;
    }

    private void showEmptyState(boolean show) {
        if (show) {
            ProfileAchievementState emptyState = new ProfileAchievementState();
            tvTrophyCount.setText("0 achievements");
            updateStreakState(0);
            bindAchievementBadges(emptyState);
            bindPinnedAchievement(null, 0, 0);
        }
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String joinedLabel(DocumentSnapshot doc) {
        long createdAt = numberField(doc, "createdAt", 0);
        if (createdAt <= 0) createdAt = numberField(doc, "joinedAt", 0);
        if (createdAt <= 0) return "Cashify's member";
        return "Participate " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    private String achievementTitle(String content) {
        if (content == null || content.trim().isEmpty()) return "New finance milestone";
        return content.length() > 54 ? content.substring(0, 54).trim() + "..." : content.trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    // GIỮ NGUYÊN — dùng cho DocumentSnapshot trong observeViewModel
    private long numberField(DocumentSnapshot doc, String field, long fallback) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    // THÊM MỚI — dùng cho Map từ API response
    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : "";
    }

    // THÊM MỚI — dùng cho Map từ API response
    private long num(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "Vừa xong";
        return com.example.cashify.utils.TimeFormatter.format(timestamp);
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.getDefault());
    }

    private int avatarColor(String key) {
        int[] colors = {
                R.color.brand_primary,
                R.color.cat_pastel_blue,
                R.color.cat_pastel_green,
                R.color.cat_pastel_pink,
                R.color.cat_pastel_indigo
        };
        int index = Math.abs((key == null ? "" : key).hashCode()) % colors.length;
        return androidx.core.content.ContextCompat.getColor(requireContext(), colors[index]);
    }
}
