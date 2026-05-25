package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.HeartAnimation;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;
    private final OnCommentActionListener listener;
    private final String currentUserId;
    private final String postOwnerId;

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

    class CommentViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgAvatar;
        private TextView tvUsername, tvTime, tvContent;
        private CardView cardComment;
        private LinearLayout layoutCommentLike;
        private ImageView imgCommentLike;
        private TextView tvCommentLikeCount;
        private ImageView imgCommentMenu;

        private boolean isLiked = false;
        private int likeCount = 0;

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
            imgCommentMenu     = itemView.findViewById(R.id.imgCommentMenu);
        }

        void bind(Comment comment, int position) {
            tvUsername.setText(comment.getUsername());
            tvTime.setText(comment.getTime());
            tvContent.setText(comment.getContent());

            isLiked = false;
            likeCount = Math.max(0, comment.getLikeCount());
            imgCommentLike.clearColorFilter();
            tvCommentLikeCount.setText(String.valueOf(likeCount));

            Glide.with(itemView.getContext())
                    .load(comment.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_user)
                    .error(R.drawable.ic_default_user)
                    .into(imgAvatar);

            layoutCommentLike.setOnClickListener(v -> {
                isLiked = HeartAnimation.toggleLike(
                        itemView.getContext(),
                        imgCommentLike,
                        tvCommentLikeCount,
                        isLiked,
                        likeCount,
                        R.color.status_red
                );
                if (isLiked) {
                    likeCount++;
                } else {
                    likeCount--;
                }
            });

            imgCommentMenu.setOnClickListener(v ->
                    showCommentBottomSheet(comment, position)
            );
        }

        private void showCommentBottomSheet(Comment comment, int position) {
            BottomSheetDialog dialog = new BottomSheetDialog(itemView.getContext());
            View sheetView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.bottom_sheet_option, null);

            boolean isCommentOwner = currentUserId != null && currentUserId.equals(comment.getAuthorId());
            boolean isPostOwner = currentUserId != null && currentUserId.equals(postOwnerId);
            boolean isCommentOwner = comment.getUsername().equals("You");
            boolean isPostOwner    = currentUserId.equals(postOwnerId);

            View btnEdit   = sheetView.findViewById(R.id.btnEditComment);
            View btnDelete = sheetView.findViewById(R.id.btnDeleteComment);
            View btnHide   = sheetView.findViewById(R.id.btnHideComment);

            if (isCommentOwner) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
            } else if (isPostOwner) {
                btnHide.setVisibility(View.VISIBLE);
            } else {
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

            sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v ->
                    dialog.dismiss()
            );

            dialog.setContentView(sheetView);
            dialog.show();
        }
    }
}
