package com.example.cashify.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.HeartAnimation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private ImageView imgLikeHeart;
    private TextView tvLikeCount;
    private boolean isLiked = false;
    private int likeCount = 42;

    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private EditText etCommentInput;
    private ImageView imgSendComment;
    private ImageView imgPostMenu;

    private String currentUserId = "user123";
    private String postOwnerId   = "user123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        initViews();
        setupToolbar();
        setupLikeButton();
        setupComments();
        setupCommentInput();
        setupPostMenu();
    }

    private void initViews() {
        imgLikeHeart         = findViewById(R.id.imgLikeHeart);
        tvLikeCount          = findViewById(R.id.tvLikeCount);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        etCommentInput       = findViewById(R.id.etCommentInput);
        imgSendComment       = findViewById(R.id.imgSendComment);
        imgPostMenu          = findViewById(R.id.imgPostMenu);
        setupMockPostData();
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

    // ─── Like button ────────────────────────────────────────────────────────

    private void setupLikeButton() {
        imgLikeHeart.setOnClickListener(v -> togglePostLike());
        findViewById(R.id.layoutLikeButton).setOnClickListener(v -> togglePostLike());
    }

    private void togglePostLike() {
        isLiked = HeartAnimation.toggleLike(
                this, imgLikeHeart, tvLikeCount,
                isLiked, likeCount, R.color.status_red
        );
        if (isLiked) likeCount++; else likeCount--;
    }

    // ─── Mock data ──────────────────────────────────────────────────────────

    private void setupMockPostData() {
        ((TextView) findViewById(R.id.tvPostUsername)).setText("John Doe");
        ((TextView) findViewById(R.id.tvPostTime)).setText("2 hours ago");
        ((TextView) findViewById(R.id.tvPostContent)).setText(
                "This is a sample post about managing finances. " +
                        "It's important to track your spending and save for the future!");
        tvLikeCount.setText(String.valueOf(likeCount));
        ((TextView) findViewById(R.id.tvCommentCount)).setText("8");
    }

    // ─── Comments ───────────────────────────────────────────────────────────

    private void setupComments() {
        commentList = createMockComments();

        commentAdapter = new CommentAdapter(
                commentList,
                new CommentAdapter.OnCommentActionListener() {
                    @Override
                    public void onEditComment(int position) {
                        Toast.makeText(PostDetailActivity.this,
                                "Edit: " + position, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDeleteComment(int position) {
                        commentList.remove(position);
                        commentAdapter.notifyItemRemoved(position);
                    }

                    @Override
                    public void onHideComment(int position) {
                        commentList.remove(position);
                        commentAdapter.notifyItemRemoved(position);
                    }
                },
                currentUserId,
                postOwnerId
        );

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    // ─── Comment input ──────────────────────────────────────────────────────

    private void setupCommentInput() {
        imgSendComment.setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                return;
            }
            Comment newComment = new Comment(
                    "https://i.pravatar.cc/150?img=5", "You", text, "Just now"
            );
            commentList.add(newComment);
            commentAdapter.notifyItemInserted(commentList.size() - 1);
            etCommentInput.setText("");
            recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
        });
    }

    // ─── Post menu → BottomSheet ─────────────────────────────────────────────

    private void setupPostMenu() {
        imgPostMenu.setOnClickListener(v -> showPostBottomSheet());
    }

    private void showPostBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_option, null);

        // Edit không dùng cho post
        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        boolean isOwner = currentUserId.equals(postOwnerId);

        if (isOwner) {
            // Chủ bài: chỉ Delete
            sheetView.findViewById(R.id.btnDeleteComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            sheetView.findViewById(R.id.btnDeleteComment).setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });
        } else {
            // Người khác: Hide + Report
            sheetView.findViewById(R.id.btnDeleteComment).setVisibility(View.GONE);
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

    // ─── Mock comments ──────────────────────────────────────────────────────

    private List<Comment> createMockComments() {
        List<Comment> list = new ArrayList<>();
        list.add(new Comment("https://i.pravatar.cc/150?img=1", "John Doe",    "This is a great post!",    "2 hours ago"));
        list.add(new Comment("https://i.pravatar.cc/150?img=2", "Jane Smith",  "I totally agree!",         "3 hours ago"));
        list.add(new Comment("https://i.pravatar.cc/150?img=3", "Mike Johnson","Interesting perspective.", "5 hours ago"));
        list.add(new Comment("https://i.pravatar.cc/150?img=4", "Sarah W.",    "Really helpful.",          "1 day ago"));
        return list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}