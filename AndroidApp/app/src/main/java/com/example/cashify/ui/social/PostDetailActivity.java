package com.example.cashify.ui.social;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.cashify.R;
import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.ui.feed.CommunityFeedAdapter;
import com.example.cashify.ui.feed.FeedItem;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.TimeFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "postId";
    private boolean isAdmin = false;
    private TextView tvPostDetailMessage;
    private ProgressBar progressPostDetail;
    private RecyclerView recyclerViewMain;
    private EditText etCommentInput;
    private ImageView imgSendComment;
    private ImageView imgCurrentUserAvatar;

    private CommunityFeedAdapter postAdapter;
    private CommentAdapter commentAdapter;
    private final List<Comment> commentList = new ArrayList<>();

    private PostDetailViewModel viewModel;
    private String postId;
    private String authHeader;
    private String currentUserId = "";
    private String postOwnerId = "";
    private FeedItem currentFeedItem = null;

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
        setupCombinedRecyclerView();

        if (!currentUserId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            isAdmin = "ADMIN".equals(doc.getString("role"));
                            if (commentAdapter != null) commentAdapter.setAdmin(isAdmin);

                            String myAvatar = doc.getString("avatarUrl");
                            ImageHelper.loadAvatar(myAvatar, imgCurrentUserAvatar, doc.getString("displayName"));
                        }
                    });
        }

        imgSendComment.setOnClickListener(v -> sendComment());

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                authHeader = "Bearer " + result.getToken();
                observeViewModel();
                viewModel.loadPost(postId, authHeader);
                viewModel.loadComments(postId, authHeader);
            }).addOnFailureListener(e -> showError("Failed to get login session."));
        } else {
            showError("Please log in to view posts.");
        }
    }

    private void initViews() {
        tvPostDetailMessage = findViewById(R.id.tvPostDetailMessage);
        progressPostDetail = findViewById(R.id.progressPostDetail);
        recyclerViewMain = findViewById(R.id.recyclerViewMainHợpNhất);
        etCommentInput = findViewById(R.id.etCommentInput);
        imgSendComment = findViewById(R.id.imgSendComment);
        imgCurrentUserAvatar = findViewById(R.id.imgCurrentUserAvatar);
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

    private void setupCombinedRecyclerView() {
        postAdapter = new CommunityFeedAdapter(item -> showPostBottomSheet());

        postAdapter.setOnLikeClickListener((pId, isLiked, callback) ->
                viewModel.toggleLike(pId, authHeader, isLiked)
        );

        commentAdapter = new CommentAdapter(commentList, new CommentAdapter.OnCommentActionListener() {
            @Override public void onEditComment(int pos) { showEditCommentDialog(commentList.get(pos)); }
            @Override public void onDeleteComment(int pos) { viewModel.deleteComment(postId, commentList.get(pos).getId(), authHeader); }
            @Override public void onHideComment(int pos) { commentList.remove(pos); commentAdapter.notifyItemRemoved(pos); }
        }, currentUserId, postOwnerId);

        ConcatAdapter concatAdapter = new ConcatAdapter(postAdapter, commentAdapter);

        recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMain.setAdapter(concatAdapter);

        // Chống nháy màn hình khi update số lượng comment
        if (recyclerViewMain.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerViewMain.getItemAnimator()).setSupportsChangeAnimations(false);
        }
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::setLoading);
        viewModel.getErrorMessage().observe(this, this::showError);

        viewModel.getPostDetail().observe(this, post -> {
            if (post != null) {
                postOwnerId = post.authorId == null ? "" : post.authorId;
                commentAdapter.updatePostOwnerId(postOwnerId);

                if (post.milestoneData != null && !post.milestoneData.trim().isEmpty()) {
                    FeedItem.MilestonePost milestone = new FeedItem.MilestonePost();
                    milestone.setId(postId);
                    milestone.setUserId(post.authorId);
                    milestone.userName = post.authorName;
                    milestone.avatarUrl = post.authorAvatarUrl;
                    milestone.time = post.timestamp > 0 ? TimeFormatter.format(post.timestamp) : "";
                    milestone.setLikeCount(post.likeCount);
                    milestone.setCommentCount(post.commentCount);
                    milestone.milestoneJson = post.milestoneData;

                    try {
                        org.json.JSONObject json = new org.json.JSONObject(post.milestoneData);
                        milestone.iconText = json.optString("iconText", "🏆");
                        milestone.title = json.optString("title", "");
                        milestone.month = json.optString("month", "");
                        milestone.amount = json.optString("amount", "");
                        milestone.progress = json.optInt("progress", 0);
                        milestone.description = nonEmpty(post.content, "");
                    } catch (Exception ignored) {}

                    currentFeedItem = milestone;
                } else {
                    FeedItem.NormalPost normal = new FeedItem.NormalPost();
                    normal.setId(postId);
                    normal.setUserId(post.authorId);
                    normal.userName = post.authorName;
                    normal.avatarUrl = post.authorAvatarUrl;
                    normal.time = post.timestamp > 0 ? TimeFormatter.format(post.timestamp) : "";

                    normal.title = post.title != null ? post.title : "";
                    normal.description = nonEmpty(post.content, "");

                    normal.setLikeCount(post.likeCount);
                    normal.setCommentCount(post.commentCount);
                    normal.hasImage = post.imageUrl != null && !post.imageUrl.trim().isEmpty();
                    normal.imageUrl = post.imageUrl;

                    currentFeedItem = normal;
                }

                if (post.likedByMe) postAdapter.addLikedId(postId);
                else postAdapter.clearLikedIds();

                postAdapter.submitList(Collections.singletonList(currentFeedItem));

                recyclerViewMain.setVisibility(View.VISIBLE);
                tvPostDetailMessage.setVisibility(View.GONE);
            }
        });

        viewModel.getIsActionSuccess().observe(this, success -> { if (success) finish(); });

        viewModel.getComments().observe(this, list -> {
            if (list == null) return;
            commentList.clear();
            for (Object obj : list) {
                if (!(obj instanceof java.util.Map)) continue;
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                commentList.add(new Comment(
                        mapStr(map, "commentId"), mapStr(map, "userId"), mapStr(map, "authorAvatarUrl"),
                        mapStr(map, "authorName").isEmpty() ? "Cashify User" : mapStr(map, "authorName"),
                        mapStr(map, "content"),
                        mapNum(map, "timestamp") > 0 ? TimeFormatter.format(mapNum(map, "timestamp")) : "Just now", 0
                ));
            }
            commentAdapter.notifyDataSetChanged();
        });
    }

    private void sendComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty() || authHeader == null) return;

        etCommentInput.setText("");

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etCommentInput.getWindowToken(), 0);

        if (currentFeedItem != null) {
            currentFeedItem.setCommentCount(currentFeedItem.getCommentCount() + 1);
            postAdapter.notifyItemChanged(0);
        }

        viewModel.addComment(postId, authHeader, text);
    }

    private void showPostBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_option, null);
        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        boolean isOwner = !currentUserId.isEmpty() && currentUserId.equals(postOwnerId);
        if (isOwner || isAdmin) {
            View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
            if (btnEditPost != null) {
                btnEditPost.setVisibility(View.VISIBLE);
                btnEditPost.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(PostDetailActivity.this, MainActivity.class);
                    intent.putExtra("ACTION_EDIT_POST", true);
                    intent.putExtra("edit_post_id", postId);
                    if (currentFeedItem instanceof FeedItem.NormalPost) {
                        intent.putExtra("edit_post_content", ((FeedItem.NormalPost) currentFeedItem).description); // FIX: text -> description
                    } else if (currentFeedItem instanceof FeedItem.MilestonePost) {
                        intent.putExtra("edit_post_content", ((FeedItem.MilestonePost) currentFeedItem).description);
                        intent.putExtra("edit_milestone_data", ((FeedItem.MilestonePost) currentFeedItem).milestoneJson);
                    }
                    startActivity(intent);
                });
            }
            sheetView.findViewById(R.id.btnDeleteComment).setOnClickListener(v -> { dialog.dismiss(); viewModel.deletePost(postId, authHeader); });
        } else {
            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> { dialog.dismiss(); finish(); });
        }
        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showEditCommentDialog(Comment comment) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_comment, null);
        EditText etEditComment = view.findViewById(R.id.etEditComment);
        etEditComment.setText(comment.getContent());
        etEditComment.setSelection(etEditComment.getText().length());

        view.findViewById(R.id.btnSaveComment).setOnClickListener(v -> {
            String txt = etEditComment.getText().toString().trim();
            if (!txt.isEmpty() && !txt.equals(comment.getContent())) {
                viewModel.editComment(postId, comment.getId(), txt, authHeader);
            }
            dialog.dismiss();
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void setLoading(boolean loading) {
        progressPostDetail.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerViewMain.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String msg) {
        progressPostDetail.setVisibility(View.GONE);
        recyclerViewMain.setVisibility(View.GONE);
        tvPostDetailMessage.setText(msg);
        tvPostDetailMessage.setVisibility(View.VISIBLE);
    }

    private String nonEmpty(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    private String mapStr(java.util.Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof String ? (String) v : ""; }
    private long mapNum(java.util.Map<String, Object> map, String key) { Object v = map.get(key); return v instanceof Number ? ((Number) v).longValue() : 0L; }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}