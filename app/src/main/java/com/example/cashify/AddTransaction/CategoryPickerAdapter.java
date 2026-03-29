package com.example.cashify.AddTransaction;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Category;
import java.util.List;

public class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.ViewHolder> {
    private List<Category> list;
    private Context context;
    private int selectedPosition = -1;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryPickerAdapter(Context context, List<Category> list, OnCategoryClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_category_picker, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        holder.tvName.setText(item.name);

        // Lấy icon từ drawable
        int resId = context.getResources().getIdentifier(item.iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_other);

        // Xử lý màu sắc
        int color = Color.parseColor(item.colorCode);
        int pastel = Color.argb(51, Color.red(color), Color.green(color), Color.blue(color));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(pastel);

        // Nếu được chọn thì hiện viền đậm
        if (selectedPosition == position) {
            shape.setStroke(4, color);
            holder.tvName.setTextColor(color);
            holder.tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); // Có thể thêm icon check nhỏ
        } else {
            shape.setStroke(0, Color.TRANSPARENT);
            holder.tvName.setTextColor(context.getResources().getColor(R.color.item_description));
        }

        holder.imgIcon.setBackground(shape);
        holder.imgIcon.setImageTintList(ColorStateList.valueOf(color));

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(item);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

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