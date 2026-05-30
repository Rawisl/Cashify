package com.example.cashify.ui.FriendsActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.DirectConversation;
import com.example.cashify.utils.ImageHelper;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    public interface OnConversationClickListener {
        void onConversationClick(DirectConversation conversation);
    }

    private List<DirectConversation> conversations;
    private final OnConversationClickListener listener;

    public ConversationAdapter(List<DirectConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DirectConversation conversation = conversations.get(position);
        holder.tvName.setText(conversation.getNameToShow());
        holder.tvEmail.setText(conversation.getFriendEmail() != null ? conversation.getFriendEmail() : "");
        holder.tvPreview.setText(conversation.getLatestMessageText() != null ? conversation.getLatestMessageText() : "");
        holder.tvTimestamp.setText(conversation.getLatestMessageTimestamp() > 0
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(conversation.getLatestMessageTimestamp()))
                : "");
        holder.tvUnread.setVisibility(conversation.getUnreadCount() > 0 ? View.VISIBLE : View.GONE);
        holder.tvUnread.setText(String.valueOf(conversation.getUnreadCount()));

        ImageHelper.loadAvatar(conversation.getFriendAvatarUrl(), holder.imgAvatar, conversation.getNameToShow());
        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    public void updateList(List<DirectConversation> newList) {
        conversations = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvEmail, tvPreview, tvTimestamp, tvUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvConversationName);
            tvEmail = itemView.findViewById(R.id.tvConversationEmail);
            tvPreview = itemView.findViewById(R.id.tvLatestMessage);
            tvTimestamp = itemView.findViewById(R.id.tvLatestTimestamp);
            tvUnread = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
