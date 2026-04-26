package com.example.cashify.ui.transactions;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
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
        // Giữ nguyên layout item cũ của bạn
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_category_picker, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        holder.tvName.setText(item.name);

        // 1. Lấy icon từ drawable
        int resId = context.getResources().getIdentifier(item.iconName, "drawable", context.getPackageName());
        holder.imgIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_other);

        // --- 2. XỬ LÝ MÀU SẮC GỐC ---
        int color;
        try {
            color = Color.parseColor(item.colorCode);
        } catch (Exception e) {
            color = ContextCompat.getColor(context, R.color.brand_primary);
        }

        // Luôn nhuộm màu icon theo màu gốc của Category
        holder.imgIcon.setImageTintList(ColorStateList.valueOf(color));


        // --- 3. XỬ LÝ BACKGROUND PASTEL KHI ĐƯỢC CHỌN (CHỈNH SỬA CHÍNH TẠI ĐÂY) ---

        // Tạo sẵn một GradientDrawable để làm nền bo tròn
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(context, 30)); // Bo tròn góc (ví dụ 12dp)

        if (selectedPosition == position) {
            // NẾU ĐƯỢC CHỌN:
            // TẠO MÀU PASTEL cực nhạt (Alpha thấp, ví dụ 30 hoặc 40 trên 255)
            int pastelColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color));

            shape.setColor(pastelColor); // Đặt màu nền pastel
            shape.setStroke(0, Color.TRANSPARENT); // Không dùng viền nữa

            holder.tvName.setTextColor(color); // Màu chữ đậm theo màu gốc
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD); // Chữ in đậm
        } else {
            // NẾU KHÔNG ĐƯỢC CHỌN:
            shape.setColor(Color.TRANSPARENT); // Nền trong suốt
            shape.setStroke(0, Color.TRANSPARENT);

            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.item_description)); // Màu chữ mặc định
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL); // Chữ bình thường
        }

        // ĐIỂM QUAN TRỌNG: Gán background này cho itemView (toàn bộ ô), KHÔNG phải imgIcon
        holder.itemView.setBackground(shape);

        // Xóa background cũ của icon (nếu lúc trước item_category_picker.xml có đặt)
        holder.imgIcon.setBackground(null);


        // 4. Click listener
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Chỉ notify những item thay đổi trạng thái để tối ưu hiệu năng
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);

            listener.onCategoryClick(item);
        });
    }

    // Hàm tiện ích để đổi dp sang px cho CornerRadius
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public void setSelectedById(int categoryId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == categoryId) {
                int oldPos = selectedPosition;
                selectedPosition = i;

                if (oldPos != -1) {
                    notifyItemChanged(oldPos);
                }
                notifyItemChanged(selectedPosition);

                listener.onCategoryClick(list.get(i));
                break;
            }
        }
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