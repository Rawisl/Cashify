package com.example.cashify.ui.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.cashify.R;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.HeartAnimation;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "postId";

    private ImageView imgLikeHeart;
    private ImageView imgPostAvatar;
    private ImageView imgPostImage;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private TextView tvShareCount;
    private TextView tvPostUsername;
    private TextView tvPostTime;
    private TextView tvPostContent;
    private TextView tvPostDetailMessage;
    private TextView tvEmptyComments;
    private ProgressBar progressPostDetail;
    private CardView cardPostDetail;
    private LinearLayout layoutLikeButton;
    private LinearLayout layoutShareButton;
    private RecyclerView recyclerViewComments;
    private EditText etCommentInput;
    private ImageView imgSendComment;
    private ImageView imgPostMenu;
    private View milestoneContainer;
    private TextView tvPreviewIcon, tvPreviewTitle, tvPreviewMonth, tvPreviewAmount;
    private ProgressBar pbPreviewProgress;
    private ImageView btnRemoveMilestonePreview;

    private final List<Comment> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private PostDetailViewModel viewModel;
    private String postId;
    private String authHeader;
    private String currentUserId = "";
    private String postOwnerId = "";
    private boolean isLiked = false;
    private int likeCount = 0;
    private int commentCount = 0;
    private int shareCount = 0;
    private String globalMilestoneData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user == null ? "" : user.getUid();

        viewModel = new ViewModelProvider(this).get(PostDetailViewModel.class);

        initViews();
        setupToolbar();
        setupComments();
        setupLikeButton();
        setupShareButton();
        setupCommentInput();
        setupPostMenu();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                authHeader = "Bearer " + result.getToken();
                observeViewModel();
                viewModel.loadPost(postId, authHeader);
                viewModel.loadComments(postId, authHeader);
            }).addOnFailureListener(e -> showError("Không lấy được phiên đăng nhập."));
        } else {
            showError("Bạn cần đăng nhập để xem bài viết.");
        }
    }

    private void initViews() {
        imgLikeHeart = findViewById(R.id.imgLikeHeart);
        imgPostAvatar = findViewById(R.id.imgPostAvatar);
        imgPostImage = findViewById(R.id.imgPostImage);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        tvShareCount = findViewById(R.id.tvShareCount);
        tvPostUsername = findViewById(R.id.tvPostUsername);
        tvPostTime = findViewById(R.id.tvPostTime);
        tvPostContent = findViewById(R.id.tvPostContent);
        tvPostDetailMessage = findViewById(R.id.tvPostDetailMessage);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        progressPostDetail = findViewById(R.id.progressPostDetail);
        cardPostDetail = findViewById(R.id.cardPostDetail);
        layoutLikeButton = findViewById(R.id.layoutLikeButton);
        layoutShareButton = findViewById(R.id.layoutShareButton);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        imgSendComment = findViewById(R.id.imgSendComment);
        imgPostMenu = findViewById(R.id.imgPostMenu);
        milestoneContainer = findViewById(R.id.milestoneContainer);

        tvPreviewIcon = milestoneContainer.findViewById(R.id.tvPreviewIcon);
        tvPreviewTitle = milestoneContainer.findViewById(R.id.tvPreviewTitle);
        tvPreviewMonth = milestoneContainer.findViewById(R.id.tvPreviewMonth);
        tvPreviewAmount = milestoneContainer.findViewById(R.id.tvPreviewAmount);
        pbPreviewProgress = milestoneContainer.findViewById(R.id.pbPreviewProgress);
        btnRemoveMilestonePreview = milestoneContainer.findViewById(R.id.btnRemoveMilestonePreview);

        if (btnRemoveMilestonePreview != null) {
            btnRemoveMilestonePreview.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarPostDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupComments() {
        commentAdapter = new CommentAdapter(commentList, new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEditComment(int position) {
                Comment c = commentList.get(position);
                showEditCommentDialog(c);
            }

            @Override
            public void onDeleteComment(int position) {
                Comment c = commentList.get(position);
                viewModel.deleteComment(postId, c.getId(), authHeader);
            }

            @Override
            public void onHideComment(int position) {
                commentList.remove(position);
                commentAdapter.notifyItemRemoved(position);
                updateEmptyComments();
            }
        }, currentUserId, postOwnerId);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void showEditCommentDialog(Comment comment) {
        try {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_comment, null);

            // Ánh xạ View từ layout mới của bác
            EditText etEditComment = view.findViewById(R.id.etEditComment);
            View btnSaveComment = view.findViewById(R.id.btnSaveComment);

            // 1. Đổ nội dung cũ vào EditText
            etEditComment.setText(comment.getContent());
            etEditComment.requestFocus();

            // Đưa con trỏ (nháy chuột) xuống cuối chữ để user tiện viết tiếp
            if (etEditComment.getText() != null) {
                etEditComment.setSelection(etEditComment.getText().length());
            }

            // 2. Lắng nghe nút Lưu
            btnSaveComment.setOnClickListener(v -> {
                String newContent = etEditComment.getText().toString().trim();

                // Chặn không cho lưu bình luận rỗng
                if (newContent.isEmpty()) {
                    etEditComment.setError("Bình luận không được để trống");
                    return;
                }

                // Chặn không cho gọi API nếu nội dung không thay đổi gì
                if (newContent.equals(comment.getContent())) {
                    dialog.dismiss();
                    return;
                }

                // 3. Báo ViewModel gọi API để sửa
                viewModel.editComment(postId, comment.getId(), newContent, authHeader);

                // Đóng menu
                dialog.dismiss();

                Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();
            });

            dialog.setContentView(view);
            dialog.show();

        } catch (Exception e) {
            // Fallback lỡ như layout bị lỗi
            etCommentInput.setText(comment.getContent());
            etCommentInput.requestFocus();
        }
    }

    private void setupLikeButton() {
        layoutLikeButton.setOnClickListener(v -> togglePostLike());
    }

    private void setupShareButton() {
        layoutShareButton.setOnClickListener(v -> sharePost());
    }

    private void setupCommentInput() {
        imgSendComment.setOnClickListener(v -> sendComment());
    }

    private void setupPostMenu() {
        imgPostMenu.setOnClickListener(v -> showPostBottomSheet());
    }

    private void showPostBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        boolean isOwner = !currentUserId.isEmpty() && currentUserId.equals(postOwnerId);

        // Ánh xạ nút sửa bài viết từ layout
        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (isOwner) {
            if (btnEditPost != null) btnEditPost.setVisibility(View.VISIBLE); // Hiện nút sửa lên
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            sheetView.findViewById(R.id.btnDeleteComment).setOnClickListener(v -> {
                dialog.dismiss();
                viewModel.deletePost(postId, authHeader);
            });
            if (btnEditPost != null) {
                btnEditPost.setOnClickListener(v -> {
                    dialog.dismiss();

                    // Bắn dữ liệu sang MainActivity để nó mở khung Composer sửa bài
                    Intent intent = new Intent(PostDetailActivity.this, MainActivity.class);
                    intent.putExtra("ACTION_EDIT_POST", true);
                    intent.putExtra("edit_post_id", postId);
                    intent.putExtra("edit_post_content", tvPostContent.getText().toString().trim());

                    if (globalMilestoneData != null && !globalMilestoneData.isEmpty()) {
                        intent.putExtra("edit_milestone_data", globalMilestoneData);
                    }

                    startActivity(intent);
                });
            }
        } else {
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            btnDeletePost.setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.VISIBLE);

            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });

            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(this, "Post reported", Toast.LENGTH_SHORT).show();
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v ->
                dialog.dismiss()
        );

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::setLoading);
        viewModel.getErrorMessage().observe(this, this::showError);

        viewModel.getPostDetail().observe(this, post -> {
            if (post != null) {
                bindPost(post);
            }
        });

        viewModel.getIsActionSuccess().observe(this, success -> {
            if (success) finish();
        });

        viewModel.getComments().observe(this, list -> {
            if (list == null) return;
            commentList.clear();
            for (Object obj : list) {
                if (!(obj instanceof java.util.Map)) continue;
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                String cId = mapStr(map, "commentId");
                String cUid = mapStr(map, "userId");
                String cAvatar = mapStr(map, "authorAvatarUrl");
                String cName = mapStr(map, "authorName");
                String cContent = mapStr(map, "content");
                long cTs = mapNum(map, "timestamp");
                commentList.add(new Comment(cId, cUid, cAvatar,
                        cName.isEmpty() ? "Người dùng Cashify" : cName,
                        cContent,
                        cTs > 0 ? TimeFormatter.format(cTs) : "Vừa xong", 0));
            }
            commentAdapter.notifyDataSetChanged();
            updateEmptyComments();
        });
    }

    private void bindPost(ApiService.SocialPostDetailResponse post) {
        postOwnerId = post.authorId == null ? "" : post.authorId;
        commentAdapter.updatePostOwnerId(postOwnerId);
        tvPostUsername.setText(nonEmpty(post.authorName, "Người dùng Cashify"));
        tvPostTime.setText(post.timestamp > 0 ? TimeFormatter.format(post.timestamp) : "");
        tvPostContent.setText(nonEmpty(post.content, ""));
        likeCount = Math.max(0, post.likeCount);
        commentCount = Math.max(0, post.commentCount);
        isLiked = post.likedByMe;
        tvLikeCount.setText(String.valueOf(likeCount));
        tvCommentCount.setText(String.valueOf(commentCount));
        updateShareText();
        applyPostLikeState();

        ImageHelper.loadAvatar(post.authorAvatarUrl, imgPostAvatar,
                nonEmpty(post.authorName, nonEmpty(post.authorId, "Người dùng Cashify")));
        if (post.imageUrl != null && !post.imageUrl.trim().isEmpty()) {
            imgPostImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.imageUrl)
                    .placeholder(R.drawable.bg_feed_image_placeholder)
                    .error(R.drawable.bg_feed_image_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .override(1080, 720)
                    .centerCrop()
                    .dontAnimate()
                    .into(imgPostImage);
        } else {
            imgPostImage.setVisibility(View.GONE);
        }
        cardPostDetail.setVisibility(View.VISIBLE);
        tvPostDetailMessage.setVisibility(View.GONE);

        if (post.milestoneData != null && !post.milestoneData.trim().isEmpty()) {
            globalMilestoneData = post.milestoneData;
            try {
                org.json.JSONObject json = new org.json.JSONObject(post.milestoneData);

                tvPreviewIcon.setText(json.optString("iconText", "🏆"));
                tvPreviewTitle.setText(json.optString("title", "Cột mốc"));
                tvPreviewMonth.setText(json.optString("month", ""));
                tvPreviewAmount.setText(json.optString("amount", ""));
                pbPreviewProgress.setProgress(json.optInt("progress", 100));

                milestoneContainer.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                android.util.Log.e("Milestone", "Lỗi parse JSON: " + e.getMessage());
                milestoneContainer.setVisibility(View.GONE);
            }
        } else {
            milestoneContainer.setVisibility(View.GONE);
        }
    }

    private void togglePostLike() {
        if (authHeader == null) return;
        boolean targetLiked = !isLiked;

        isLiked = targetLiked;
        likeCount = targetLiked ? likeCount + 1 : Math.max(0, likeCount - 1);
        tvLikeCount.setText(String.valueOf(likeCount));
        applyPostLikeState();
        if (targetLiked) HeartAnimation.playRubberBand(imgLikeHeart);

        viewModel.toggleLike(postId, authHeader, targetLiked);
    }

    private void sendComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) {
            etCommentInput.setError("Comment first");
            return;
        }
        if (authHeader == null) {
            Toast.makeText(this, "Login first", Toast.LENGTH_SHORT).show();
            return;
        }

        imgSendComment.setEnabled(false);
        etCommentInput.setText("");

        commentList.add(new Comment(null, currentUserId, null, "Bạn", text, "Vừa xong", 0));
        commentAdapter.notifyItemInserted(commentList.size() - 1);
        commentCount++;
        tvCommentCount.setText(String.valueOf(commentCount));
        recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
        updateEmptyComments();

        viewModel.addComment(postId, authHeader, text);

        imgSendComment.setEnabled(true);
    }

    private void sharePost() {
        copyPostLink();
    }

    private void copyPostLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cashify post", "cashify://posts/" + postId));
        }
        Toast.makeText(this, "Post link copied", Toast.LENGTH_SHORT).show();
    }

    private void applyPostLikeState() {
        int color = ContextCompat.getColor(this, isLiked ? R.color.status_red : R.color.icon_inactive);
        int textColor = ContextCompat.getColor(this, isLiked ? R.color.status_red : R.color.item_description);
        layoutLikeButton.setBackgroundResource(isLiked ? R.drawable.bg_social_reaction_chip_active : R.drawable.bg_action_button);
        imgLikeHeart.setImageTintList(ColorStateList.valueOf(color));
        tvLikeCount.setTextColor(textColor);
        if (imgLikeHeart.getDrawable() != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(imgLikeHeart.getDrawable()).mutate(), color);
        }
    }

    private void updateShareText() {
        tvShareCount.setText(shareCount > 0 ? String.valueOf(shareCount) : "Chia sẻ");
    }

    private void setLoading(boolean loading) {
        progressPostDetail.setVisibility(loading ? View.VISIBLE : View.GONE);
        cardPostDetail.setVisibility(loading ? View.GONE : View.VISIBLE);
        recyclerViewComments.setVisibility(loading ? View.GONE : View.VISIBLE);
        tvEmptyComments.setVisibility(View.GONE);
        etCommentInput.setEnabled(!loading);
        imgSendComment.setEnabled(!loading);
    }

    private void showError(String message) {
        setLoading(false);
        cardPostDetail.setVisibility(View.GONE);
        recyclerViewComments.setVisibility(View.GONE);
        tvEmptyComments.setVisibility(View.GONE);
        tvPostDetailMessage.setText(message);
        tvPostDetailMessage.setVisibility(View.VISIBLE);
        etCommentInput.setEnabled(false);
        imgSendComment.setEnabled(false);
    }

    private void updateEmptyComments() {
        boolean empty = commentList.isEmpty();
        recyclerViewComments.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyComments.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String mapStr(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : "";
    }

    private long mapNum(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}