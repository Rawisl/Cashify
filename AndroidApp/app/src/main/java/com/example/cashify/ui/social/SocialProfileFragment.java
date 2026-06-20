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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.common.BaseFragment;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.ui.notifications.InvitationsActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
    public static String syncedPostId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (syncedPostId != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    viewModel.syncSinglePost(syncedPostId, token);
                    syncedPostId = null; // Update xong thì dọn rác
                });
            } else {
                syncedPostId = null;
            }
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
        View bellIcon = view.findViewById(R.id.imgBellIcon);
        TextView bellBadge = view.findViewById(R.id.tvBellBadge);
        setupCommonHeader(toolbar, bellIcon, bellBadge);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SocialProfileViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            viewModel.loadProfileData(currentUserId);

            // YÊU CẦU VIEWMODEL TẢI HUY HIỆU
            viewModel.loadAchievements(currentUserId);

            currentUser.getIdToken(true).addOnSuccessListener(result -> {
                viewModel.loadWallPosts(currentUserId, "Bearer " + result.getToken());
            });
        } else {
            showEmptyState(true);
        }
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        view.findViewById(R.id.btnShareProfile).setOnClickListener(v -> shareProfile());
        view.findViewById(R.id.txtProfileLink).setOnClickListener(v -> copyProfileLink());

        View.OnClickListener createPostListener = v -> openCreatePost("thoughts");
        view.findViewById(R.id.btnStartGrowing).setOnClickListener(createPostListener);
        view.findViewById(R.id.actionFirstEntry).setOnClickListener(createPostListener);
        view.findViewById(R.id.actionSetMilestone).setOnClickListener(v -> openCreatePost("milestone"));
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
                user.getIdToken(true).addOnSuccessListener(result -> {
                    viewModel.toggleLike(postId, "Bearer " + result.getToken(), isLiked);
                    callback.onResult(true);
                }).addOnFailureListener(e -> callback.onResult(false));
            } else callback.onResult(false);
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
        viewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            if (doc == null) return;
            String displayName = firstNonEmpty(doc.getString("displayName"), doc.getString("username"), "Cashify User");
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"), doc.getString("about"), "Ready to share your finance journey.");

            tvDisplayName.setText(displayName);
            tvBio.setText(bio);
            tvJoinedDate.setText(joinedLabel(doc));

            Object streakObj = doc.get("streakDays");
            long streak = streakObj instanceof Number ? ((Number) streakObj).longValue() : 0;
            tvStreakCount.setText(Math.max(0, streak) + " days");

            ImageHelper.loadAvatar(doc.getString("avatarUrl"), imgAvatar, firstNonEmpty(displayName, currentUserId));
        });

        viewModel.getFriendCount().observe(getViewLifecycleOwner(), count -> tvFriendCount.setText(Math.max(0, count) + " friends"));

        // LẮNG NGHE LIVEDATA HUY HIỆU TỪ VIEWMODEL
        viewModel.getAchievements().observe(getViewLifecycleOwner(), badges -> {
            if (badges == null) return;
            tvTrophyCount.setText(badges.size() + " Trophies");
            badgeAdapter.updateData(badges);
        });

        viewModel.getWallPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts == null) return;
            myPostsAdapter.submitList(new ArrayList<>(posts));
            tvPostCount.setText(posts.size() + " posts");

            FeedItem firstAchievement = null;
            for (FeedItem item : posts) {
                if (item instanceof FeedItem.MilestonePost) {
                    if (firstAchievement == null) firstAchievement = item;
                    break;
                }
            }

            bindPinnedAchievement(firstAchievement, posts.size());
            showEmptyState(posts.isEmpty());

            if (layoutActionCards != null) {
                layoutActionCards.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                viewModel.resetDeleteStatus();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    // =========================================================================
    // INTERACTIONS & UTILITIES
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
                    viewModel.deletePost(item.getId(), "Bearer " + result.getToken());
                });
            });

            btnEditPost.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra("ACTION_EDIT_POST", true);
                intent.putExtra("edit_post_id", item.getId());
                if (item instanceof FeedItem.NormalPost) {
                    intent.putExtra("edit_post_title", ((FeedItem.NormalPost) item).title);
                    intent.putExtra("edit_post_content", ((FeedItem.NormalPost) item).description);
                    intent.putExtra("edit_post_image", ((FeedItem.NormalPost) item).imageUrl);

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

    private void bindPinnedAchievement(@Nullable FeedItem achievement, int postCount) {
        if (achievement instanceof FeedItem.MilestonePost) {
            FeedItem.MilestonePost milestone = (FeedItem.MilestonePost) achievement;

            String displayTitle = milestone.title;
            String displayAmount = milestone.amount;

            if (displayTitle == null || displayTitle.trim().isEmpty() || displayTitle.equals("New Milestone")) {
                displayTitle = "Financial Milestone";
            }

            String text = displayTitle;
            if (displayAmount != null && !displayAmount.trim().isEmpty() && !displayAmount.equals("null")) {
                text += " · " + displayAmount;
            }

            tvPinnedAchievement.setText(text);
            return;
        }

        tvPinnedAchievement.setText(postCount > 0 ? "Started building your personal financial house." : "Share your first milestone to pin a highlighted achievement here.");
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void shareProfile() {
        String name = tvDisplayName.getText().toString().trim();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, (name.isEmpty() ? "Cashify Profile" : name) + "\n" + profileLink() + "\nCheck out my financial journey on Cashify.");
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
        if (requireActivity() instanceof MainActivity) ((MainActivity) requireActivity()).openCreatePostScreen(categoryKey);
    }

    private String profileLink() {
        return currentUserId == null || currentUserId.trim().isEmpty() ? "cashify.vn/profile" : "cashify.vn/profile/" + currentUserId;
    }

    private String joinedLabel(DocumentSnapshot doc) {
        Object createdObj = doc.get("createdAt");
        if (createdObj == null) createdObj = doc.get("joinedAt");
        long createdAt = createdObj instanceof Number ? ((Number) createdObj).longValue() : 0;
        return createdAt <= 0 ? "Cashify Member" : "Joined " + new SimpleDateFormat("MM/yyyy", Locale.ENGLISH).format(new Date(createdAt));
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "";
    }
}