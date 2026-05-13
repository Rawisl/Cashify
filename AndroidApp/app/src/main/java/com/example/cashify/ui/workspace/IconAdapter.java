package com.example.cashify.ui.workspace;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;

import java.util.List;

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

    private final Context context;
    private final List<String> iconNames; // Lưu tên file icon (VD: "ic_food", "ic_home")
    private int selectedPosition = 0; // Mặc định chọn cái đầu tiên

    public IconAdapter(Context context, List<String> iconNames) {
        this.context = context;
        this.iconNames = iconNames;
    }

    // Hàm cực quan trọng để BottomSheet lấy được tên icon đang chọn
    public String getSelectedIconName() {
        return iconNames.get(selectedPosition);
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_icon, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position);

        // 1. Biến tên String (VD: "ic_home") thành ID drawable thực tế
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_other); // Backup nếu không tìm thấy
        }

        // 2. Xử lý UI đang chọn hay không chọn
        if (selectedPosition == position) {
            // Đang chọn -> Nền viền xanh, Icon màu nổi
            holder.layoutIconContainer.setBackgroundResource(R.drawable.bg_icon_selected);
            holder.imgIcon.setColorFilter(Color.parseColor("#313B60")); // Màu xanh dương đậm
        } else {
            // Không chọn -> Nền trong suốt, Icon màu xám
            holder.layoutIconContainer.setBackgroundResource(R.drawable.bg_icon_unselected);
            holder.imgIcon.setColorFilter(Color.parseColor("#6B7280")); // Màu xám nhạt
        }

        // 3. Sự kiện bấm vào 1 icon
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            // Chỉ vẽ lại 2 cục thay đổi cho app chạy mượt, không cần vẽ lại cả list
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return iconNames != null ? iconNames.size() : 0;
    }

    public static class IconViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIconContainer;
        ImageView imgIcon;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIconContainer = itemView.findViewById(R.id.layoutIconContainer);
            imgIcon = itemView.findViewById(R.id.imgIcon);
        }
    }
}