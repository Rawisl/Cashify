package com.example.cashify.ui.category;

import android.annotation.SuppressLint;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> list;
    private final Context context;
    private final OnCategoryListener listener;

    public interface OnCategoryListener {
        // Delegates actions to the View layer (Activity/Fragment)
        void onDeleteClick(Category category);
        void onEditClick(Category category);
        void onRestoreClick(Category category);
    }

    public CategoryAdapter(Context context, List<Category> list, OnCategoryListener listener) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Safely updates the adapter's data set and refreshes the UI.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Category> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = list.get(position);

        // =========================================================================
        // 1. DYNAMIC ICON LOADING
        // =========================================================================
        String iconName = (category.iconName != null && !category.iconName.isEmpty()) ? category.iconName : "ic_food";
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        // =========================================================================
        // 2. DYNAMIC COLOR STYLING (Pastel Background + Solid Icon)
        // =========================================================================
        String colorStr = (category.colorCode != null && !category.colorCode.trim().isEmpty()) ? category.colorCode.trim() : "#4CAF50";
        try {
            int originColor = Color.parseColor(colorStr);
            // Create a pastel version of the color (20% opacity = 51 alpha)
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

        // =========================================================================
        // 3. UI STATE MANAGEMENT (Active vs. Soft-Deleted)
        // =========================================================================
        if (category.isDeleted == 1) {
            // Soft-Deleted State
            holder.tvName.setText(category.name + " (Hidden)");
            holder.tvName.setTextColor(Color.GRAY);
            holder.itemView.setAlpha(0.5f); // Dim the item to indicate inactive status

            holder.btnDelete.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnRestore.setVisibility(View.VISIBLE);

            // Disable long click when hidden
            holder.itemView.setOnLongClickListener(null);

            // Action: Restore
            holder.btnRestore.setOnClickListener(v -> listener.onRestoreClick(category));

        } else {
            // Active State
            holder.tvName.setText(category.name);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.item_title));
            holder.itemView.setAlpha(1.0f);

            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnRestore.setVisibility(View.GONE);

            // Action: Long press to edit
            holder.itemView.setOnLongClickListener(v -> {
                listener.onEditClick(category);
                return true; // Consume the long-click event
            });

            // Actions: Standard Edit / Delete
            holder.btnEdit.setOnClickListener(v -> listener.onEditClick(category));
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(category));
        }

        // Removed the redundant listeners that were duplicated here previously!
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        ImageButton btnEdit, btnDelete, btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRestore = itemView.findViewById(R.id.btnRestore);
        }
    }
}