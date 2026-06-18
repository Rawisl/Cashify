package com.example.cashify.ui.social;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.HeartAnimation;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;
    private final OnCommentActionListener listener;
    private final String currentUserId;
    private String postOwnerId;
    private boolean isAdmin = false;

    public interface OnCommentActionListener {
        void onEditComment(int position);
        void onDeleteComment(int position);
        void onHideComment(int position);
    }

    public CommentAdapter(List<Comment> commentList, OnCommentActionListener listener,
                          String currentUserId, String postOwnerId) {
        this.commentList = commentList;
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.postOwnerId = postOwnerId;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
        notifyDataSetChanged();
    }

    public void updatePostOwnerId(String postOwnerId) {
        this.postOwnerId = postOwnerId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(commentList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    class CommentViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgAvatar;
        private final TextView tvUsername, tvTime, tvContent;
        private final CardView cardComment;
        private final LinearLayout layoutCommentLike;
        private final ImageView imgCommentLike;
        private final TextView tvCommentLikeCount;
        private final ImageView imgCommentMenu;

        private boolean isLiked = false;
        private int likeCount = 0;
        private final int activeLikeColor;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgCommentAvatar);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            cardComment = itemView.findViewById(R.id.cardComment);
            layoutCommentLike = itemView.findViewById(R.id.layoutCommentLike);
            imgCommentLike = itemView.findViewById(R.id.imgCommentLike);
            tvCommentLikeCount = itemView.findViewById(R.id.tvCommentLikeCount);
            imgCommentMenu = itemView.findViewById(R.id.imgCommentMenu);

            // Pre-fetch color to avoid repeated lookups during scroll
            activeLikeColor = ContextCompat.getColor(itemView.getContext(), R.color.status_red);
        }

        @SuppressLint("SetTextI18n")
        void bind(Comment comment, int position) {
            tvUsername.setText(comment.getUsername());
            tvTime.setText(comment.getTime());
            tvContent.setText(comment.getContent());

            // Reset UI states for recycled views
            isLiked = false;
            likeCount = Math.max(0, comment.getLikeCount());
            imgCommentLike.clearColorFilter();
            tvCommentLikeCount.setText(likeCount > 0 ? String.valueOf(likeCount) : "");

            // Load Avatar
            ImageHelper.loadAvatar(comment.getAvatarUrl(), imgAvatar, comment.getUsername());

            // Handle Local Like Action (No API call required per current logic)
            layoutCommentLike.setOnClickListener(v -> {
                isLiked = !isLiked;
                if (isLiked) {
                    likeCount++;
                    imgCommentLike.setColorFilter(activeLikeColor);
                    HeartAnimation.playRubberBand(imgCommentLike); // Trigger animation
                } else {
                    likeCount = Math.max(0, likeCount - 1);
                    imgCommentLike.clearColorFilter();
                }
                tvCommentLikeCount.setText(likeCount > 0 ? String.valueOf(likeCount) : "");
            });

            imgCommentMenu.setOnClickListener(v -> showCommentBottomSheet(comment, position));
        }

        private void showCommentBottomSheet(Comment comment, int position) {
            BottomSheetDialog dialog = new BottomSheetDialog(itemView.getContext());
            View sheetView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.bottom_sheet_option, null);

            boolean isCommentOwner = currentUserId != null && currentUserId.equals(comment.getAuthorId());
            boolean isPostOwner = currentUserId != null && currentUserId.equals(postOwnerId);

            // Permissions: Admins and the original author have full rights.
            boolean canEditOrDelete = isCommentOwner || isAdmin;

            View btnEdit = sheetView.findViewById(R.id.btnEditComment);
            View btnDelete = sheetView.findViewById(R.id.btnDeleteComment);
            View btnHide = sheetView.findViewById(R.id.btnHideComment);

            if (canEditOrDelete) {
                // Full rights
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                btnHide.setVisibility(View.GONE);
            } else if (isPostOwner) {
                // Post owner can moderate (delete) comments on their post, but cannot edit them
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                btnHide.setVisibility(View.GONE);
            } else {
                // Third-party viewer can only hide the comment locally
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                btnHide.setVisibility(View.VISIBLE);
            }

            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onEditComment(position);
            });

            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onDeleteComment(position);
            });

            btnHide.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onHideComment(position);
            });

            sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());

            dialog.setContentView(sheetView);
            dialog.show();
        }
    }
}