package com.example.cashify.ui.FriendsActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
// Chú ý: Đổi import User này cho đúng đường dẫn model của ghệ nhé (vd: com.example.cashify.data.model.User)
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<User> users;

    public FriendAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tham chiếu đúng file item_friend.xml của ghệ
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        // Map dữ liệu từ User vào UI
        // Nhớ check lại hàm getDisplayName() xem file User.java của ghệ khai báo là gì nhé
        holder.tvFriendName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
        holder.tvStatus.setText(user.getEmail());

        // Load Avatar xịn xò qua cái ImageHelper anh em mình làm hôm bữa
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_user);
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Khai báo các view trong item_friend.xml
        TextView tvFriendName;
        TextView tvStatus;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ id (Nhớ đổi lại ID cho đúng với file XML của ghệ)
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}