package com.example.cashify.ui.category;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> list;
    private final Context context;
    private final OnCategoryListener listener;

    public interface OnCategoryListener {
        // Chỉ cần báo là muốn xóa hoặc sửa, không cần xử lý logic ở đây
        void onDeleteClick(Category category);
        void onEditClick(Category category);
    }

    public CategoryAdapter(Context context, List<Category> list, OnCategoryListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = list.get(position);
        holder.tvName.setText(category.name);

        // --- 1. Dynamic Icon Loading ---
        String iconName = (category.iconName != null && !category.iconName.isEmpty()) ? category.iconName : "ic_food";
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        // --- 2. Styling (Giữ nguyên logic Pastel của bạn vì nó đẹp) ---
        String colorStr = (category.colorCode != null && !category.colorCode.trim().isEmpty()) ? category.colorCode.trim() : "#4CAF50";
        try {
            int originColor = Color.parseColor(colorStr);
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(25f);
            shape.setColor(pastelColor);

            holder.imgIcon.setBackground(shape);
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        }

        // --- 3. Event Listeners (Gửi tín hiệu về cho Activity) ---
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(category));

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(category));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onEditClick(category);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}