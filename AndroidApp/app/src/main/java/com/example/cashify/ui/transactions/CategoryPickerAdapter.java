package com.example.cashify.ui.transactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.example.cashify.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.ViewHolder> {

    private List<Category> list;
    private final Context context;
    private int selectedPosition = -1;
    private final OnCategoryClickListener listener;
    private String selectedFirestoreId = "";

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryPickerAdapter(Context context, List<Category> list, OnCategoryClickListener listener) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        holder.tvName.setText(item.name != null ? item.name : "Unknown");

        // Smart match: Check locally generated int ID OR synced firestoreId
        boolean isSelected = (selectedPosition == position) ||
                (item.firestoreId != null && item.firestoreId.equals(selectedFirestoreId));

        // 1. Icon Binding
        String iconName = item.iconName != null ? item.iconName : "";
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_other);

        // 2. Base Color Resolution
        int color;
        try {
            color = Color.parseColor(item.colorCode);
        } catch (Exception e) {
            color = ContextCompat.getColor(context, R.color.brand_primary);
        }

        // Apply base color to the vector icon
        holder.imgIcon.setImageTintList(ColorStateList.valueOf(color));

        // 3. Dynamic Selection Styling (Pastel Background)
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(30));

        if (isSelected) {
            // Selected: Pastel Background (~15% opacity), Bold colored text
            int pastelColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color));

            shape.setColor(pastelColor);
            shape.setStroke(0, Color.TRANSPARENT);

            holder.tvName.setTextColor(color);
            holder.tvName.setTypeface(null, Typeface.BOLD);
        } else {
            // Unselected: Transparent background, Neutral normal text
            shape.setColor(Color.TRANSPARENT);
            shape.setStroke(0, Color.TRANSPARENT);

            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.item_description));
            holder.tvName.setTypeface(null, Typeface.NORMAL);
        }

        // Apply generated shape to the root container
        holder.itemView.setBackground(shape);
        holder.imgIcon.setBackground(null);

        // 4. Click Listener Delegation
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (selectedPosition == RecyclerView.NO_POSITION) return;

            // Notify only modified items to optimize render cycle
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) listener.onCategoryClick(item);
        });
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public void setSelectedById(int categoryId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == categoryId) {
                int oldPos = selectedPosition;
                selectedPosition = i;

                if (oldPos != -1) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);

                if (listener != null) listener.onCategoryClick(list.get(i));
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedByFirestoreId(String firestoreId) {
        this.selectedFirestoreId = (firestoreId != null) ? firestoreId : "";
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).firestoreId != null && list.get(i).firestoreId.equals(firestoreId)) {
                selectedPosition = i;
                notifyDataSetChanged();
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNewData(List<Category> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        this.selectedPosition = -1; // Reset selection on tab changes (Income/Expense)
        this.selectedFirestoreId = "";
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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