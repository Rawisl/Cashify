package com.example.cashify.CategoryManagement;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
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

    // Interface to handle clicks in the Activity/Fragment
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
        // Inflate the custom XML layout for a single item row
        View v = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = list.get(position);
        holder.tvName.setText(category.name);

        // --- 1. Dynamic Icon Loading ---
        // Converts a string (e.g., "ic_food") into a Resource ID.
        // This allows you to store just the name in the database.
        String iconName = (category.iconName != null && !category.iconName.isEmpty()) ? category.iconName : "ic_food";
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        // --- 2. Dynamic Color & Pastel Background Styling ---
        String colorStr = (category.colorCode != null && !category.colorCode.trim().isEmpty()) ? category.colorCode.trim() : "#4CAF50";

        try {
            int originColor = Color.parseColor(colorStr);

            /* Logic: Create a "Pastel" effect by setting Alpha to 51 (approx 20% opacity).
               Color.argb(alpha, red, green, blue) where 255 is fully opaque.
            */
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            // Dynamically create a rounded background (shape)
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(25f);
            shape.setColor(pastelColor);

            holder.imgIcon.setBackground(shape);

            // Tint the icon itself with the original solid color
            holder.imgIcon.setColorFilter(originColor, PorterDuff.Mode.SRC_IN);
        } catch (Exception e) {
            // Fallback UI if the color string is invalid
            int fallback = Color.parseColor("#4CAF50");
            holder.imgIcon.setColorFilter(fallback, PorterDuff.Mode.SRC_IN);
            holder.imgIcon.setBackgroundColor(Color.TRANSPARENT);
        }

        // --- 3. Event Listeners ---
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Xóa danh mục '" + category.name + "'?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        // Room database operations MUST NOT run on the Main Thread.
                        // We use a SingleThreadExecutor for background processing.
                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(context).categoryDao().softDelete(category.id);

                            // UI updates (like refreshing the list) MUST happen back on the Main Thread.
                            ((CategoryManagement)context).runOnUiThread(() -> listener.onDeleteSuccess());
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(category);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ViewHolder holds references to the views to avoid calling findViewById() repeatedly
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