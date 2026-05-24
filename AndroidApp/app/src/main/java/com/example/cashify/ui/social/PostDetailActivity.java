package com.example.cashify.ui.social;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    private final List<Comment> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private ApiService apiService;
    private String postId;
    private String authHeader;
    private String currentUserId = "";
    private String postOwnerId = "";
    private boolean isLiked = false;
    private int likeCount = 0;
    private int commentCount = 0;
    private int shareCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        apiService = ApiClient.getClient().create(ApiService.class);
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user == null ? "" : user.getUid();

        initViews();
        setupToolbar();
        setupComments();
        setupLikeButton();
        setupShareButton();
        setupCommentInput();
        setupPostMenu();
        loadPostDetail();
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
        commentAdapter = new CommentAdapter(
                commentList,
                new CommentAdapter.OnCommentActionListener() {
                    @Override public void onEditComment(int position) {
                        Toast.makeText(PostDetailActivity.this, "Chỉnh sửa bình luận chưa được hỗ trợ", Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onDeleteComment(int position) {
                        Toast.makeText(PostDetailActivity.this, "Xóa bình luận chưa được hỗ trợ", Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onHideComment(int position) {
                        commentList.remove(position);
                        commentAdapter.notifyItemRemoved(position);
                        updateEmptyComments();
                    }
                },
                currentUserId,
                postOwnerId
        );
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void setupLikeButton() {
        layoutLikeButton.setOnClickListener(v -> togglePostLike());
        imgLikeHeart.setOnClickListener(v -> togglePostLike());
    }

    private void setupShareButton() {
        layoutShareButton.setOnClickListener(v -> sharePost());
    }

    private void setupCommentInput() {
        imgSendComment.setOnClickListener(v -> sendComment());
    }

    private void setupPostMenu() {
        imgPostMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, imgPostMenu);
            popup.getMenuInflater().inflate(R.menu.menu_post, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_hide_post || id == R.id.action_delete_post) {
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void loadPostDetail() {
        if (postId == null || postId.trim().isEmpty()) {
            showError("Không tìm thấy mã bài viết.");
            return;
        }

        setLoading(true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showError("Bạn cần đăng nhập để xem bài viết.");
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            authHeader = "Bearer " + result.getToken();
            apiService.getPostDetail(postId, authHeader).enqueue(new Callback<ApiService.SocialPostDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiService.SocialPostDetailResponse> call,
                                       @NonNull Response<ApiService.SocialPostDetailResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        showError(response.code() == 404 ? "Bài viết không tồn tại." : "Không tải được bài viết.");
                        return;
                    }
                    bindPost(response.body());
                    loadComments();
                }

                @Override
                public void onFailure(@NonNull Call<ApiService.SocialPostDetailResponse> call, @NonNull Throwable t) {
                    showError("Không kết nối được backend: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> showError("Không lấy được phiên đăng nhập."));
    }

    private void loadComments() {
        apiService.getPostComments(postId, authHeader).enqueue(new Callback<List<ApiService.SocialCommentResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ApiService.SocialCommentResponse>> call,
                                   @NonNull Response<List<ApiService.SocialCommentResponse>> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showInlineMessage("Không tải được bình luận.");
                    return;
                }
                commentList.clear();
                for (ApiService.SocialCommentResponse item : response.body()) {
                    commentList.add(mapComment(item));
                }
                commentAdapter.notifyDataSetChanged();
                updateEmptyComments();
            }

            @Override
            public void onFailure(@NonNull Call<List<ApiService.SocialCommentResponse>> call, @NonNull Throwable t) {
                setLoading(false);
                showInlineMessage("Không kết nối được bình luận.");
            }
        });
    }

    private void bindPost(ApiService.SocialPostDetailResponse post) {
        postOwnerId = post.authorId == null ? "" : post.authorId;
        tvPostUsername.setText(nonEmpty(post.authorName, "Người dùng Cashify"));
        tvPostTime.setText(post.timestamp > 0 ? TimeFormatter.format(post.timestamp) : "");
        tvPostContent.setText(nonEmpty(post.content, ""));
        likeCount = Math.max(0, post.likeCount);
        commentCount = Math.max(0, post.commentCount);
        shareCount = Math.max(0, post.shareCount);
        isLiked = post.likedByMe;
        tvLikeCount.setText(String.valueOf(likeCount));
        tvCommentCount.setText(String.valueOf(commentCount));
        updateShareText();
        applyPostLikeState();

        if (post.authorAvatarUrl != null && !post.authorAvatarUrl.trim().isEmpty()) {
            ImageHelper.loadAvatar(post.authorAvatarUrl, imgPostAvatar);
        }
        if (post.imageUrl != null && !post.imageUrl.trim().isEmpty()) {
            imgPostImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.imageUrl).into(imgPostImage);
        } else {
            imgPostImage.setVisibility(View.GONE);
        }
        cardPostDetail.setVisibility(View.VISIBLE);
        tvPostDetailMessage.setVisibility(View.GONE);
    }

    private void togglePostLike() {
        if (authHeader == null) {
            return;
        }
        boolean targetLiked = !isLiked;
        layoutLikeButton.setEnabled(false);
        apiService.setPostLike(postId, authHeader, new ApiService.SocialLikeRequest(targetLiked))
                .enqueue(new Callback<ApiService.SocialReactionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.SocialReactionResponse> call,
                                           @NonNull Response<ApiService.SocialReactionResponse> response) {
                        layoutLikeButton.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PostDetailActivity.this, "Không cập nhật được lượt thích", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        isLiked = response.body().likedByMe;
                        likeCount = Math.max(0, response.body().likeCount);
                        tvLikeCount.setText(String.valueOf(likeCount));
                        applyPostLikeState();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.SocialReactionResponse> call, @NonNull Throwable t) {
                        layoutLikeButton.setEnabled(true);
                        Toast.makeText(PostDetailActivity.this, "Không kết nối được backend", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Hãy nhập bình luận trước nhé", Toast.LENGTH_SHORT).show();
            return;
        }
        imgSendComment.setEnabled(false);
        apiService.addPostComment(postId, authHeader, new ApiService.SocialCommentRequest(text))
                .enqueue(new Callback<ApiService.SocialCommentResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.SocialCommentResponse> call,
                                           @NonNull Response<ApiService.SocialCommentResponse> response) {
                        imgSendComment.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PostDetailActivity.this, "Không gửi được bình luận", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        commentList.add(mapComment(response.body()));
                        commentAdapter.notifyItemInserted(commentList.size() - 1);
                        commentCount++;
                        tvCommentCount.setText(String.valueOf(commentCount));
                        etCommentInput.setText("");
                        updateEmptyComments();
                        recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.SocialCommentResponse> call, @NonNull Throwable t) {
                        imgSendComment.setEnabled(true);
                        Toast.makeText(PostDetailActivity.this, "Không kết nối được backend", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sharePost() {
        copyPostLink();
        if (authHeader == null) {
            return;
        }
        apiService.sharePost(postId, authHeader).enqueue(new Callback<ApiService.SocialReactionResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.SocialReactionResponse> call,
                                   @NonNull Response<ApiService.SocialReactionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    shareCount = Math.max(0, response.body().shareCount);
                    updateShareText();
                }
            }

            @Override public void onFailure(@NonNull Call<ApiService.SocialReactionResponse> call, @NonNull Throwable t) {}
        });
    }

    private void copyPostLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cashify post", "cashify://posts/" + postId));
        }
        Toast.makeText(this, "Đã sao chép liên kết bài viết", Toast.LENGTH_SHORT).show();
    }

    private Comment mapComment(ApiService.SocialCommentResponse item) {
        return new Comment(
                item.id,
                item.authorId,
                item.authorAvatarUrl,
                nonEmpty(item.authorName, "Người dùng Cashify"),
                nonEmpty(item.content, ""),
                item.timestamp > 0 ? TimeFormatter.format(item.timestamp) : "",
                Math.max(0, item.likeCount)
        );
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

    private void showInlineMessage(String message) {
        tvPostDetailMessage.setText(message);
        tvPostDetailMessage.setVisibility(View.VISIBLE);
    }

    private void updateEmptyComments() {
        boolean empty = commentList.isEmpty();
        recyclerViewComments.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyComments.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
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
