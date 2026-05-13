package com.example.cashify.ui.FriendsActivity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private static final String TAG = "CASHIFY";

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
        Log.e(TAG, "Bind: " + user.getNameToShow() + " | status=" + user.getFriendStatus());

        // Tên
        holder.tvFriendName.setText(user.getNameToShow());

        // Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_user);
        }

        // Ẩn hết trước
        hideAllButtons(holder);

        switch (user.getFriendStatus()) {
            case 1: // ĐÃ LÀ BẠN
                holder.tvAlreadyFriend.setVisibility(View.VISIBLE);
                holder.btnMessage.setVisibility(View.VISIBLE);
                holder.btnUnfriend.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Bạn bè");

                holder.btnMessage.setOnClickListener(v -> listener.onMessage(user));
                holder.btnUnfriend.setOnClickListener(v -> listener.onUnfriend(user));
                break;

            case 2: // MÌNH ĐÃ GỬI LỜI MỜI
                holder.tvSentRequest.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Đã gửi lời mời...");

                // Bấm vào "Sent" để huỷ
                holder.tvSentRequest.setOnClickListener(v -> listener.onCancelRequest(user));
                break;

            case 3: // HỌ GỬI LỜI MỜI CHO MÌNH
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnDecline.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Đã gửi lời mời kết bạn");

                holder.btnAccept.setOnClickListener(v -> listener.onAccept(user));
                holder.btnDecline.setOnClickListener(v -> listener.onDecline(user));
                break;

            default: // NGƯỜI LẠ
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
        this.users = newList;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName, tvStatus, tvAlreadyFriend, tvSentRequest;
        ImageView imgAvatar;
        MaterialButton btnMessage, btnUnfriend, btnAddFriend, btnAccept, btnDecline;

        public ViewHolder(@NonNull View itemView) {
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