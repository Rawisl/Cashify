package com.example.cashify.ui.transactions;

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

    public void setChips(List<FilterChip> newChips) {
        this.chips = newChips;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout mới chỉ có 1 thẻ Chip
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        FilterChip chipData = chips.get(position);
        Chip chipView = holder.filterChip;
        Context context = chipView.getContext();

        // Set nội dung hiển thị
        chipView.setText(chipData.isActive() ? chipData.getActiveLabel() : chipData.getFilLabel());

        // Thay đổi Giao diện (Màu sắc & Icon) trực tiếp bằng code
        if (chipData.isActive()) {
            // --- ACTIVE STATE ---
            chipView.setChipBackgroundColorResource(R.color.status_background_green);
            chipView.setChipStrokeColorResource(R.color.status_green);
            chipView.setTextColor(ContextCompat.getColor(context, R.color.status_green));

            chipView.setCloseIconVisible(true);
            chipView.setCloseIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            // Đổi màu icon X thành xanh
            chipView.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary)));

            // Bấm vào dấu X để xóa lọc
            chipView.setOnCloseIconClickListener(v -> listener.onChipClearClick(chipData, position));

        } else {
            // --- INACTIVE STATE ---
            chipView.setChipBackgroundColorResource(R.color.white);
            chipView.setChipStrokeColorResource(R.color.cardview_divider);
            chipView.setTextColor(ContextCompat.getColor(context, R.color.item_title));

            chipView.setCloseIconVisible(true);
            chipView.setCloseIconResource(android.R.drawable.arrow_down_float);
            // Đổi màu icon mũi tên thành xám
            chipView.setCloseIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.item_title)));

            // Bấm vào icon mũi tên để mở menu
            chipView.setOnCloseIconClickListener(v -> listener.onChipClick(chipData, position, chipView));
        }

        // Bấm vào phần chữ của Chip để mở menu
        chipView.setOnClickListener(v -> listener.onChipClick(chipData, position, chipView));
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