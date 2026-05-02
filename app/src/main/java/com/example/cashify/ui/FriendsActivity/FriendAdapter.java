package com.example.cashify.ui.FriendsActivity;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cashify.R;
import com.example.cashify.databinding.ItemFriendBinding;
import com.example.cashify.database.User;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<User> users;

    public FriendAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        // Map dữ liệu từ User vào UI
        holder.binding.tvFriendName.setText(user.name != null ? user.name : "Unknown");
        holder.binding.tvStatus.setText(user.email); // Tạm lấy email làm status

        // Ảnh đại diện mẫu
        holder.binding.imgAvatar.setImageResource(android.R.drawable.ic_menu_report_image);
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemFriendBinding binding;
        public ViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}