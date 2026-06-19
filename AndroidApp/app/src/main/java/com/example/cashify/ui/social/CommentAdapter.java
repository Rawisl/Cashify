package com.example.cashify.ui.social;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Comment;
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
        private final ImageView imgCommentMenu;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgCommentAvatar);
            tvUsername = itemView.findViewById(R.id.tvCommentUsername);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            imgCommentMenu = itemView.findViewById(R.id.imgCommentMenu);
        }

        void bind(Comment comment, int position) {
            tvUsername.setText(comment.getUsername());
            tvTime.setText(comment.getTime());
            tvContent.setText(comment.getContent());

            ImageHelper.loadAvatar(comment.getAvatarUrl(), imgAvatar, comment.getUsername());

            imgCommentMenu.setOnClickListener(v -> showCommentBottomSheet(comment, position));
        }

        private void showCommentBottomSheet(Comment comment, int position) {
            BottomSheetDialog dialog = new BottomSheetDialog(itemView.getContext());
            View sheetView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.bottom_sheet_option, null);

            boolean isCommentOwner = currentUserId != null && currentUserId.equals(comment.getAuthorId());
            boolean isPostOwner = currentUserId != null && currentUserId.equals(postOwnerId);

            boolean canEditOrDelete = isCommentOwner || isAdmin;

            View btnEdit = sheetView.findViewById(R.id.btnEditComment);
            View btnDelete = sheetView.findViewById(R.id.btnDeleteComment);
            View btnHide = sheetView.findViewById(R.id.btnHideComment);

            if (canEditOrDelete) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                btnHide.setVisibility(View.GONE);
            } else if (isPostOwner) {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                btnHide.setVisibility(View.GONE);
            } else {
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