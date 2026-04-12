package com.example.cashify.ui.transactions;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Category;
import java.util.List;

public class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.ViewHolder> {
    private List<Category> list;
    private Context context;
    private int selectedPosition = -1;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryPickerAdapter(Context context, List<Category> list, OnCategoryClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_category_picker, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        holder.tvName.setText(item.name);

        // 1. Lấy icon từ drawable
        int resId = context.getResources().getIdentifier(item.iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_other);

        // --- 2. XỬ LÝ MÀU SẮC (FIX CRASH TẠI ĐÂY) ---
        int color;
        try {
            // Chuyển chuỗi ID từ DB thành số nguyên, sau đó lấy màu thực tế từ Resource
            int colorResId = Integer.parseInt(item.colorCode);
            color = ContextCompat.getColor(context, colorResId);
        } catch (Exception e) {
            // Phòng hờ dữ liệu cũ hoặc lỗi, mặc định dùng màu brand_primary hoặc đen
            color = ContextCompat.getColor(context, R.color.brand_primary);
        }

        // Tạo màu Pastel (Alpha = 51 tương đương khoảng 20%)
        int pastel = Color.argb(51, Color.red(color), Color.green(color), Color.blue(color));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(pastel);

        // 3. Nếu được chọn thì hiện viền đậm
        if (selectedPosition == position) {
            shape.setStroke(4, color);
            holder.tvName.setTextColor(color);
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD); // Làm đậm chữ khi chọn
        } else {
            shape.setStroke(0, Color.TRANSPARENT);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.item_description));
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.imgIcon.setBackground(shape);
        holder.imgIcon.setImageTintList(ColorStateList.valueOf(color));

        // 4. Click listener
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(item);
        });
    }

    public void setSelectedById(int categoryId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == categoryId) {
                int oldPos = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                // Cập nhật listener để Activity/ViewModel biết category này đã được chọn
                listener.onCategoryClick(list.get(i));
                break;
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCatIcon);
            tvName = itemView.findViewById(R.id.tvCatName);
        }
    }
}