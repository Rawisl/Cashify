package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.utils.ImageHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private final String currentUserId;
    private String ownerId;
    private final OnMessageLongClickListener listener;

    public interface OnMessageLongClickListener {
        void onLongClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messageList, OnMessageLongClickListener listener) {
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage currentMessage = messageList.get(position);

        boolean isMine = currentMessage.getSenderId() != null && currentMessage.getSenderId().equals(currentUserId);
        boolean isOwner = ownerId != null && ownerId.equals(currentUserId);
        boolean canRecall = isMine || isOwner;

        // =========================================================================
        // MESSAGE GROUPING ALGORITHM (For Incoming Messages Only)
        // Groups consecutive messages from the same sender to optimize UI space
        // =========================================================================
        boolean isFirstInGroup = true;
        boolean isLastInGroup = true;

        if (position > 0) {
            ChatMessage prevMessage = messageList.get(position - 1);
            if (prevMessage.getSenderId() != null && prevMessage.getSenderId().equals(currentMessage.getSenderId())) {
                isFirstInGroup = false;
            }
        }

        if (position < messageList.size() - 1) {
            ChatMessage nextMessage = messageList.get(position + 1);
            if (nextMessage.getSenderId() != null && nextMessage.getSenderId().equals(currentMessage.getSenderId())) {
                isLastInGroup = false;
            }
        }

        // =========================================================================
        // UI BINDING
        // =========================================================================
        if (isMine) {
            // --- OUTGOING MESSAGES (MINE) ---
            holder.layoutLeft.setVisibility(View.GONE);
            holder.layoutRight.setVisibility(View.VISIBLE);

            if (currentMessage.isRecalled()) {
                // Handling Recalled State
                holder.tvTextRight.setText("You unsent a message");
                holder.tvTextRight.setVisibility(View.VISIBLE);

                String imageUrlRight = currentMessage.getImageUrl();
                if (imageUrlRight != null && !imageUrlRight.isEmpty()) {
                    holder.imgMessageRight.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrlRight)
                            .placeholder(R.drawable.ic_camera)
                            .into(holder.imgMessageRight);
                } else {
                    Glide.with(holder.itemView.getContext()).clear(holder.imgMessageRight);
                    holder.imgMessageRight.setVisibility(View.GONE);
                }

                holder.tvTextRight.setTypeface(null, Typeface.ITALIC);
                holder.tvTextRight.setAlpha(0.6f);
                holder.layoutRight.setOnLongClickListener(null); // Disable interactions for recalled messages

            } else {
                // Handling Normal State
                holder.tvTextRight.setText(currentMessage.getText());
                holder.tvTextRight.setVisibility(
                        (currentMessage.getText() == null || currentMessage.getText().isEmpty())
                                ? View.GONE : View.VISIBLE);

                String imageUrlRight = currentMessage.getImageUrl();
                if (imageUrlRight != null && !imageUrlRight.isEmpty()) {
                    holder.imgMessageRight.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrlRight)
                            .placeholder(R.drawable.ic_camera)
                            .into(holder.imgMessageRight);
                } else {
                    Glide.with(holder.itemView.getContext()).clear(holder.imgMessageRight);
                    holder.imgMessageRight.setVisibility(View.GONE);
                }

                holder.tvTextRight.setTypeface(null, Typeface.NORMAL);
                holder.tvTextRight.setAlpha(1.0f);

                // Enable Long Click for Context Menu
                holder.layoutRight.setOnLongClickListener(v -> {
                    if (listener != null) listener.onLongClick(currentMessage);
                    return true;
                });
            }
        } else {
            // --- INCOMING MESSAGES (THEIRS) ---
            holder.layoutRight.setVisibility(View.GONE);
            holder.layoutLeft.setVisibility(View.VISIBLE);

            if (currentMessage.isRecalled()) {
                // Handling Recalled State
                String senderName = currentMessage.getSenderName() != null ? currentMessage.getSenderName() : "Unknown";
                holder.tvTextLeft.setText(senderName + " unsent a message");
                holder.tvTextLeft.setVisibility(View.VISIBLE);

                String imageUrlLeft = currentMessage.getImageUrl();
                if (imageUrlLeft != null && !imageUrlLeft.isEmpty()) {
                    holder.imgMessageLeft.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrlLeft)
                            .placeholder(R.drawable.ic_camera)
                            .into(holder.imgMessageLeft);
                } else {
                    Glide.with(holder.itemView.getContext()).clear(holder.imgMessageLeft);
                    holder.imgMessageLeft.setVisibility(View.GONE);
                }

                holder.tvTextLeft.setTypeface(null, Typeface.ITALIC);
                holder.tvTextLeft.setAlpha(0.6f);
            } else {
                // Handling Normal State
                holder.tvTextLeft.setText(currentMessage.getText());
                holder.tvTextLeft.setVisibility(
                        (currentMessage.getText() == null || currentMessage.getText().isEmpty())
                                ? View.GONE : View.VISIBLE);

                String imageUrlLeft = currentMessage.getImageUrl();
                if (imageUrlLeft != null && !imageUrlLeft.isEmpty()) {
                    holder.imgMessageLeft.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrlLeft)
                            .placeholder(R.drawable.ic_camera)
                            .into(holder.imgMessageLeft);
                } else {
                    Glide.with(holder.itemView.getContext()).clear(holder.imgMessageLeft);
                    holder.imgMessageLeft.setVisibility(View.GONE);
                }

                holder.tvTextLeft.setTypeface(null, Typeface.NORMAL);
                holder.tvTextLeft.setAlpha(1.0f);
            }

            // Bind Long Click Event (with God Mode fallback for Admins/Owners)
            if (!currentMessage.isRecalled() && canRecall) {
                holder.layoutLeft.setOnLongClickListener(v -> {
                    if (listener != null) listener.onLongClick(currentMessage);
                    return true;
                });
            } else {
                holder.layoutLeft.setOnLongClickListener(null);
            }

            // Name & Avatar Visibility Logic (Based on Grouping)
            if (isFirstInGroup) {
                holder.tvNameLeft.setVisibility(View.VISIBLE);
                holder.tvNameLeft.setText(currentMessage.getSenderName() != null ? currentMessage.getSenderName() : "Unknown");
            } else {
                holder.tvNameLeft.setVisibility(View.GONE);
            }

            if (isLastInGroup) {
                holder.imgAvatarLeft.setVisibility(View.VISIBLE);
                ImageHelper.loadAvatar(currentMessage.getSenderAvatar(), holder.imgAvatarLeft, currentMessage.getSenderName());
            } else {
                holder.imgAvatarLeft.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMessages(List<ChatMessage> messages) {
        this.messageList = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutLeft, layoutRight;
        TextView tvTextLeft, tvNameLeft, tvTextRight;
        ImageView imgAvatarLeft, imgMessageLeft, imgMessageRight;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutLeft = itemView.findViewById(R.id.layoutMessageLeft);
            layoutRight = itemView.findViewById(R.id.layoutMessageRight);
            tvTextLeft = itemView.findViewById(R.id.tvTextLeft);
            tvNameLeft = itemView.findViewById(R.id.tvNameLeft);
            tvTextRight = itemView.findViewById(R.id.tvTextRight);
            imgAvatarLeft = itemView.findViewById(R.id.imgAvatarLeft);
            imgMessageLeft = itemView.findViewById(R.id.imgMessageLeft);
            imgMessageRight = itemView.findViewById(R.id.imgMessageRight);
        }
    }
}