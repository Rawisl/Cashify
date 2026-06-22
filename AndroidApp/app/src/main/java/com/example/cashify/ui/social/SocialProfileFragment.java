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
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.common.BaseFragment;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

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

        boolean isTopLevel = false;
        try {
            androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(view);
            if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.nav_social_profile) {
                isTopLevel = true;
            }
        } catch (Exception e) {
            // ignore
        }

        if (viewingOwnProfile && isTopLevel) {
            if (tvGreeting != null) {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour >= 5 && hour < 12) greeting = "Good morning ☀️";
                else if (hour >= 12 && hour < 18) greeting = "Good afternoon 🌤️";
                else greeting = "Good night 🌙";
                tvGreeting.setText(greeting);
                tvGreeting.setVisibility(View.VISIBLE);
            }
            View bellIcon = view.findViewById(R.id.imgBellIcon);
            if (bellIcon != null) {
                bellIcon.setVisibility(View.VISIBLE);
                bellIcon.setOnClickListener(v -> {
                    com.example.cashify.ui.notifications.NotificationBottomSheet bottomSheet = new com.example.cashify.ui.notifications.NotificationBottomSheet();
                    bottomSheet.show(getParentFragmentManager(), "NotificationBottomSheet");
                });
            }
            
            TextView tvBellBadge = view.findViewById(R.id.tvBellBadge);
            if (tvBellBadge != null && mainViewModel != null) {
                mainViewModel.getUnreadNotificationCount().observe(getViewLifecycleOwner(), count -> {
                    if (count != null && count > 0) {
                        tvBellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        tvBellBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvBellBadge.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            if (tvGreeting != null) {
                if (viewingOwnProfile) {
                    tvGreeting.setText(R.string.social_profile_title);
                } else {
                    tvGreeting.setText(R.string.social_profile_title);
                }
                tvGreeting.setVisibility(View.VISIBLE);
            }
            toolbar.setTitle("");
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_back);
            if (view.findViewById(R.id.imgBellIcon) != null) {
                view.findViewById(R.id.imgBellIcon).setVisibility(View.GONE);
            }
            if (view.findViewById(R.id.tvBellBadge) != null) {
                view.findViewById(R.id.tvBellBadge).setVisibility(View.GONE);
            }
        }

        final boolean topLevelFinal = isTopLevel;
        toolbar.setNavigationOnClickListener(v -> {
            if (!topLevelFinal) {
                boolean finishOnBack = getArguments() != null && getArguments().getBoolean("FINISH_ON_BACK", false);
                if (finishOnBack && getActivity() != null) {
                    getActivity().finish();
                } else {
                    androidx.navigation.Navigation.findNavController(view).popBackStack();
                }
            } else {
                if (getActivity() == null) return;
                androidx.drawerlayout.widget.DrawerLayout drawer =
                        getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START);
                }
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
        view.findViewById(R.id.fabProfileCreatePost).setVisibility(View.GONE);
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
        } else {
            view.findViewById(R.id.btnMessage).setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.example.cashify.ui.FriendsActivity.FriendChatActivity.class);
                intent.putExtra(com.example.cashify.ui.FriendsActivity.FriendChatActivity.EXTRA_FRIEND_UID, currentUserId);
                startActivity(intent);
            });

            view.findViewById(R.id.btnUnfriend).setOnClickListener(v -> {
                viewModel.unfriendUser(currentUserId);
            });

            view.findViewById(R.id.btnAddFriend).setOnClickListener(v -> {
                viewModel.sendFriendRequest(currentUserId);
            });
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

            ImageHelper.loadAvatar(doc.getString("avatarUrl"), imgAvatar,
                    firstNonEmpty(displayName, currentUserId));
            
        });

        viewModel.getFriendCount().observe(getViewLifecycleOwner(), count -> {
            tvFriendCount.setText(Math.max(0, count) + " friends");
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

            boolean isEmpty = posts.isEmpty();
            showEmptyState(isEmpty);
            if (!isEmpty) {
                ProfileAchievementState state = evaluateAchievements(new ArrayList<>(posts));
                bindPinnedAchievement(firstMilestone, posts.size(), state.earnedCount);
                updateStreakState(state.currentStreakDays);
            }
            if (layoutActionCards != null) {
                layoutActionCards.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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

        viewModel.isFriend().observe(getViewLifecycleOwner(), isFriend -> {
            if (!viewingOwnProfile && getView() != null) {
                getView().findViewById(R.id.btnMessage).setVisibility(isFriend ? View.VISIBLE : View.GONE);
                getView().findViewById(R.id.btnUnfriend).setVisibility(isFriend ? View.VISIBLE : View.GONE);
                getView().findViewById(R.id.btnAddFriend).setVisibility(isFriend ? View.GONE : View.VISIBLE);
            }
        });
        viewModel.getAchievements().observe(getViewLifecycleOwner(), badges -> {
            if (badges == null) return;

            // Lấy thêm danh sách tự tính
            java.util.List<ProfileAchievementAdapter.BadgeMeta> combinedBadges = new java.util.ArrayList<>(badges);

            // Đếm cho chuẩn tổng số lượng
            if (tvTrophyCount != null) {
                tvTrophyCount.setText(combinedBadges.size() + " achievements");
            }

            // Đẩy vào Adapter
            if (badgeAdapter != null) {
                badgeAdapter.updateData(combinedBadges);
            }
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
        boolean active = days >= 1;
        tvStreakCount.setText(days + (days == 1 ? " day" : " days"));
        tvStreakCount.setBackgroundResource(active
                ? R.drawable.bg_profile_streak_fire_circle
                : R.drawable.bg_profile_streak_fire_circle_inactive);
        tvStreakCount.setTextColor(androidx.core.content.ContextCompat.getColor(
                requireContext(), active ? R.color.status_red : R.color.item_description));
        tvStreakCount.setAlpha(active ? 1f : 0.65f);
    }
    
    private ProfileAchievementState evaluateAchievements(java.util.List<FeedItem> items) {
        ProfileAchievementState state = new ProfileAchievementState();
        Set<Long> postDays = new HashSet<>();

        for (FeedItem item : items) {
            long timestamp = normalizeTimestamp(item.getTimestamp());
            if (timestamp > 0) postDays.add(dayKey(timestamp));

            String content = "";
            if (item instanceof FeedItem.NormalPost) {
                content = ((FeedItem.NormalPost) item).description;
            } else if (item instanceof FeedItem.MilestonePost) {
                content = ((FeedItem.MilestonePost) item).description;
            }
            if (content == null) content = "";
            content = content.toLowerCase(Locale.US);

            String type = item.getRawType() != null ? item.getRawType().toLowerCase(Locale.US) : "";
            String category = item.getCategory() != null ? item.getCategory().toLowerCase(Locale.US) : "";

            state.hasFirstPost = true;
            if (isCompletedMilestone(item)) state.hasCompletedMilestone = true;
            if (type.contains("share") || category.contains("share")
                    || content.contains("#tip") || content.contains("#share") || content.contains(" tip ")) {
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

    private void setAchievementVisible(int iconId, boolean visible) {
        View root = getView();
        if (root == null) return;
        View icon = root.findViewById(iconId);
        if (icon == null) return;
        View parent = (View) icon.getParent();
        parent.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isCompletedMilestone(FeedItem item) {
        if (item == null || item.getRawType() == null) return false;
        String type = item.getRawType().toLowerCase(Locale.US);
        if (!type.contains("milestone") && !type.contains("achievement")) return false;
        if (item instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost mp = (FeedItem.MilestonePost) item;
            return Math.max(mp.progress, progressFromMilestoneData(mp.milestoneJson)) >= 100;
        }
        return false;
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
