package com.example.cashify.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cashify.database.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends ArrayAdapter<Category> {
    private List<Integer> disabledIds = new ArrayList<>();

    public CategoryAdapter(@NonNull Context context, List<Category> categories) {
        super(context, android.R.layout.simple_dropdown_item_1line, categories);
    }

    // Hàm để cập nhật danh sách các ID cần khóa từ Fragment truyền qua
    public void setDisabledIds(List<Integer> ids) {
        if (ids != null) {
            this.disabledIds = ids;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        setupView(position, view);
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        setupView(position, view);
        return view;
    }

    private void setupView(int position, View view) {
        TextView text = (TextView) view.findViewById(android.R.id.text1);
        Category item = getItem(position);

        if (item != null) {
            text.setText(item.name);
            // LOGIC: Nếu ID nằm trong danh sách đã có Budget thì làm mờ 40%
            if (disabledIds.contains(item.id)) {
                view.setAlpha(0.4f);
                view.setBackgroundColor(Color.parseColor("#F5F5F5")); // Màu xám nhẹ cho biết đã bị khóa
            } else {
                view.setAlpha(1.0f);
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    @Override
    public boolean isEnabled(int position) {
        // KHÓA CLICK: Mục nào có ID trong danh sách disabled sẽ không bấm được
        Category item = getItem(position);
        return item != null && !disabledIds.contains(item.id);
    }
}