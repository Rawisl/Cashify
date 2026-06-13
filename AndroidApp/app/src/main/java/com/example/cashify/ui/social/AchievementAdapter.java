package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.ApiService.AchievementSuggestion;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<AchievementSuggestion> achievements;
    private final OnAchievementClickListener listener;

    // Interface để truyền sự kiện click ngược ra ngoài Fragment
    public interface OnAchievementClickListener {
        void onAchievementClick(AchievementSuggestion achievement);
    }

    public AchievementAdapter(List<AchievementSuggestion> achievements, OnAchievementClickListener listener) {
        this.achievements = achievements;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementSuggestion currentItem = achievements.get(position);

        // Đổ dữ liệu text và icon emoji vào UI
        if (currentItem.iconText != null) {
            holder.tvIcon.setText(currentItem.iconText);
        }

        if (currentItem.title != null) {
            holder.tvTitle.setText(currentItem.title);
        }

        if (currentItem.description != null) {
            holder.tvDesc.setText(currentItem.description);
        }

        // Bắt sự kiện người dùng bấm chọn 1 thẻ thành tựu
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAchievementClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return achievements != null ? achievements.size() : 0;
    }

    // Ánh xạ các view từ file item_achievement_suggestion.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvTitle;
        TextView tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvAchievementIcon);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDesc = itemView.findViewById(R.id.tvAchievementDesc);
        }
    }
}