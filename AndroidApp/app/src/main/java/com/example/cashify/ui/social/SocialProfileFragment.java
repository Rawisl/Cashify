package com.example.cashify.ui.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private ListenerRegistration postsRegistration;
    private String currentUserId = "";

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
        setupActions();
        setupRecyclerView();
        observeViewModel();
        loadMyPosts();
    }

    @Override
    public void onDestroyView() {
        if (postsRegistration != null) {
            postsRegistration.remove();
            postsRegistration = null;
        }
        super.onDestroyView();
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
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() == null) {
                return;
            }
            androidx.drawerlayout.widget.DrawerLayout drawer =
                    getActivity().findViewById(R.id.drawerLayout);
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
            if (doc == null) {
                return;
            }

            String displayName = firstNonEmpty(doc.getString("displayName"), doc.getString("username"), "Người dùng Cashify");
            String bio = firstNonEmpty(doc.getString("bio"), doc.getString("status"), doc.getString("about"),
                    "Sẵn sàng chia sẻ hành trình tài chính.");
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

    private void loadMyPosts() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            showEmptyState(true);
            return;
        }

        postsRegistration = FirebaseFirestore.getInstance()
                .collection("posts")
                .whereEqualTo("authorId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (error != null || snapshot == null) {
                        showEmptyState(true);
                        return;
                    }

                    List<FeedItem> posts = new ArrayList<>();
                    int achievementCount = 0;
                    FeedItem firstAchievement = null;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        FeedItem item = mapPost(doc);
                        posts.add(item);
                        if (item instanceof FeedItem.MilestonePost) {
                            achievementCount++;
                            if (firstAchievement == null) {
                                firstAchievement = item;
                            }
                        }
                    }

                    myPostsAdapter.submitList(posts);
                    tvPostCount.setText(posts.size() + " bài viết");
                    tvTrophyCount.setText(achievementCount + " thành tựu");
                    bindPinnedAchievement(firstAchievement, posts.size());
                    showEmptyState(posts.isEmpty());
                });
    }

    private FeedItem mapPost(DocumentSnapshot doc) {
        String id = doc.getId();
        String content = firstNonEmpty(doc.getString("content"), doc.getString("text"), "");
        String name = firstNonEmpty(doc.getString("authorName"), doc.getString("displayName"), "Bạn");
        long timestamp = numberField(doc, "timestamp", 0);
        String imageUrl = doc.getString("imageUrl");
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean expandable = content.length() > 120;
        String type = firstNonEmpty(doc.getString("type"), doc.getString("postType"), "").toLowerCase(Locale.US);

        if (type.contains("milestone") || type.contains("achievement") || doc.contains("progress")) {
            int progress = (int) Math.max(0, Math.min(100, numberField(doc, "progress", 100)));
            String title = firstNonEmpty(doc.getString("title"), achievementTitle(content));
            String amount = firstNonEmpty(doc.getString("amountText"), doc.getString("amount"), progress + "% hoàn thành");
            String month = firstNonEmpty(doc.getString("period"), doc.getString("month"), "Thành tựu");
            return new FeedItem.MilestonePost(id, title, content, month, amount, progress + "%", progress, expandable);
        }

        return new FeedItem.NormalPost(
                id,
                name,
                formatTime(timestamp),
                content,
                hasImage,
                avatarColor(name),
                initials(name),
                expandable
        );
    }

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
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String joinedLabel(DocumentSnapshot doc) {
        long createdAt = numberField(doc, "createdAt", 0);
        if (createdAt <= 0) {
            createdAt = numberField(doc, "joinedAt", 0);
        }
        if (createdAt <= 0) {
            return "Thành viên Cashify";
        }
        return "Tham gia " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    private String achievementTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Cột mốc tài chính mới";
        }
        return content.length() > 54 ? content.substring(0, 54).trim() + "..." : content.trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private long numberField(DocumentSnapshot doc, String field, long fallback) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "Vừa xong";
        }
        return com.example.cashify.utils.TimeFormatter.format(timestamp);
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "CF";
        }
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
