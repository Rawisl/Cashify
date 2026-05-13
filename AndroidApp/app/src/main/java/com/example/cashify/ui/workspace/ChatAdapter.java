package com.example.cashify.ui.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.utils.ImageHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private final String currentUserId;
    private String ownerId;

    private final OnMessageLongClickListener listener;

    public interface OnMessageLongClickListener {
        void onLongClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messageList, OnMessageLongClickListener listener) {
        this.messageList = messageList;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    // ĐÃ FIX: Thêm body cho hàm update OwnerId
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

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage currentMessage = messageList.get(position);
        boolean isMine = currentMessage.getSenderId() != null && currentMessage.getSenderId().equals(currentUserId);
        boolean isOwner = (ownerId != null && ownerId.equals(currentUserId));
        boolean canRecall = isMine || isOwner;

        // ==========================================================
        // THUẬT TOÁN GỘP TIN NHẮN (CHỈ DÀNH CHO NGƯỜI KHÁC)
        // ==========================================================
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

        // ==========================================================
        // HIỂN THỊ GIAO DIỆN
        // ==========================================================
        if (isMine) {
            // TIN NHẮN CỦA MÌNH
            holder.layoutLeft.setVisibility(View.GONE);
            holder.layoutRight.setVisibility(View.VISIBLE);

            // ĐÃ FIX: Gộp logic giao diện và sự kiện vào chung 1 chỗ
            if (currentMessage.isRecalled()) {
                holder.tvTextRight.setText("You unsent a message");
                holder.tvTextRight.setTypeface(null, android.graphics.Typeface.ITALIC);
                holder.tvTextRight.setAlpha(0.6f);
                holder.layoutRight.setOnLongClickListener(null); // Bị thu hồi rồi thì khóa mõm
            } else {
                holder.tvTextRight.setText(currentMessage.getText());
                holder.tvTextRight.setTypeface(null, android.graphics.Typeface.NORMAL);
                holder.tvTextRight.setAlpha(1.0f);

                // Mở khóa Long Click
                holder.layoutRight.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onLongClick(currentMessage);
                    }
                    return true;
                });
            }
        } else {
            // TIN NHẮN CỦA NGƯỜI KHÁC
            holder.layoutRight.setVisibility(View.GONE);
            holder.layoutLeft.setVisibility(View.VISIBLE);

            if (currentMessage.isRecalled()) {
                String senderName = currentMessage.getSenderName() != null ? currentMessage.getSenderName() : "Unknown";
                holder.tvTextLeft.setText(senderName + " unsent a message");
                holder.tvTextLeft.setTypeface(null, android.graphics.Typeface.ITALIC);
                holder.tvTextLeft.setAlpha(0.6f);
            } else {
                holder.tvTextLeft.setText(currentMessage.getText());
                holder.tvTextLeft.setTypeface(null, android.graphics.Typeface.NORMAL);
                holder.tvTextLeft.setAlpha(1.0f);
            }

            // ĐÃ FIX GOD MODE: Xử lý Long Click gộp chung luôn
            if (!currentMessage.isRecalled() && canRecall) {
                holder.layoutLeft.setOnLongClickListener(v -> {
                    if (listener != null) listener.onLongClick(currentMessage);
                    return true;
                });
            } else {
                holder.layoutLeft.setOnLongClickListener(null);
            }

            // TÊN & AVATAR
            if (isFirstInGroup) {
                holder.tvNameLeft.setVisibility(View.VISIBLE);
                holder.tvNameLeft.setText(currentMessage.getSenderName() != null ? currentMessage.getSenderName() : "Unknown");
            } else {
                holder.tvNameLeft.setVisibility(View.GONE);
            }

            if (isLastInGroup) {
                holder.imgAvatarLeft.setVisibility(View.VISIBLE);
                ImageHelper.loadAvatar(currentMessage.getSenderAvatar(), holder.imgAvatarLeft);
            } else {
                holder.imgAvatarLeft.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutLeft, layoutRight;
        TextView tvTextLeft, tvNameLeft, tvTextRight;
        CircleImageView imgAvatarLeft;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutLeft = itemView.findViewById(R.id.layoutMessageLeft);
            layoutRight = itemView.findViewById(R.id.layoutMessageRight);
            tvTextLeft = itemView.findViewById(R.id.tvTextLeft);
            tvNameLeft = itemView.findViewById(R.id.tvNameLeft);
            tvTextRight = itemView.findViewById(R.id.tvTextRight);
            imgAvatarLeft = itemView.findViewById(R.id.imgAvatarLeft);
        }
    }
}