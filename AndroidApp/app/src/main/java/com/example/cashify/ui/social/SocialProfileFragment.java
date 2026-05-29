package com.example.cashify.ui.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.feed.CommunityFeedAdapter;
import com.example.cashify.ui.feed.FeedItem;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

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

    private ShapeableImageView imgAvatar;
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
    private String currentUserId = "";
    private boolean isOwnProfile = true;
    private boolean isFromNewsfeed = false; // CỜ ĐIỀU HƯỚNG: Xác định có phải đi từ Bảng tin sang không

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LIFECYCLE
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

        // ĐÃ SỬA: Xác định nguồn gốc điều hướng dựa trên Destination ID trong NavGraph
        checkNavigationSource();

        initViewModel();
        initToolbar(view);

        setupActions();
        setupRecyclerView();
        observeViewModel();
        loadMyPosts();

        // ĐÃ SỬA: Cứ đi từ bảng tin sang là kích hoạt nút Back hệ thống, chấp nhận cả trường hợp click vào chính mình
        if (isFromNewsfeed) {
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                    new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            androidx.navigation.NavController navController = NavHostFragment.findNavController(SocialProfileFragment.this);
                            if (!navController.popBackStack()) {
                                setEnabled(false);
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                                setEnabled(true);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CÁC HÀM SETUP
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void checkNavigationSource() {
        try {
            int currentDestId = NavHostFragment.findNavController(this).getCurrentDestination().getId();
            // Nếu ID trùng với nav_other_profile trong bản đồ định tuyến -> Đi từ Newsfeed sang
            isFromNewsfeed = (currentDestId == R.id.nav_other_profile);
        } catch (Exception e) {
            isFromNewsfeed = false;
        }
    }

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

        // ĐÃ SỬA: Logic hiển thị nút bấm Toolbar dựa vào nguồn gốc di chuyển thay vì ID chủ sở hữu
        if (isFromNewsfeed) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_back);
            toolbar.setNavigationOnClickListener(v -> {
                androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
                if (!navController.popBackStack()) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        } else {
            // Nếu chọn từ Menu Tab chính: Hiện nút Hamburger để mở thanh Menu Drawer cạnh giường
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() == null) return;
                androidx.drawerlayout.widget.DrawerLayout drawer =
                        getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START);
                }
            });
        }
    }

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            isOwnProfile = true;

            if (getArguments() != null && getArguments().containsKey("userId")) {
                String targetUid = getArguments().getString("userId");
                if (targetUid != null && !targetUid.trim().isEmpty()) {
                    currentUserId = targetUid;
                    isOwnProfile = currentUserId.equals(currentUser.getUid());
                }
            }

            // Nút Chỉnh sửa chỉ phụ thuộc vào việc có phải chính chủ hay không (vẫn giữ nguyên)
            if (!isOwnProfile) {
                btnEditProfile.setVisibility(View.GONE);
            } else {
                btnEditProfile.setVisibility(View.VISIBLE);
            }

            socialViewModel.loadProfile(currentUserId);
        }
    }

    private void setupActions() {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));
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
                    doc.getString("username"), "Người dùng Cashify");
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"),
                    doc.getString("about"), "Sẵn sàng chia sẻ hành trình tài chính.");
            String avatarUrl = doc.getString("avatarUrl");
            tvDisplayName.setText(displayName);
            tvBio.setText(bio);
            tvJoinedDate.setText(joinedLabel(doc));
            tvStreakCount.setText("Streak " + Math.max(0, numberField(doc, "streakDays", 0)) + " ngày");
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                ImageHelper.loadAvatar(avatarUrl, imgAvatar);
            }
        });

        socialViewModel.getFriendCount().observe(getViewLifecycleOwner(), count ->
                tvFriendCount.setText(Math.max(0, count) + " bạn bè"));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOAD POSTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
        tvPostCount.setText(posts.size() + " bài viết");
        tvTrophyCount.setText(achievementCount + " thành tựu");
        bindPinnedAchievement(firstAchievement, posts.size());
        showEmptyState(posts.isEmpty());
    }

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

        String userId = firstNonEmpty(str(map, "authorId"), str(map, "userId"), currentUserId);

        if (type.contains("milestone") || type.contains("achievement")) {
            long progress = Math.max(0, Math.min(100, num(map, "progress")));
            String title  = firstNonEmpty(str(map, "title"), achievementTitle(content));
            String amount = firstNonEmpty(str(map, "amountText"), progress + "% hoàn thành");
            String month  = firstNonEmpty(str(map, "period"), "Thành tựu");

            return new FeedItem.MilestonePost(
                    id, userId, title, content, month, amount, progress + "%", (int) progress, expandable);
        }

        return new FeedItem.NormalPost(
                id,
                userId,
                name.isEmpty() ? (isOwnProfile ? "Bạn" : "Thành viên Cashify") : name,
                formatTime(timestamp),
                content,
                hasImage,
                imageUrl,
                avatarColor(name),
                initials(name),
                expandable,
                avatarUrl
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CÁC HÀM HELPER
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void bindPinnedAchievement(@Nullable FeedItem achievement, int postCount) {
        if (achievement instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) achievement;
            tvPinnedAchievement.setText(milestone.title + " · " + milestone.amount);
            return;
        }
        tvPinnedAchievement.setText(postCount > 0
                ? "Đã bắt đầu xây dựng ngôi nhà tài chính cá nhân."
                : "Chia sẻ cột mốc đầu tiên để ghim thành tựu nổi bật tại đây.");
    }

    private void showEmptyState(boolean show) {
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvMyPosts != null) rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String joinedLabel(DocumentSnapshot doc) {
        long createdAt = numberField(doc, "createdAt", 0);
        if (createdAt <= 0) createdAt = numberField(doc, "joinedAt", 0);
        if (createdAt <= 0) return "Thành viên Cashify";
        return "Tham gia " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    private String achievementTitle(String content) {
        if (content == null || content.trim().isEmpty()) return "Cột mốc tài chính mới";
        return content.length() > 54 ? content.substring(0, 54).trim() + "..." : content.trim();
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