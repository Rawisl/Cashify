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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.feed.CommunityFeedAdapter;
import com.example.cashify.ui.feed.FeedItem;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SocialNewsfeedFragment extends Fragment {

    private SocialViewModel socialViewModel;
    private final boolean[] likedPosts = new boolean[5];

    private RecyclerView rvFeed;
    private CommunityFeedAdapter feedAdapter;
    private View layoutFeedEmpty;
    private ProgressBar progressFeed;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRealFeed();
    }

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialNewsfeed);
        FloatingActionButton fabCreatePost = view.findViewById(R.id.fabCreatePost);
        View createPostPrompt = view.findViewById(R.id.cardCreatePostPrompt);

        rvFeed = view.findViewById(R.id.rvNewsfeed);
        layoutFeedEmpty = view.findViewById(R.id.layoutNewsfeedEmpty);
        progressFeed = view.findViewById(R.id.progressNewsfeed);

        if (rvFeed != null) {
            feedAdapter = new CommunityFeedAdapter(
                    item -> {
                        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                        startActivity(intent);
                    },
                    this::showPostBottomSheet // Gọi Menu
            );
            rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFeed.setAdapter(feedAdapter);
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            }
        });

        fabCreatePost.setOnClickListener(v -> runPressAnimation(v, this::openCreatePost));
        createPostPrompt.setOnClickListener(v -> runPressAnimation(v, this::openCreatePost));
        setupProfileSurfaces(view);
    }

    private void showPostBottomSheet(FeedItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // CLEAN CODE: Nhờ có FeedItem mới, check Owner chỉ tốn 1 dòng!
        boolean isOwner = !currentUserId.isEmpty() && currentUserId.equals(item.getUserId());

        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (isOwner) {
            btnEditPost.setVisibility(View.VISIBLE);
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            // LOGIC XÓA BÀI VIẾT BẰNG API
            btnDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                if (progressFeed != null) progressFeed.setVisibility(View.VISIBLE);

                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    ApiClient.getClient().create(ApiService.class)
                            .deletePost(token, new ApiService.DeletePostRequest(item.getId()))
                            .enqueue(new Callback<Object>() {
                                @Override
                                public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(requireContext(), "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                                        loadRealFeed(); // Xóa xong tự động load lại feed
                                    } else {
                                        if (progressFeed != null) progressFeed.setVisibility(View.GONE);
                                        Toast.makeText(requireContext(), "Lỗi xóa bài", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                                    if (progressFeed != null) progressFeed.setVisibility(View.GONE);
                                    Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
            });

            // LOGIC SỬA BÀI VIẾT NÉM QUA COMPOSER
            btnEditPost.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra("ACTION_EDIT_POST", true);
                intent.putExtra("edit_post_id", item.getId());

                if (item instanceof FeedItem.NormalPost) {
                    intent.putExtra("edit_post_content", ((FeedItem.NormalPost) item).text);
                } else if (item instanceof FeedItem.MilestonePost) {
                    intent.putExtra("edit_post_content", ((FeedItem.MilestonePost) item).description);
                    intent.putExtra("edit_milestone_data", ((FeedItem.MilestonePost) item).milestoneJson); // Truyền JSON thật
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
                Toast.makeText(requireContext(), "Đã ẩn bài viết", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Đã báo cáo bài viết", Toast.LENGTH_SHORT).show();
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void loadRealFeed() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (progressFeed != null) progressFeed.setVisibility(View.VISIBLE);

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            ApiService.FeedRequest req = new ApiService.FeedRequest(20, 0, "PUBLIC");

            apiService.getFeed(token, req).enqueue(new Callback<List<Object>>() {
                @Override
                public void onResponse(@NonNull Call<List<Object>> call,
                                       @NonNull Response<List<Object>> response) {
                    if (!isAdded()) return;
                    if (progressFeed != null) progressFeed.setVisibility(View.GONE);

                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e("FEED", "API lỗi: " + response.code());
                        showFeedEmpty(true);
                        return;
                    }

                    List<FeedItem> items = mapResponseToFeedItems(response.body());
                    if (feedAdapter != null) feedAdapter.submitList(items);
                    showFeedEmpty(items.isEmpty());
                }

                @Override
                public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    if (progressFeed != null) progressFeed.setVisibility(View.GONE);
                    Log.e("FEED", "Mạng lỗi: " + t.getMessage());
                    showFeedEmpty(true);
                }
            });
        }).addOnFailureListener(e -> {
            if (progressFeed != null) progressFeed.setVisibility(View.GONE);
            Log.e("FEED", "Token lỗi: " + e.getMessage());
        });
    }

    @SuppressWarnings("unchecked")
    private List<FeedItem> mapResponseToFeedItems(List<Object> raw) {
        List<FeedItem> result = new ArrayList<>();
        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) obj;

            String id       = str(map, "postId");
            String content  = str(map, "content");
            String imageUrl = str(map, "imageUrl");

            String type = str(map, "type");
            if (type.isEmpty()) type = str(map, "kind");

            long timestamp  = num(map, "timestamp");
            String userId   = str(map, "userId"); // LẤY USER ID TỪ JSON
            String name     = str(map, "authorName");
            String avatarUrl = str(map, "authorAvatarUrl");

            boolean isLiked = Boolean.TRUE.equals(map.get("isLiked"));
            boolean hasImage   = !imageUrl.isEmpty();
            boolean expandable = content.length() > 120;

            if (isLiked) feedAdapter.addLikedId(id);

            if (type.toLowerCase().contains("milestone")) {
                String mIconText = "🏆";
                String mTitle = "Thành tựu mới";
                String mDesc = content;
                String mMonth = "Kỳ này";
                String mAmount = "";
                int mProgress = 0;
                String milestoneDataStr = str(map, "milestoneData");

                if (!milestoneDataStr.isEmpty()) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(milestoneDataStr);
                        mIconText = json.optString("iconText", mIconText);
                        mTitle    = json.optString("title", mTitle);
                        mMonth    = json.optString("month", mMonth);
                        mAmount   = json.optString("amount", mAmount);
                        mProgress = json.optInt("progress", 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("FEED", "Lỗi bóc JSON Milestone: " + e.getMessage());
                    }
                }

                // NHÉT THÊM USER ID VÀ MILESTONE DATA STR
                result.add(new FeedItem.MilestonePost(
                        id, userId, mTitle, mDesc, mMonth, mAmount, mIconText, mProgress, expandable, milestoneDataStr
                ));

            } else {
                // NHÉT THÊM USER ID
                result.add(new FeedItem.NormalPost(
                        id, userId,
                        name.isEmpty() ? "Người dùng Cashify" : name,
                        com.example.cashify.utils.TimeFormatter.format(timestamp),
                        content,
                        hasImage,
                        imageUrl,
                        ContextCompat.getColor(requireContext(), R.color.brand_primary),
                        initials(name),
                        expandable,
                        avatarUrl
                ));
            }
        }
        return result;
    }

    private void showFeedEmpty(boolean show) {
        if (layoutFeedEmpty != null)
            layoutFeedEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvFeed != null)
            rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
    }

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

    private void setupDemoPost(View root, int cardId, int likeId, int commentId, int shareId, int index, int baseLikes, String postId) {
        View card = root.findViewById(cardId);
        TextView likeButton = root.findViewById(likeId);
        View commentButton = root.findViewById(commentId);
        View shareButton = root.findViewById(shareId);

        applyReactionState(likeButton, false);
        card.setOnClickListener(v -> runPressAnimation(v, () -> openPostDetail(postId)));
        commentButton.setOnClickListener(v -> runPressAnimation(v, () -> openPostDetail(postId)));
        shareButton.setOnClickListener(v -> runPressAnimation(v,
                () -> Toast.makeText(requireContext(), "Đã sao chép liên kết bài viết", Toast.LENGTH_SHORT).show()));
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
        if (user != null && user.getPhotoUrl() != null) {
            ImageHelper.loadAvatar(user.getPhotoUrl(), createAvatar);
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
                        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                            ImageHelper.loadAvatar(avatarUrl, createAvatar);
                        }
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
        if (avatarView != null && avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            ImageHelper.loadAvatar(avatarUrl, avatarView);
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
        return "Người dùng Cashify";
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

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.nav_post_feed) {
            return;
        }
        navController.navigate(R.id.nav_post_feed);
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
}