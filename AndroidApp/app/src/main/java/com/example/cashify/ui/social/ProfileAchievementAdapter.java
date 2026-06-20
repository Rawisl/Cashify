package com.example.cashify.ui.social;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;

import java.util.ArrayList;
import java.util.List;

public class ProfileAchievementAdapter extends RecyclerView.Adapter<ProfileAchievementAdapter.ViewHolder> {

    // Internal data set
    private List<BadgeMeta> badges = new ArrayList<>();

    // =========================================================================
    // DATA MODEL
    // =========================================================================
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

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<BadgeMeta> newBadges) {
        this.badges = newBadges != null ? newBadges : new ArrayList<>();
        notifyDataSetChanged();
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

        // Bind Emoji and Title to the UI
        holder.tvBadgeIcon.setText(badge.icon);
        holder.tvBadgeTitle.setText(badge.title);

        // =========================================================================
        // DYNAMIC BACKGROUND COLOR APPLICATION
        // Uses TintList to preserve the circular shape of the underlying drawable
        // =========================================================================
        if (badge.bgColor != null && !badge.bgColor.isEmpty()) {
            try {
                int color = Color.parseColor(badge.bgColor);
                holder.layoutBadgeBg.setBackgroundTintList(ColorStateList.valueOf(color));
            } catch (IllegalArgumentException e) {
                // Fallback to default XML drawable color if hex parsing fails
                holder.layoutBadgeBg.setBackgroundTintList(null);
            }
        } else {
            // Reset to default if no color is provided (crucial for View Recycling)
            holder.layoutBadgeBg.setBackgroundTintList(null);
        }
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBadgeBg;
        TextView tvBadgeIcon;
        TextView tvBadgeTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Map the layout container that holds the background drawable
            layoutBadgeBg = itemView.findViewById(R.id.layoutBadgeBg);
            tvBadgeIcon = itemView.findViewById(R.id.tvBadgeIcon);
            tvBadgeTitle = itemView.findViewById(R.id.tvBadgeTitle);
        }
    }
}