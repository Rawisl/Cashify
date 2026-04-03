package com.example.cashify.ui.category;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * A helper class containing adapters for the Icon and Color selection grids.
 */
public class PopupAdapter {

    /**
     * IconGridAdapter: Tái sử dụng view để mượt mà khi cuộn
     */
    public static class IconGridAdapter extends BaseAdapter {
        private Context context;
        private int[] icons;

        public IconGridAdapter(Context context, int[] icons) {
            this.context = context;
            this.icons = icons;
        }

        @Override public int getCount() { return icons.length; }
        @Override public Object getItem(int i) { return icons[i]; }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView imageView;
            if (view == null) {
                // Chỉ khởi tạo lần đầu, các lần sau dùng lại view cũ
                imageView = new ImageView(context);
                // Tip: 150px này nếu muốn chuẩn nên convert từ DP sang PX
                imageView.setLayoutParams(new GridView.LayoutParams(150, 150));
                imageView.setPadding(25, 25, 25, 25);
            } else {
                imageView = (ImageView) view;
            }

            imageView.setImageResource(icons[i]);
            return imageView;
        }
    }

    /**
     * ColorGridAdapter: Chỉnh lại hình tròn cho tinh tế
     */
    public static class ColorGridAdapter extends BaseAdapter {
        private Context context;
        private String[] colors;

        public ColorGridAdapter(Context context, String[] colors) {
            this.context = context;
            this.colors = colors;
        }

        @Override public int getCount() { return colors.length; }
        @Override public Object getItem(int i) { return colors[i]; }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View colorView;
            if (view == null) {
                colorView = new View(context);
                colorView.setLayoutParams(new GridView.LayoutParams(100, 100));
            } else {
                colorView = view;
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            try {
                shape.setColor(Color.parseColor(colors[i]));
            } catch (Exception e) {
                shape.setColor(Color.GRAY); // Fallback nếu mã màu lỗi
            }

            colorView.setBackground(shape);
            return colorView;
        }
    }
}