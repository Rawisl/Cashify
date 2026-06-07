package com.example.cashify.ui.FriendsActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.common.AvatarImageView;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    public interface ActionListener {
        void onAddFriend(User user);
        void onCancelRequest(User user);
        void onAccept(User user);
        void onDecline(User user);
        void onUnfriend(User user);
        void onMessage(User user);
    }

    private List<User> users;
    private final ActionListener listener;

    public FriendAdapter(List<User> users, ActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvFriendName.setText(user.getNameToShow());
        ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar, user.getNameToShow());
        hideAllButtons(holder);

        switch (user.getFriendStatus()) {
            case 1:
                holder.btnMessage.setVisibility(View.VISIBLE);
                holder.btnUnfriend.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Friends");
                holder.btnMessage.setOnClickListener(v -> listener.onMessage(user));
                holder.btnUnfriend.setOnClickListener(v -> listener.onUnfriend(user));
                break;
            case 2:
                holder.tvSentRequest.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Waiting for response");
                holder.tvSentRequest.setOnClickListener(v -> listener.onCancelRequest(user));
                break;
            case 3:
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnDecline.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Friend request sent");
                holder.btnAccept.setOnClickListener(v -> listener.onAccept(user));
                holder.btnDecline.setOnClickListener(v -> listener.onDecline(user));
                break;
            default:
                holder.btnAddFriend.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(user.getEmail() != null ? user.getEmail() : "");
                holder.btnAddFriend.setOnClickListener(v -> listener.onAddFriend(user));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public void updateList(List<User> newList) {
        users = newList;
        notifyDataSetChanged();
    }

    private void hideAllButtons(ViewHolder holder) {
        holder.btnAddFriend.setVisibility(View.GONE);
        holder.btnMessage.setVisibility(View.GONE);
        holder.btnUnfriend.setVisibility(View.GONE);
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnDecline.setVisibility(View.GONE);
        holder.tvAlreadyFriend.setVisibility(View.GONE);
        holder.tvSentRequest.setVisibility(View.GONE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName;
        TextView tvStatus;
        TextView tvAlreadyFriend;
        TextView tvSentRequest;
        AvatarImageView imgAvatar;
        MaterialButton btnMessage;
        MaterialButton btnUnfriend;
        MaterialButton btnAddFriend;
        MaterialButton btnAccept;
        MaterialButton btnDecline;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            btnMessage = itemView.findViewById(R.id.btnMessage);
            btnUnfriend = itemView.findViewById(R.id.btnUnfriend);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            tvAlreadyFriend = itemView.findViewById(R.id.tvAlreadyFriend);
            tvSentRequest = itemView.findViewById(R.id.tvSentRequest);
        }
    }
}
