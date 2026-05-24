package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.HeartAnimation;

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

        private final ImageView imgAvatar;
        private final TextView tvUsername;
        private final TextView tvTime;
        private final TextView tvContent;
        private final CardView cardComment;
        private final LinearLayout layoutCommentLike;
        private final ImageView imgCommentLike;
        private final TextView tvCommentLikeCount;

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

            cardComment.setOnLongClickListener(v -> {
                showCommentOptions(v, comment, position);
                return true;
            });
        }

        private void showCommentOptions(View anchor, Comment comment, int position) {
            PopupMenu popup = new PopupMenu(itemView.getContext(), anchor);

            boolean isCommentOwner = currentUserId != null && currentUserId.equals(comment.getAuthorId());
            boolean isPostOwner = currentUserId != null && currentUserId.equals(postOwnerId);

            if (isCommentOwner) {
                popup.getMenuInflater().inflate(R.menu.menu_comment, popup.getMenu());
            } else if (isPostOwner) {
                popup.getMenuInflater().inflate(R.menu.menu_comment_post_owner, popup.getMenu());
            } else {
                popup.getMenuInflater().inflate(R.menu.menu_comment_other, popup.getMenu());
            }

            popup.setOnMenuItemClickListener(item -> {
                if (listener == null) return false;
                int id = item.getItemId();
                if (id == R.id.action_edit_comment) {
                    listener.onEditComment(position);
                    return true;
                } else if (id == R.id.action_delete_comment) {
                    listener.onDeleteComment(position);
                    return true;
                } else if (id == R.id.action_hide_comment) {
                    listener.onHideComment(position);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
