package com.example.cashify.CategoryManagement;

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
     * Adapter to display a grid of selectable icons (ImageResources).
     */
    public static class IconGridAdapter extends BaseAdapter {
        private Context context;
        private int[] icons; // Array of drawable resource IDs (e.g., R.drawable.ic_food)

        public IconGridAdapter(Context context, int[] icons) {
            this.context = context;
            this.icons = icons;
        }

        @Override public int getCount() { return icons.length; }
        @Override public Object getItem(int i) { return icons[i]; }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Logic: Dynamically create an ImageView for each icon in the array
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(icons[i]);

            // Set fixed size for grid items (150x150 pixels)
            imageView.setLayoutParams(new GridView.LayoutParams(150, 150));

            // Add padding so icons don't touch the edges of their grid cell
            imageView.setPadding(25, 25, 25, 25);
            return imageView;
        }
    }

    /**
     * Adapter to display a grid of selectable colors.
     */
    public static class ColorGridAdapter extends BaseAdapter {
        private Context context;
        private String[] colors; // Array of Hex strings (e.g., "#F44336")

        public ColorGridAdapter(Context context, String[] colors) {
            this.context = context;
            this.colors = colors;
        }

        @Override public int getCount() { return colors.length; }
        @Override public Object getItem(int i) { return colors[i]; }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Logic: Create a simple View to act as a colored square/rectangle
            View colorView = new View(context);

            // Set size for the color preview circle/square
            colorView.setLayoutParams(new GridView.LayoutParams(100, 100));

            // Trong getView của ColorGridAdapter
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL); // Chọn màu thì nên để hình tròn cho tinh tế
            shape.setColor(Color.parseColor(colors[i]));
            colorView.setBackground(shape);

            colorView.setBackground(shape);
            return colorView;
        }
    }
}