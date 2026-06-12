package com.example.cashify.ui.social;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cashify.R;
import com.example.cashify.ui.feed.CommunityFeedAdapter;
import com.example.cashify.ui.feed.FeedItem;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
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
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SocialNewsfeedFragment — Tab "Bảng tin".
 * Giống WorkspaceHomeFragment: toolbar nằm ở đây, mở sidebar qua getActivity().
 */
public class SocialNewsfeedFragment extends Fragment {

    private SocialViewModel socialViewModel;
    private final boolean[] likedPosts = new boolean[5];

    // Thêm mới
    private RecyclerView rvFeed;
    private CommunityFeedAdapter feedAdapter;
    private View layoutFeedEmpty;
    private View layoutFeedEnd;
    private View layoutFeedSkeleton;
    private TextView layoutFeedError;
    private ProgressBar progressFeed;
    private ProgressBar progressFeedMore;
    private SwipeRefreshLayout swipeRefreshNewsfeed;
    private NestedScrollView scrollNewsfeed;

    private static final int FEED_PAGE_SIZE = 10;
    private final List<FeedItem> feedItems = new ArrayList<>();
    private final Set<String> loadedPostIds = new HashSet<>();
    private boolean isAdmin = false;
    private boolean isLoadingFeed = false;
    private boolean isRefreshingFeed = false;
    private boolean isLastFeedPage = false;
    private long nextFeedCursor = 0L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_newsfeed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
        initViews(view);
        refreshFeed(false);
    }

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            socialViewModel.loadProfile(user.getUid());
        }
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialNewsfeed);
        View createPostPrompt = view.findViewById(R.id.cardCreatePostPrompt);
        View createPostPromptButton = view.findViewById(R.id.btnCreatePostPrompt);

        // Bind RecyclerView mới (thêm vào layout XML)
        rvFeed = view.findViewById(R.id.rvNewsfeed);
        layoutFeedEmpty = view.findViewById(R.id.layoutNewsfeedEmpty);
        layoutFeedEnd = view.findViewById(R.id.layoutNewsfeedEnd);
        layoutFeedSkeleton = view.findViewById(R.id.layoutNewsfeedSkeleton);
        layoutFeedError = view.findViewById(R.id.layoutNewsfeedError);
        progressFeed = view.findViewById(R.id.progressNewsfeed);
        progressFeedMore = view.findViewById(R.id.progressNewsfeedMore);
        swipeRefreshNewsfeed = view.findViewById(R.id.swipeRefreshNewsfeed);
        scrollNewsfeed = view.findViewById(R.id.scrollNewsfeed);

        if (rvFeed != null) {
            feedAdapter = new CommunityFeedAdapter(
                    item -> {
                        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                        startActivity(intent);
                    },
                    this::showPostBottomSheet
            );

            rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFeed.setAdapter(feedAdapter);
            rvFeed.setHasFixedSize(false);
        }

        if (swipeRefreshNewsfeed != null) {
            swipeRefreshNewsfeed.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.brand_primary));
            swipeRefreshNewsfeed.setOnRefreshListener(() -> refreshFeed(true));
        }

        if (scrollNewsfeed != null) {
            scrollNewsfeed.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        View child = v.getChildAt(0);
                        if (child == null) return;
                        int distanceToBottom = child.getMeasuredHeight() - (scrollY + v.getHeight());
                        if (distanceToBottom <= dp(320)) {
                            loadNextFeedPage();
                        }
                    });
        }

        if (layoutFeedError != null) {
            layoutFeedError.setOnClickListener(v -> refreshFeed(false));
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            }
        });

        createPostPrompt.setOnClickListener(v -> runPressAnimation(
                createPostPromptButton != null ? createPostPromptButton : v,
                this::openCreatePost
        ));
        if (createPostPromptButton != null) {
            createPostPromptButton.setOnClickListener(v -> runPressAnimation(v, this::openCreatePost));
        }
        setupProfileSurfaces(view);

    }

    // =========================================================
    // LOAD FEED THẬT — ĐÃ FIX
    // =========================================================
    private void refreshFeed(boolean fromSwipe) {
        if (isLoadingFeed) {
            if (swipeRefreshNewsfeed != null) swipeRefreshNewsfeed.setRefreshing(false);
            return;
        }
        isRefreshingFeed = fromSwipe;
        isLastFeedPage = false;
        nextFeedCursor = 0L;
        loadedPostIds.clear();
        feedItems.clear();
        if (feedAdapter != null) {
            feedAdapter.clearLikedIds();
            feedAdapter.submitList(new ArrayList<>());
        }
        showFeedEmpty(false);
        showFeedError(false);
        showFeedSkeleton(false);
        loadFriendListAndFeed();
    }

    private void loadNextFeedPage() {
        // Pagination not implemented for Firebase query yet
        // if (isLoadingFeed || isRefreshingFeed || isLastFeedPage || feedItems.isEmpty()) return;
        // loadFeedPageFromFirebase(friendIds, false);
    }

    private void loadFriendListAndFeed() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finishFeedLoading();
            showFeedError(true);
            return;
        }

        isLoadingFeed = true;
        showFeedSkeleton(true);

        // 1. KÉO THÔNG TIN USER TRỰC TIẾP TRƯỚC KHI LOAD FEED (Chống delay 100%)
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    // Cập nhật cờ Admin chắc cú
                    if (userDoc != null && userDoc.exists()) {
                        isAdmin = "ADMIN".equals(userDoc.getString("role"));
                    }

                    if (isAdmin) {
                        // 👑 NẾU LÀ ADMIN: Đi cổng VIP, không thèm tải bạn bè, phi thẳng tới hàm Load
                        loadFeedPageFromFirebase(new ArrayList<>(), true);
                    } else {
                        // 👤 NẾU LÀ USER THƯỜNG: Bắt buộc tải danh sách bạn bè
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("friends")
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    List<String> friendIds = new ArrayList<>();
                                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                        friendIds.add(doc.getId());
                                    }

                                    // ĐÃ XÓA KHÓA CHẶN "KHÔNG CÓ BẠN" Ở ĐÂY.
                                    // Không có bạn thì mảng rỗng, vào trong nó tự nhét UID của mình vào mảng để lấy bài của chính mình!
                                    loadFeedPageFromFirebase(friendIds, true);
                                })
                                .addOnFailureListener(e -> {
                                    finishFeedLoading();
                                    Log.e("FEED", "Lỗi tải danh sách bạn: " + e.getMessage());
                                    showFeedError(true);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    finishFeedLoading();
                    Log.e("FEED", "Lỗi lấy thông tin user: " + e.getMessage());
                    showFeedError(true);
                });
    }

    private void loadFeedPageFromFirebase(List<String> friendIds, boolean firstPage) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Query query = FirebaseFirestore.getInstance().collection("posts");

        // NẾU KHÔNG PHẢI ADMIN THÌ MỚI LỌC BẠN BÈ
        if (!isAdmin) {
            List<String> allUserIds = new ArrayList<>(friendIds);
            allUserIds.add(user.getUid()); // Thêm mình vào để xem bài của mình

            if (allUserIds.size() > 30) {
                allUserIds = allUserIds.subList(0, 30);
            }
            query = query.whereIn("userId", allUserIds);
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(FEED_PAGE_SIZE)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;
                    finishFeedLoading();

                    List<FeedItem> newItems = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FeedItem item = mapFirebaseDocToFeedItem(doc);
                        if (item != null) {
                            newItems.add(item);
                        }
                    }

                    appendFeedItems(newItems);
                    isLastFeedPage = newItems.size() < FEED_PAGE_SIZE;

                    // XỬ LÝ GIAO DIỆN KHI KHÔNG CÓ BÀI
                    if (feedItems.isEmpty()) {
                        showFeedEmpty(true);
                        // Nếu user thường, không có bạn bè, và cũng chưa có bài viết nào
                        if (!isAdmin && friendIds.isEmpty()) {
                            showNoFriendsEmptyState(true);
                        } else {
                            showNoFriendsEmptyState(false);
                        }
                    } else {
                        showFeedEmpty(false);
                        showNoFriendsEmptyState(false);
                    }

                    showFeedError(false);
                    updateFeedEndState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    finishFeedLoading();
                    Log.e("FEED", "Firebase lỗi: " + e.getMessage());
                    if (feedItems.isEmpty()) {
                        showFeedError(true);
                    } else {
                        Toast.makeText(requireContext(), "Cannot load more posts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private FeedItem mapFirebaseDocToFeedItem(DocumentSnapshot doc) {
        String id = doc.getId();
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
        if (authorName == null) authorName = "Người dùng Cashify";

        boolean hasImage = !imageUrl.isEmpty();
        boolean expandable = content.length() > 120;

        FeedItem item = null;

        if (type.toLowerCase().contains("milestone")) {
            String milestoneData = doc.getString("milestoneData") != null ? doc.getString("milestoneData") : "";

            // Khởi tạo các giá trị mặc định phòng trường hợp chuỗi trống
            String milestoneTitle = "Cột mốc mới";
            String milestoneDescription = "";
            String amountText = "";
            String iconText = "🏆"; // Fallback icon
            int progressValue = 0;

            // BÓC TÁCH DATA CHUẨN TỪ CỤC JSON MILESTONE DATA LƯU TRÊN FIRESTORE
            if (!milestoneData.trim().isEmpty()) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(milestoneData);
                    iconText = json.optString("iconText", "🏆");
                    milestoneTitle = json.optString("title", "Cột mốc mới");
                    milestoneDescription = json.optString("description", "");
                    amountText = json.optString("amount", "");
                    progressValue = json.optInt("progress", 0);
                } catch (Exception e) {
                    Log.e("FEED_PARSE", "Lỗi giải mã milestoneData JSON: " + e.getMessage());
                }
            }

            item = new FeedItem.MilestonePost(
                    id,
                    userId,
                    authorName,
                    com.example.cashify.utils.TimeFormatter.format(timestamp),
                    milestoneTitle,
                    // Nếu user có viết thêm caption (content), ưu tiên hiện caption đó lên trước
                    !content.isEmpty() ? content : milestoneDescription,
                    "Cột mốc",
                    amountText,
                    iconText, // TRUYỀN ĐÚNG EMOJI (Ví dụ: "🐝") LẤY TỪ JSON VÀO ĐÂY
                    progressValue,
                    (!content.isEmpty() ? content : milestoneDescription).length() > 120,
                    milestoneData,
                    authorAvatarUrl,
                    initials(authorName)
            );
        } else {
            // Xử lý bài viết thông thường
            item = new FeedItem.NormalPost(
                    id,
                    userId,
                    authorName,
                    com.example.cashify.utils.TimeFormatter.format(timestamp),
                    content,
                    hasImage,
                    imageUrl,
                    initials(authorName),
                    expandable,
                    authorAvatarUrl
            );
        }

        if (item != null) {
            item.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
            item.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
        }

        return item;
    }

    private void appendFeedItems(List<FeedItem> newItems) {
        for (FeedItem item : newItems) {
            if (item == null || item.getId() == null || item.getId().isEmpty()) continue;
            if (loadedPostIds.add(item.getId())) {
                feedItems.add(item);
            }
        }
        if (feedAdapter != null) {
            feedAdapter.submitList(new ArrayList<>(feedItems));
        }
        if (!feedItems.isEmpty()) {
            showFeedSkeleton(false);
            if (rvFeed != null) rvFeed.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateNextCursor(List<Object> rawPosts) {
        // Not used for Firebase query
        if (rawPosts == null || rawPosts.isEmpty()) return;
        Object last = rawPosts.get(rawPosts.size() - 1);
        if (last instanceof Map) {
            long timestamp = num((Map<String, Object>) last, "timestamp");
            if (timestamp > 0) nextFeedCursor = timestamp;
        }
    }

    private void finishFeedLoading() {
        isLoadingFeed = false;
        isRefreshingFeed = false;
        if (progressFeed != null) progressFeed.setVisibility(View.GONE);
        if (progressFeedMore != null) progressFeedMore.setVisibility(View.GONE);
        if (swipeRefreshNewsfeed != null) swipeRefreshNewsfeed.setRefreshing(false);
        showFeedSkeleton(false);
    }

    private void showFeedEmpty(boolean show) {
        if (show) showFeedSkeleton(false);
        if (layoutFeedEmpty != null)
            layoutFeedEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvFeed != null)
            rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) showFeedEnd(false);
    }

    private void showFeedError(boolean show) {
        if (layoutFeedError != null) {
            layoutFeedError.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            showFeedSkeleton(false);
            showFeedEmpty(false);
            showNoFriendsEmptyState(false);
            showFeedEnd(false);
            if (rvFeed != null) rvFeed.setVisibility(View.GONE);
        }
    }

    private void updateFeedEndState() {
        boolean show = isLastFeedPage
                && !feedItems.isEmpty()
                && !isLoadingFeed
                && !isRefreshingFeed
                && layoutFeedError != null
                && layoutFeedError.getVisibility() != View.VISIBLE;
        showFeedEnd(show);
    }

    private void showFeedEnd(boolean show) {
        if (layoutFeedEnd != null) {
            layoutFeedEnd.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showFeedSkeleton(boolean show) {
        if (layoutFeedSkeleton != null) {
            layoutFeedSkeleton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            if (rvFeed != null) rvFeed.setVisibility(View.GONE);
            if (layoutFeedEmpty != null) layoutFeedEmpty.setVisibility(View.GONE);
            if (layoutFeedError != null) layoutFeedError.setVisibility(View.GONE);
            showNoFriendsEmptyState(false);
            showFeedEnd(false);
        }
    }

    private void showNoFriendsEmptyState(boolean show) {
        View layoutNoFriends = getView() != null ? getView().findViewById(R.id.layoutNoFriendsEmpty) : null;
        if (layoutNoFriends != null) {
            layoutNoFriends.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            if (rvFeed != null) rvFeed.setVisibility(View.GONE);
            if (layoutFeedEmpty != null) layoutFeedEmpty.setVisibility(View.GONE);
            if (layoutFeedError != null) layoutFeedError.setVisibility(View.GONE);
            showFeedEnd(false);

            // Setup button to navigate to FriendsActivity
            View btnFindFriends = layoutNoFriends != null ? layoutNoFriends.findViewById(R.id.btnFindFriends) : null;
            if (btnFindFriends != null) {
                btnFindFriends.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), com.example.cashify.ui.FriendsActivity.FriendsActivity.class);
                    startActivity(intent);
                });
            }
        }
    }

    // Helpers
    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : "";
    }

    private long num(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        String f = parts[0].substring(0, 1);
        String s = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (f + s).toUpperCase(Locale.getDefault());
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private boolean sameText(String first, String second) {
        if (first == null || second == null) return false;
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private String jsonString(String json, String key) {
        if (json == null || json.trim().isEmpty()) return "";
        try {
            return new JSONObject(json).optString(key, "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private long jsonLong(String json, String key) {
        if (json == null || json.trim().isEmpty()) return 0L;
        try {
            return new JSONObject(json).optLong(key, 0L);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private long firstPositive(long first, long second) {
        return first > 0 ? first : Math.max(second, 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupDemoPost(View root, int cardId, int likeId, int commentId, int shareId, int index, int baseLikes, String postId) {
        View card = root.findViewById(cardId);
        TextView likeButton = root.findViewById(likeId);
        View commentButton = root.findViewById(commentId);
        View shareButton = root.findViewById(shareId);

        applyReactionState(likeButton, false);
        card.setOnClickListener(v -> runPressAnimation(v, () -> openPostDetail(postId)));
        commentButton.setOnClickListener(v -> runPressAnimation(v, () -> openPostDetail(postId)));
        shareButton.setOnClickListener(v -> runPressAnimation(v,
                () -> Toast.makeText(requireContext(), "Post link copied", Toast.LENGTH_SHORT).show()));
        likeButton.setOnClickListener(v -> {
            likedPosts[index] = !likedPosts[index];
            int count = likedPosts[index] ? baseLikes + 1 : baseLikes;
            likeButton.setText(String.valueOf(count));
            applyReactionState(likeButton, likedPosts[index]);
            likeButton.animate()
                    .scaleX(likedPosts[index] ? 1.08f : 1f)
                    .scaleY(likedPosts[index] ? 1.08f : 1f)
                    .setDuration(90)
                    .withEndAction(() -> likeButton.animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                    .start();
        });
    }

    private void setupProfileSurfaces(View root) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView createAvatar = root.findViewById(R.id.imgCreatePromptAvatar);
        if (user != null) {
            ImageHelper.loadAvatar(user.getPhotoUrl(), createAvatar,
                    firstNonEmpty(user.getDisplayName(), user.getEmail(), user.getUid()));
        }

        FirebaseFirestore.getInstance().collection("users").limit(5).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || snapshot == null || snapshot.isEmpty()) {
                        return;
                    }
                    int index = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        bindPostProfile(root, index, doc);
                        index++;
                        if (index > 5) {
                            break;
                        }
                    }
                });

        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || doc == null || !doc.exists()) {
                            return;
                        }
                        String avatarUrl = doc.getString("avatarUrl");
                        ImageHelper.loadAvatar(avatarUrl, createAvatar,
                                firstNonEmpty(displayNameFromProfile(doc), user.getEmail(), user.getUid()));
                    });
        }
    }

    private void bindPostProfile(View root, int index, DocumentSnapshot doc) {
        TextView nameView = root.findViewById(getResources().getIdentifier(
                "txtPostName" + index, "id", requireContext().getPackageName()));
        ImageView avatarView = root.findViewById(getResources().getIdentifier(
                "imgPostAvatar" + index, "id", requireContext().getPackageName()));
        String name = displayNameFromProfile(doc);
        if (nameView != null && !name.isEmpty()) {
            nameView.setText(name);
        }
        String avatarUrl = doc.getString("avatarUrl");
        if (avatarView != null) {
            ImageHelper.loadAvatar(avatarUrl, avatarView, name);
        }
    }

    private String displayNameFromProfile(DocumentSnapshot doc) {
        String displayName = doc.getString("displayName");
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }
        String username = doc.getString("username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "Cashify User";
    }

    private void applyReactionState(TextView button, boolean active) {
        int textColor = ContextCompat.getColor(requireContext(),
                active ? R.color.status_red : R.color.item_description);
        int iconColor = ContextCompat.getColor(requireContext(),
                active ? R.color.status_red : R.color.icon_inactive);
        button.setTextColor(textColor);
        button.setBackgroundResource(active
                ? R.drawable.bg_social_reaction_chip_active
                : R.drawable.bg_action_button);
        for (Drawable drawable : button.getCompoundDrawablesRelative()) {
            if (drawable != null) {
                Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
                DrawableCompat.setTint(wrapped, iconColor);
            }
        }
        button.setCompoundDrawableTintList(ColorStateList.valueOf(iconColor));
    }

    private void openPostDetail(String postId) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        startActivity(intent);
    }

    private void openCreatePost() {
        if (getActivity() == null) {
            return;
        }

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCreatePostScreen();
            return;
        }

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        if (navController.getGraph().findNode(R.id.nav_post_feed) != null) {
            navController.navigate(R.id.nav_post_feed);
        }
    }

    private void runPressAnimation(View view, Runnable action) {
        view.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .withEndAction(action)
                        .start())
                .start();
    }
    private void showPostBottomSheet(FeedItem item) {
        if (getActivity() == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = user != null ? user.getUid() : "";

        // Check xem có phải bài của mình không
        boolean canEditOrDelete = (!currentUserId.isEmpty() && currentUserId.equals(item.getUserId())) || isAdmin;
        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (canEditOrDelete) {
            if (btnEditPost != null) btnEditPost.setVisibility(View.VISIBLE);
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            // 1. LOGIC XÓA BÀI TRÊN NEWSFEED (ĐÃ CHUYỂN QUA VIEWMODEL)
            btnDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                if (user == null) return;
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    // Gọi ViewModel thay vì dùng Retrofit trực tiếp
                    socialViewModel.deletePost(item.getId(), token);
                }).addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi xác thực", Toast.LENGTH_SHORT).show()
                );
            });

            // 2. LOGIC SỬA BÀI TRÊN NEWSFEED
            if (btnEditPost != null) {
                btnEditPost.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.putExtra("ACTION_EDIT_POST", true);
                    intent.putExtra("edit_post_id", item.getId());

                    if (item instanceof FeedItem.NormalPost) {
                        intent.putExtra("edit_post_content", ((FeedItem.NormalPost) item).text);
                    } else if (item instanceof FeedItem.MilestonePost) {
                        intent.putExtra("edit_post_content", ((FeedItem.MilestonePost) item).title);
                        intent.putExtra("edit_milestone_data", ((FeedItem.MilestonePost) item).milestoneJson);
                    }
                    startActivity(intent);
                });
            }
        } else {
            // NẾU LÀ BÀI CỦA NGƯỜI KHÁC -> CHỈ CHO ẨN HOẶC BÁO CÁO
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            btnDeletePost.setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.VISIBLE);

            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Hided this post", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Reported this post", Toast.LENGTH_SHORT).show();
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }
}
