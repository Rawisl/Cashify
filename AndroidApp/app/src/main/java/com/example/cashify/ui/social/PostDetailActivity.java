package com.example.cashify.ui.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.ui.main.MainActivity;
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

    private ImageView imgLikeHeart, imgPostAvatar, imgPostImage, imgSendComment, imgPostMenu;
    private TextView tvLikeCount, tvCommentCount, tvPostUsername, tvPostTime, tvPostContent, tvPostDetailMessage, tvEmptyComments;
    private ProgressBar progressPostDetail;
    private CardView cardPostDetail;
    private LinearLayout layoutLikeButton, layoutShareButton;
    private RecyclerView recyclerViewComments;
    private EditText etCommentInput;

    private final List<Comment> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private PostDetailViewModel viewModel;

    private String postId;
    private String authHeader;
    private String currentUserId = "";
    private String postOwnerId = "";
    private boolean isLiked = false;
    private int likeCount = 0;

    // THÊM BIẾN NÀY ĐỂ LƯU TRỮ MILESTONE DATA
    private String currentMilestoneData = null;

    private View milestoneContainer;
    private TextView tvMilestoneIcon, tvMilestoneTitle, tvMilestoneMonth, tvMilestoneAmount;
    private ProgressBar pbMilestoneProgress;

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

        observeViewModel();
        getTokenAndLoad();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::setLoading);

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) showError(msg);
        });

        viewModel.getIsActionSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Vẽ UI Post
        // Vẽ UI Post
        viewModel.getPostDetail().observe(this, post -> {
            if (post == null) return;
            postOwnerId = post.authorId == null ? "" : post.authorId;
            commentAdapter.updatePostOwnerId(postOwnerId);

            currentMilestoneData = post.milestoneData;

            tvPostUsername.setText(nonEmpty(post.authorName, "Người dùng Cashify"));
            tvPostTime.setText(post.timestamp > 0 ? TimeFormatter.format(post.timestamp) : "");
            tvPostContent.setText(nonEmpty(post.content, ""));

            likeCount = Math.max(0, post.likeCount);
            isLiked = post.likedByMe;
            tvLikeCount.setText(String.valueOf(likeCount));
            tvCommentCount.setText(String.valueOf(Math.max(0, post.commentCount)));

            applyPostLikeState();

            if (post.authorAvatarUrl != null && !post.authorAvatarUrl.trim().isEmpty()) {
                ImageHelper.loadAvatar(post.authorAvatarUrl, imgPostAvatar);
            }

            // ==============================================================
            // PHÂN LOẠI HIỂN THỊ: CỘT MỐC HAY BÀI BÌNH THƯỜNG?
            // ==============================================================
            boolean isMilestone = post.type != null && (post.type.contains("MILESTONE") || post.type.contains("ACHIEVEMENT"));

            if (isMilestone && currentMilestoneData != null && !currentMilestoneData.isEmpty()) {
                // CHẾ ĐỘ 1: BÀI CỘT MỐC (Hiện Card, Ẩn Ảnh)
                milestoneContainer.setVisibility(View.VISIBLE);
                imgPostImage.setVisibility(View.GONE);
                try {
                    org.json.JSONObject json = new org.json.JSONObject(currentMilestoneData);
                    tvMilestoneIcon.setText(json.optString("iconText", "🏆"));
                    tvMilestoneTitle.setText(json.optString("title", "Cột mốc"));
                    tvMilestoneMonth.setText(json.optString("month", ""));
                    tvMilestoneAmount.setText(json.optString("amount", ""));
                    pbMilestoneProgress.setProgress(json.optInt("progress", 0));
                } catch (Exception e) { e.printStackTrace(); }

            } else {
                // CHẾ ĐỘ 2: BÀI BÌNH THƯỜNG (Ẩn Card, Hiện Ảnh Nếu Có)
                milestoneContainer.setVisibility(View.GONE);

                if (post.imageUrl != null && !post.imageUrl.trim().isEmpty()) {
                    imgPostImage.setVisibility(View.VISIBLE);
                    Glide.with(this).load(post.imageUrl).into(imgPostImage);
                } else {
                    imgPostImage.setVisibility(View.GONE);
                }
            }

            cardPostDetail.setVisibility(View.VISIBLE);
            tvPostDetailMessage.setVisibility(View.GONE);
        });

        // Vẽ danh sách Comments
        viewModel.getComments().observe(this, rawList -> {
            commentList.clear();
            for (Object obj : rawList) {
                if (!(obj instanceof java.util.Map)) continue;
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                String cId = mapStr(map, "commentId");
                String cUid = mapStr(map, "userId");
                String cAvatar = mapStr(map, "authorAvatarUrl");
                String cName = mapStr(map, "authorName");
                String cContent = mapStr(map, "content");
                long cTs = mapNum(map, "timestamp");
                commentList.add(new Comment(cId, cUid, cAvatar, cName.isEmpty() ? "Người dùng Cashify" : cName, cContent, cTs > 0 ? TimeFormatter.format(cTs) : "Vừa xong", 0));
            }
            commentAdapter.notifyDataSetChanged();
            updateEmptyComments();
        });
    }

    private void getTokenAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || postId == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {
            authHeader = "Bearer " + result.getToken();
            viewModel.loadPost(postId, authHeader);
            viewModel.loadComments(postId, authHeader);
        });
    }

    private void setupComments() {
        commentAdapter = new CommentAdapter(commentList, new CommentAdapter.OnCommentActionListener() {
            @Override public void onEditComment(int position) {
                Comment c = commentList.get(position);
                showEditCommentDialog(c);
            }
            @Override public void onDeleteComment(int position) {
                Comment c = commentList.get(position);
                viewModel.deleteComment(postId, c.getId(), authHeader);
            }
            @Override public void onHideComment(int position) {
                commentList.remove(position);
                commentAdapter.notifyItemRemoved(position);
                updateEmptyComments();
            }
        }, currentUserId, postOwnerId);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void showEditCommentDialog(Comment comment) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_comment, null);

        EditText etInput = view.findViewById(R.id.etEditComment);
        etInput.setText(comment.getContent());
        etInput.requestFocus();

        view.findViewById(R.id.btnSaveComment).setOnClickListener(v -> {
            String newText = etInput.getText().toString().trim();
            if (!newText.isEmpty()) {
                viewModel.editComment(postId, comment.getId(), newText, authHeader);
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void togglePostLike() {
        if (authHeader == null) return;
        isLiked = !isLiked;
        likeCount = isLiked ? likeCount + 1 : Math.max(0, likeCount - 1);
        tvLikeCount.setText(String.valueOf(likeCount));
        applyPostLikeState();
        if (isLiked) HeartAnimation.playRubberBand(imgLikeHeart);

        viewModel.toggleLike(postId, authHeader, isLiked);
    }

    private void sendComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty() || authHeader == null) return;

        etCommentInput.setText("");
        viewModel.addComment(postId, authHeader, text);
    }

    private void showPostBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);
        boolean isOwner = !currentUserId.isEmpty() && currentUserId.equals(postOwnerId);

        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (isOwner) {
            btnEditPost.setVisibility(View.VISIBLE);
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            btnDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                if (authHeader != null) viewModel.deletePost(postId, authHeader);
            });

            btnEditPost.setOnClickListener(v -> {
                dialog.dismiss();

                android.content.Intent intent = new android.content.Intent(PostDetailActivity.this, MainActivity.class);

                intent.putExtra("ACTION_EDIT_POST", true);
                intent.putExtra("edit_post_id", postId);
                intent.putExtra("edit_post_content", tvPostContent.getText().toString());

                // NÉM MILESTONE DATA VÀO ĐÂY
                if (currentMilestoneData != null && !currentMilestoneData.isEmpty()) {
                    intent.putExtra("edit_milestone_data", currentMilestoneData);
                }

                startActivity(intent);
                finish();
            });
        } else {
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            btnDeletePost.setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.VISIBLE);

            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> { dialog.dismiss(); finish(); });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> { dialog.dismiss(); Toast.makeText(this, "Đã báo cáo", Toast.LENGTH_SHORT).show(); });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void initViews() {
        imgLikeHeart = findViewById(R.id.imgLikeHeart); imgPostAvatar = findViewById(R.id.imgPostAvatar);
        imgPostImage = findViewById(R.id.imgPostImage); tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount); tvPostUsername = findViewById(R.id.tvPostUsername);
        tvPostTime = findViewById(R.id.tvPostTime); tvPostContent = findViewById(R.id.tvPostContent);
        tvPostDetailMessage = findViewById(R.id.tvPostDetailMessage); tvEmptyComments = findViewById(R.id.tvEmptyComments);
        progressPostDetail = findViewById(R.id.progressPostDetail); cardPostDetail = findViewById(R.id.cardPostDetail);
        layoutLikeButton = findViewById(R.id.layoutLikeButton); layoutShareButton = findViewById(R.id.layoutShareButton);
        recyclerViewComments = findViewById(R.id.recyclerViewComments); etCommentInput = findViewById(R.id.etCommentInput);
        imgSendComment = findViewById(R.id.imgSendComment); imgPostMenu = findViewById(R.id.imgPostMenu);
        milestoneContainer = findViewById(R.id.milestoneContainer);
        tvMilestoneIcon = findViewById(R.id.tvMilestoneIcon);
        tvMilestoneTitle = findViewById(R.id.tvMilestoneTitle);
        tvMilestoneMonth = findViewById(R.id.tvMilestoneMonth);
        tvMilestoneAmount = findViewById(R.id.tvMilestoneAmount);
        pbMilestoneProgress = findViewById(R.id.pbMilestoneProgress);
    }
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarPostDetail); setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); getSupportActionBar().setTitle(""); }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    private void setupLikeButton() { layoutLikeButton.setOnClickListener(v -> togglePostLike()); }
    private void setupShareButton() { layoutShareButton.setOnClickListener(v -> copyPostLink()); }
    private void setupCommentInput() { imgSendComment.setOnClickListener(v -> sendComment()); }
    private void setupPostMenu() { imgPostMenu.setOnClickListener(v -> showPostBottomSheet()); }
    private void copyPostLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Cashify post", "cashify://posts/" + postId));
        Toast.makeText(this, "Đã sao chép liên kết bài viết", Toast.LENGTH_SHORT).show();
    }
    private void applyPostLikeState() {
        int color = ContextCompat.getColor(this, isLiked ? R.color.status_red : R.color.icon_inactive);
        int textColor = ContextCompat.getColor(this, isLiked ? R.color.status_red : R.color.item_description);
        layoutLikeButton.setBackgroundResource(isLiked ? R.drawable.bg_social_reaction_chip_active : R.drawable.bg_action_button);
        imgLikeHeart.setImageTintList(ColorStateList.valueOf(color));
        tvLikeCount.setTextColor(textColor);
        if (imgLikeHeart.getDrawable() != null) DrawableCompat.setTint(DrawableCompat.wrap(imgLikeHeart.getDrawable()).mutate(), color);
    }
    private void setLoading(boolean loading) {
        progressPostDetail.setVisibility(loading ? View.VISIBLE : View.GONE);
        cardPostDetail.setVisibility(loading ? View.GONE : View.VISIBLE);
        recyclerViewComments.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
    private void showError(String message) {
        setLoading(false); cardPostDetail.setVisibility(View.GONE); recyclerViewComments.setVisibility(View.GONE);
        tvEmptyComments.setVisibility(View.GONE); tvPostDetailMessage.setText(message); tvPostDetailMessage.setVisibility(View.VISIBLE);
    }
    private void updateEmptyComments() {
        boolean empty = commentList.isEmpty();
        recyclerViewComments.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyComments.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
    private String nonEmpty(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    private String mapStr(java.util.Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof String ? (String) v : ""; }
    private long mapNum(java.util.Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof Number ? ((Number) v).longValue() : 0L; }
    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; } return super.onOptionsItemSelected(item); }
}