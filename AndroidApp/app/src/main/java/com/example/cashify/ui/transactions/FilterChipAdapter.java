package com.example.cashify.ui.transactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class FilterChipAdapter extends RecyclerView.Adapter<FilterChipAdapter.ChipViewHolder> {

    private List<FilterChip> chips = new ArrayList<>();
    private final OnChipClickListener listener;

    public interface OnChipClickListener {
        void onChipClick(FilterChip chip, int position, View anchorView);
        void onChipClearClick(FilterChip chip, int position);
    }

    public FilterChipAdapter(OnChipClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setChips(List<FilterChip> newChips) {
        this.chips = newChips != null ? newChips : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        FilterChip chipData = chips.get(position);
        Chip chipView = holder.filterChip;
        Context context = chipView.getContext();

        // Update text based on active state
        chipView.setText(chipData.isActive() ? chipData.getActiveLabel() : chipData.getFilLabel());
        chipView.setChipIconVisible(false);
        chipView.setCheckedIconVisible(false);

        // Dynamically update UI (Colors & Icons) based on state
        if (chipData.isActive()) {
            // --- ACTIVE STATE ---
            chipView.setChipBackgroundColorResource(R.color.status_background_green);
            chipView.setChipStrokeColorResource(R.color.status_green);
            chipView.setTextColor(ContextCompat.getColor(context, R.color.status_green));

            chipView.setCloseIconVisible(true);
            chipView.setCloseIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            chipView.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary)));

            // Action: Clear filter
            chipView.setOnCloseIconClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onChipClearClick(chipData, adapterPosition);
                }
            });

        } else {
            // --- INACTIVE STATE ---
            chipView.setChipBackgroundColorResource(R.color.white);
            chipView.setChipStrokeColorResource(R.color.cardview_divider);
            chipView.setTextColor(ContextCompat.getColor(context, R.color.item_title));

            chipView.setCloseIconVisible(true);
            chipView.setCloseIconResource(android.R.drawable.arrow_down_float); // Expand arrow
            chipView.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.item_title)));

            // Action: Open filter menu
            chipView.setOnCloseIconClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onChipClick(chipData, adapterPosition, chipView);
                }
            });
        }

        // Action: Open filter menu via body click
        chipView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onChipClick(chipData, adapterPosition, chipView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chips.size();
    }

    static class ChipViewHolder extends RecyclerView.ViewHolder {
        Chip filterChip;

        public ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            filterChip = (Chip) itemView;
        }
    }
}