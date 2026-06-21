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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.common.BaseFragment;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SocialProfileFragment extends BaseFragment {

    private ImageView imgAvatar;
    private TextView tvDisplayName, tvBio, tvFriendCount, tvTrophyCount;
    private TextView tvPostCount, tvJoinedDate, tvPinnedAchievement, tvStreakCount;
    private RecyclerView rvMyPosts, rvAchievementsProfile;
    private View layoutEmptyState;
    private View layoutActionCards;

    private SocialProfileViewModel viewModel;
    private SocialNewsfeedAdapter myPostsAdapter;
    private ProfileAchievementAdapter badgeAdapter;

    private String currentUserId = "";
    private boolean viewingOwnProfile = true;   // ui-consistency
    public static String syncedPostId = null;   // master

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (syncedPostId == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                viewModel.syncSinglePost(syncedPostId, "Bearer " + result.getToken());
                syncedPostId = null;
            });
        } else {
            syncedPostId = null;
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
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        rvAchievementsProfile = view.findViewById(R.id.rvAchievementsProfile);
        layoutActionCards = view.findViewById(R.id.layoutActionCards);
    }

   
    private void initToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialProfile);
        TextView tvGreeting = view.findViewById(R.id.tvProfileGreeting);

        if (viewingOwnProfile) {
            if (tvGreeting != null) {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour >= 5 && hour < 12) greeting = "Good morning ☀️";
                else if (hour >= 12 && hour < 18) greeting = "Good afternoon 🌤️";
                else greeting = "Good night 🌙";
                tvGreeting.setText(greeting);
                tvGreeting.setVisibility(View.VISIBLE);
            }
            view.findViewById(R.id.btnProfileNotifications).setVisibility(View.VISIBLE);
        } else {
            if (tvGreeting != null) tvGreeting.setVisibility(View.GONE);
            toolbar.setTitle(R.string.social_profile_title);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_back);
            view.findViewById(R.id.btnProfileNotifications).setVisibility(View.GONE);
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
        viewModel = new ViewModelProvider(this).get(SocialProfileViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String signedInUserId = currentUser != null ? currentUser.getUid() : "";
        String requestedUserId = getArguments() != null ? getArguments().getString("USER_ID") : null;

        currentUserId = firstNonEmpty(requestedUserId, signedInUserId);
        viewingOwnProfile = !signedInUserId.trim().isEmpty() && currentUserId.equals(signedInUserId);

        if (currentUser != null && !currentUserId.trim().isEmpty()) {
            viewModel.loadProfileData(currentUserId);
            viewModel.loadAchievements(currentUserId);
            currentUser.getIdToken(true).addOnSuccessListener(result ->
                    viewModel.loadWallPosts(currentUserId, "Bearer " + result.getToken()));
        } else {
            showEmptyState(true);
        }
    }

    
    private void setupActions(View view) {
        int ownVisibility = viewingOwnProfile ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.btnEditProfile).setVisibility(ownVisibility);
        view.findViewById(R.id.fabProfileCreatePost).setVisibility(ownVisibility);
        view.findViewById(R.id.btnStartGrowing).setVisibility(ownVisibility);
        view.findViewById(R.id.actionSetMilestone).setVisibility(ownVisibility);
        view.findViewById(R.id.actionFirstEntry).setVisibility(ownVisibility);

        if (viewingOwnProfile) {
            view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), EditProfileActivity.class)));

            View.OnClickListener createThoughts = v -> openCreatePost("thoughts");
            view.findViewById(R.id.btnStartGrowing).setOnClickListener(createThoughts);
            view.findViewById(R.id.actionFirstEntry).setOnClickListener(createThoughts);
            view.findViewById(R.id.actionSetMilestone).setOnClickListener(v -> openCreatePost("milestone"));

            view.findViewById(R.id.actionAchievementEarly).setOnClickListener(v ->
                    toast("Early Start: post your first post to unlock."));
            view.findViewById(R.id.actionAchievementTop).setOnClickListener(v ->
                    toast("Top 1%: maintain consistent engagement."));
            view.findViewById(R.id.actionAchievementVault).setOnClickListener(v ->
                    toast("Safe Vault: complete a savings milestone."));
            view.findViewById(R.id.actionAchievementGiver).setOnClickListener(v ->
                    toast("The Giver: share a useful financial tip."));
        }

        view.findViewById(R.id.btnShareProfile).setOnClickListener(v -> shareProfile());
        view.findViewById(R.id.txtProfileLink).setOnClickListener(v -> copyProfileLink());
    }

    private void setupRecyclerView() {
        myPostsAdapter = new SocialNewsfeedAdapter(
                item -> {
                    Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                    intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                    startActivity(intent);
                    syncedPostId = item.getId();
                },
                this::showPostBottomSheet
        );

        myPostsAdapter.setOnLikeClickListener((postId, isLiked, callback) -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.getIdToken(true)
                        .addOnSuccessListener(result -> {
                            viewModel.toggleLike(postId, "Bearer " + result.getToken(), isLiked);
                            callback.onResult(true);
                        })
                        .addOnFailureListener(e -> callback.onResult(false));
            } else {
                callback.onResult(false);
            }
        });

        rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyPosts.setNestedScrollingEnabled(false);
        rvMyPosts.setHasFixedSize(false);
        rvMyPosts.setAdapter(myPostsAdapter);

        badgeAdapter = new ProfileAchievementAdapter();
        rvAchievementsProfile.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvAchievementsProfile.setAdapter(badgeAdapter);
    }

    
    private void observeViewModel() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            if (doc == null) return;
            String displayName = firstNonEmpty(doc.getString("displayName"), doc.getString("username"), "Cashify User");
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"), doc.getString("about"),
                    "Ready to share your finance journey.");

            tvDisplayName.setText(displayName);
            tvBio.setText(bio);
            tvJoinedDate.setText(joinedLabel(doc));

            // ui-consistency: updateStreakState với visual (background, color, alpha)
            Object streakObj = doc.get("streakDays");
            long streak = streakObj instanceof Number ? ((Number) streakObj).longValue() : 0;
            updateStreakState((int) Math.max(0, streak));

            ImageHelper.loadAvatar(doc.getString("avatarUrl"), imgAvatar,
                    firstNonEmpty(displayName, currentUserId));
        });

        viewModel.getFriendCount().observe(getViewLifecycleOwner(), count ->
                tvFriendCount.setText(Math.max(0, count) + " friends"));

        viewModel.getAchievements().observe(getViewLifecycleOwner(), badges -> {
            if (badges == null) return;
            tvTrophyCount.setText(badges.size() + " Trophies");
            badgeAdapter.updateData(badges);
        });

        viewModel.getWallPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts == null) return;
            myPostsAdapter.submitList(new ArrayList<>(posts));
            tvPostCount.setText(posts.size() + " posts");

            FeedItem firstMilestone = null;
            for (FeedItem item : posts) {
                if (item instanceof FeedItem.MilestonePost) {
                    firstMilestone = item;
                    break;
                }
            }
            // ui-consistency: earnedCount param
            bindPinnedAchievement(firstMilestone, posts.size(), 0);
            showEmptyState(posts.isEmpty());
            if (layoutActionCards != null) {
                layoutActionCards.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                toast("Post deleted");
                viewModel.resetDeleteStatus();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) toast(error);
        });
    }

    
    private void showPostBottomSheet(FeedItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_option, null);

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
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) return; // ui-consistency: null-check
                user.getIdToken(true).addOnSuccessListener(result ->
                        viewModel.deletePost(item.getId(), "Bearer " + result.getToken()));
            });

            btnEditPost.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra("ACTION_EDIT_POST", true);
                intent.putExtra("edit_post_id", item.getId());
                if (item instanceof FeedItem.NormalPost) {
                    FeedItem.NormalPost post = (FeedItem.NormalPost) item;
                    intent.putExtra("edit_post_title", post.title);
                    intent.putExtra("edit_post_content", post.description);
                    intent.putExtra("edit_post_image", post.imageUrl);
                } else if (item instanceof FeedItem.MilestonePost) {
                    FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) item;
                    intent.putExtra("edit_post_content", milestone.description);
                    intent.putExtra("edit_milestone_data", milestone.milestoneJson);
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
                toast("Post hidden");
            });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                toast("Post reported");
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void bindPinnedAchievement(@Nullable FeedItem achievement, int postCount, int earnedCount) {
        if (achievement instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) achievement;
            String title = (milestone.title == null || milestone.title.trim().isEmpty()
                    || milestone.title.equals("New Milestone"))
                    ? "Financial Milestone"
                    : milestone.title;
            String text = title;
            if (milestone.amount != null && !milestone.amount.trim().isEmpty()
                    && !milestone.amount.equals("null")) {
                text += " · " + milestone.amount;
            }
            tvPinnedAchievement.setText(text);
            return;
        }
        tvPinnedAchievement.setText(earnedCount > 0
                ? "Achievement unlocked. Keep building your financial journey."
                : postCount > 0
                ? "Started building your personal financial house."
                : "Share your first milestone to pin a highlighted achievement here.");
    }

    // ui-consistency: visual states (background, color, alpha)
    private void updateStreakState(int currentStreakDays) {
        if (tvStreakCount == null) return;
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
    private ProfileAchievementState evaluateAchievements(java.util.List<Object> raw) {
        ProfileAchievementState state = new ProfileAchievementState();
        Set<Long> postDays = new HashSet<>();

        for (Object obj : raw) {
            if (!(obj instanceof java.util.Map)) continue;
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            long timestamp = normalizeTimestamp(num(map, "timestamp"));
            if (timestamp > 0) postDays.add(dayKey(timestamp));

            String content = str(map, "content").toLowerCase(Locale.US);
            String type = str(map, "type").toLowerCase(Locale.US);
            String category = str(map, "category").toLowerCase(Locale.US);
            String categoryKey = str(map, "categoryKey").toLowerCase(Locale.US);

            state.hasFirstPost = true;
            if (isCompletedMilestone(map)) state.hasCompletedMilestone = true;
            if (type.contains("share") || category.contains("share")
                    || categoryKey.contains("share") || content.contains("#tip")
                    || content.contains("#share") || content.contains(" tip ")) {
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

    @SuppressWarnings("unchecked")
    private boolean isCompletedMilestone(java.util.Map<String, Object> map) {
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
        if (postDays.contains(today)) cursor = today;
        else if (postDays.contains(yesterday)) cursor = yesterday;
        else return 0;

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
            tvTrophyCount.setText("0 achievements");
            updateStreakState(0);
            bindPinnedAchievement(null, 0, 0);
        }
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void shareProfile() {
        String name = tvDisplayName.getText().toString().trim();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                (name.isEmpty() ? "Cashify Profile" : name)
                        + "\n" + profileLink()
                        + "\nCheck out my financial journey on Cashify.");
        startActivity(Intent.createChooser(intent, "Share profile"));
    }

    private void copyProfileLink() {
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cashify profile", profileLink()));
            toast("Profile link copied");
        }
    }

    
    private void openCreatePost(String categoryKey) {
        if (!viewingOwnProfile) return;
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCreatePostScreen(categoryKey);
        }
    }

    
    private String profileLink() {
        return currentUserId == null || currentUserId.trim().isEmpty()
                ? "cashify.vn/ho-so"
                : "cashify.vn/ho-so/" + currentUserId;
    }

    private String joinedLabel(DocumentSnapshot doc) {
        Object raw = doc.get("createdAt");
        if (raw == null) raw = doc.get("joinedAt");
        long ts = raw instanceof Number ? ((Number) raw).longValue() : 0;
        return ts <= 0 ? "Cashify Member"
                : "Joined " + new SimpleDateFormat("MM/yyyy", Locale.ENGLISH).format(new Date(ts));
    }

    private String str(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : "";
    }

    private long num(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}