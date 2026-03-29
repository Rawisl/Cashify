package com.example.cashify.CategoryManagement;

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
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;

import java.util.List;
import java.util.concurrent.Executors;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> list;
    private Context context;
    private OnCategoryListener listener;

    private static final String DEFAULT_COLOR = "#4C6FFF";

    public interface OnCategoryListener {
        void onDeleteSuccess();
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

        // --- 1. Load Icon động ---
        String iconName = (category.iconName != null && !category.iconName.isEmpty()) ? category.iconName : "ic_other";
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        // --- 2. Xử lý Màu sắc & Nền Circle Pastel (Style Settings) ---
        String colorStr = (category.colorCode != null && !category.colorCode.trim().isEmpty()) ? category.colorCode.trim() : DEFAULT_COLOR;

        try {
            int originColor = Color.parseColor(colorStr);
            int pastelColor = Color.argb(40, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            // 1. Set màu icon
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(originColor));

            // 2. Tạo nền hình vuông bo góc bằng code (thay vì dùng BackgroundTintList)
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);

            // Bo góc 14dp (tính theo pixel) - số này càng lớn bo càng mạnh
            float radius = 14 * context.getResources().getDisplayMetrics().density;
            gd.setCornerRadius(radius);
            gd.setColor(pastelColor);

            holder.imgIcon.setBackground(gd);

            // Đảm bảo icon không bị to quá
            holder.imgIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        } catch (Exception e) {
            int defaultColor = Color.parseColor(DEFAULT_COLOR);
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(defaultColor));

            int pastelColor = Color.argb(40, Color.red(defaultColor), Color.green(defaultColor), Color.blue(defaultColor));

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            gd.setColor(pastelColor);
            holder.imgIcon.setBackground(gd);
        }

        // --- 3. Sự kiện Click ---
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Xóa danh mục '" + category.name + "'?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(context).categoryDao().softDelete(category.id);
                            if (context instanceof CategoryManagement) {
                                ((CategoryManagement)context).runOnUiThread(() -> listener.onDeleteSuccess());
                            }
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Click vào cả dòng để sửa
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(category);
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