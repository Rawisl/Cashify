package com.example.cashify.ui.category;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PopupAdapter {

    // --- 1. ADAPTER CHO ICON ---
    public static class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private final Context context;
        private final int[] icons;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onClick(int position);
        }

        public IconAdapter(Context context, int[] icons, OnItemClickListener listener) {
            this.context = context;
            this.icons = icons;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(context);

            // Quy đổi sang DP cho chuẩn màn hình (Khoảng 45dp)
            int size = (int) (45 * context.getResources().getDisplayMetrics().density);
            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            imageView.setLayoutParams(params);

            // Padding cho icon khỏi chạm viền
            imageView.setPadding(margin, margin, margin, margin);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageResource(icons[position]);
            holder.itemView.setOnClickListener(v -> listener.onClick(position));
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

    // --- 2. ADAPTER CHO MÀU SẮC ---
    public static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
        private final Context context;
        private final String[] colors;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onClick(int position);
        }

        public ColorAdapter(Context context, String[] colors, OnItemClickListener listener) {
            this.context = context;
            this.colors = colors;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View colorView = new View(context);

            // Quy đổi kích thước màu (Khoảng 35dp)
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
            shape.setShape(GradientDrawable.OVAL); // Vẫn dùng hình tròn cho tinh tế
            try {
                shape.setColor(Color.parseColor(colors[position]));
            } catch (Exception e) {
                shape.setColor(Color.GRAY);
            }
            holder.colorView.setBackground(shape);

            holder.itemView.setOnClickListener(v -> listener.onClick(position));
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