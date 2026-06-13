package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;

import java.util.ArrayList;
import java.util.List;

public class ProfileAchievementAdapter extends RecyclerView.Adapter<ProfileAchievementAdapter.ViewHolder> {

    // Danh sách chứa các cúp
    private final List<BadgeMeta> badges = new ArrayList<>();

    // CLASS DATA MODEL
    public static class BadgeMeta {
        public String title;
        public String icon;
        public String bgColor;

        public BadgeMeta(String title, String icon, String bgColor) {
            this.title = title;
            this.icon = icon;
            this.bgColor = bgColor;
        }
    }

    // Hàm nhận dữ liệu từ Fragment truyền sang
    public void submitList(List<BadgeMeta> newBadges) {
        badges.clear();
        if (newBadges != null) {
            badges.addAll(newBadges);
        }
        notifyDataSetChanged(); // Load lại giao diện
    }

    @NonNull
    @Override
    public ProfileAchievementAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_achievement_badge, parent, false);
        return new ProfileAchievementAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileAchievementAdapter.ViewHolder holder, int position) {
        BadgeMeta badge = badges.get(position);

        // Đắp Emoji và Tiêu đề vào UI
        holder.tvBadgeIcon.setText(badge.icon);
        holder.tvBadgeTitle.setText(badge.title);

        // (Optional) Nếu bro muốn đổi màu nền theo bgColor thì chọc thêm vào layoutBadgeBg ở đây
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    // Ánh xạ các view từ file XML
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadgeIcon;
        TextView tvBadgeTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeIcon = itemView.findViewById(R.id.tvBadgeIcon);
            tvBadgeTitle = itemView.findViewById(R.id.tvBadgeTitle);
        }
    }
}