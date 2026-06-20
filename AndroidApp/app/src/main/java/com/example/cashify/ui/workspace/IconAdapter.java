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
    private final List<String> iconNames; // Stores drawable resource names (e.g., "ic_food", "ic_home")
    private int selectedPosition = 0;     // Defaults to the first item

    public IconAdapter(Context context, List<String> iconNames) {
        this.context = context;
        this.iconNames = iconNames;
    }

    /**
     * Required by BottomSheet to fetch the currently selected icon name.
     */
    public String getSelectedIconName() {
        if (iconNames != null && !iconNames.isEmpty() && selectedPosition >= 0 && selectedPosition < iconNames.size()) {
            return iconNames.get(selectedPosition);
        }
        return "ic_other"; // Safe fallback
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

        // 1. Resolve String name (e.g., "ic_home") into actual drawable ID
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_other); // Fallback if missing
        }

        // 2. Handle Selection UI State
        if (selectedPosition == position) {
            // Selected: Blue border container, Deep Blue icon
            holder.layoutIconContainer.setBackgroundResource(R.drawable.bg_icon_selected);
            holder.imgIcon.setColorFilter(Color.parseColor("#313B60"));
        } else {
            // Unselected: Transparent container, Light Gray icon
            holder.layoutIconContainer.setBackgroundResource(R.drawable.bg_icon_unselected);
            holder.imgIcon.setColorFilter(Color.parseColor("#6B7280"));
        }

        // 3. Handle Item Selection Click
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Only re-bind the two affected items to optimize render performance
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected);
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconNames != null ? iconNames.size() : 0;
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
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