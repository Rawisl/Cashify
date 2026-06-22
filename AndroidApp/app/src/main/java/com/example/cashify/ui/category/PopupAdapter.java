package com.example.cashify.ui.category;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PopupAdapter {

    // =========================================================================
    // ICON ADAPTER
    // =========================================================================
    public static class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private final Context context;
        private final int[] icons;
        private final OnItemClickListener listener;
        private int selectedPosition = -1; // Biến lưu vị trí đang chọn

        public interface OnItemClickListener {
            void onClick(int position);
        }

        public IconAdapter(Context context, int[] icons, OnItemClickListener listener) {
            this.context = context;
            this.icons = icons;
            this.listener = listener;
        }

        // Hàm hỗ trợ prefill khi mở chế độ Edit
        @SuppressLint("NotifyDataSetChanged")
        public void setSelectedPosition(int position) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(context);

            int size = (int) (45 * context.getResources().getDisplayMetrics().density);
            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            imageView.setLayoutParams(params);

            imageView.setPadding(margin, margin, margin, margin);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageResource(icons[position]);

            // Hiệu ứng Visual Feedback cho Icon được chọn
            if (selectedPosition == position) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadius(24f);
                bg.setColor(Color.parseColor("#E8EAF6")); // Nền xanh pastel nhẹ
                bg.setStroke(3, Color.parseColor("#1A237E")); // Viền màu brand_primary
                holder.imageView.setBackground(bg);
            } else {
                holder.imageView.setBackground(null);
            }

            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                if (selectedPosition == RecyclerView.NO_POSITION) return;

                // Chỉ vẽ lại 2 ô thay đổi để tối ưu hiệu suất
                if (oldPos != -1) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);

                if (listener != null) listener.onClick(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }

    // =========================================================================
    // COLOR ADAPTER
    // =========================================================================
    public static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
        private final Context context;
        private final String[] colors;
        private final OnItemClickListener listener;
        private int selectedPosition = -1; // Biến lưu vị trí đang chọn

        public interface OnItemClickListener {
            void onClick(int position);
        }

        public ColorAdapter(Context context, String[] colors, OnItemClickListener listener) {
            this.context = context;
            this.colors = colors;
            this.listener = listener;
        }

        // Hàm hỗ trợ prefill khi mở chế độ Edit
        @SuppressLint("NotifyDataSetChanged")
        public void setSelectedPosition(int position) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View colorView = new View(context);

            int size = (int) (35 * context.getResources().getDisplayMetrics().density);
            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);

            return new ViewHolder(colorView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);

            try {
                shape.setColor(Color.parseColor(colors[position]));
            } catch (Exception e) {
                shape.setColor(Color.GRAY);
            }

            // Hiệu ứng Visual Feedback cho Màu được chọn (Phóng to + Đổ bóng)
            if (selectedPosition == position) {
                holder.colorView.setScaleX(1.3f);
                holder.colorView.setScaleY(1.3f);
                holder.colorView.setElevation(8f);
                shape.setStroke(4, Color.WHITE); // Viền trắng mỏng bên trong để tạo điểm nhấn
            } else {
                holder.colorView.setScaleX(1.0f);
                holder.colorView.setScaleY(1.0f);
                holder.colorView.setElevation(0f);
                shape.setStroke(0, Color.TRANSPARENT);
            }

            holder.colorView.setBackground(shape);

            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                if (selectedPosition == RecyclerView.NO_POSITION) return;

                // Chỉ vẽ lại 2 ô thay đổi
                if (oldPos != -1) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);

                if (listener != null) listener.onClick(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return colors.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            View colorView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                colorView = itemView;
            }
        }
    }
}