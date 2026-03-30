package com.example.cashify.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cashify.R;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<BudgetWithSpent> budgetList = new ArrayList<>();
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onBudgetClick(BudgetWithSpent item);
    }

    public BudgetAdapter(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    public void setBudgets(List<BudgetWithSpent> budgets) {
        this.budgetList = budgets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetWithSpent item = budgetList.get(position);

        double spent = item.spentAmount;
        double limit = item.limitAmount;
        int percent = limit > 0 ? (int) ((spent / limit) * 100) : 0;
        double remaining = limit - spent;

        String nameToShow = (item.categoryName != null) ? item.categoryName : ("Danh mục " + item.categoryId);
        holder.tvCategoryName.setText(nameToShow);

        // === ĐOẠN CODE LÔI ICON TỪ DATABASE LÊN ĐÂY KHI CÓ ICON===
//        String iconName = item.categoryIcon; // Kiểm tra lại tên biến này trong class BudgetWithSpent
//
//        if (iconName != null && !iconName.isEmpty()) {
//            // Biến cái tên String (VD: "ic_food") thành ID (int) trong res/drawable
//            int resId = holder.itemView.getContext().getResources().getIdentifier(
//                    iconName,
//                    "drawable",
//                    holder.itemView.getContext().getPackageName()
//            );
//
//            if (resId != 0) {
//                holder.ivCategoryIcon.setImageResource(resId);
//            } else
//            {
//                // Nếu lỡ trong DB lưu tên icon mà trong máy không có file ảnh đó
//                holder.ivCategoryIcon.setImageResource(R.drawable.food_drinking_solid);
//            }
//        } else {
//            // Nếu DB chưa có dữ liệu icon
//            holder.ivCategoryIcon.setImageResource(R.drawable.food_drinking_solid);
//        }


        // =========================================================
        // === 1. ĐEM LÒ ĐÚC TIỀN VÀO SỬ DỤNG Ở ĐÂY ===
        // =========================================================
        String shortSpent = CurrencyFormatter.formatCompactVND(spent);
        String shortLimit = CurrencyFormatter.formatCompactVND(limit);

        // Gắn số đã format lên UI
        holder.tvSpentAndLimit.setText(shortSpent + " / " + shortLimit);

        // Cập nhật % và Progress Bar
        holder.tvPercent.setText(percent + "%");
        holder.pbBudget.setMax(100);
        holder.pbBudget.setProgress(Math.min(percent, 100));

        // LOGIC ĐỔI MÀU & HIỆN CẢNH BÁO
        if (percent >= 100) {
            String redColor = "#E53935";
            holder.pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(redColor)));
            holder.tvPercent.setTextColor(android.graphics.Color.parseColor(redColor));
            holder.tvAlertMessage.setVisibility(View.VISIBLE);

            // CẢNH BÁO: Rút gọn luôn số tiền vượt để tránh vỡ Layout
            String overSpent = CurrencyFormatter.formatCompactVND(Math.abs(remaining));
            holder.tvAlertMessage.setText("Vượt ngân sách " + overSpent);
            holder.tvAlertMessage.setTextColor(android.graphics.Color.parseColor(redColor));

        } else if (percent >= 80) {
            String orangeColor = "#FB8C00";
            holder.pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(orangeColor)));
            holder.tvPercent.setTextColor(android.graphics.Color.parseColor(orangeColor));
            holder.tvAlertMessage.setVisibility(View.VISIBLE);

            // CẢNH BÁO: Rút gọn số tiền còn lại
            String leftAmount = CurrencyFormatter.formatCompactVND(remaining);
            holder.tvAlertMessage.setText("Chỉ còn " + leftAmount);
            holder.tvAlertMessage.setTextColor(android.graphics.Color.parseColor(orangeColor));

        } else {
            String blueColor = "#4C6FFF";
            holder.pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(blueColor)));
            holder.tvPercent.setTextColor(android.graphics.Color.parseColor("#000000"));
            holder.tvAlertMessage.setVisibility(View.GONE);
        }

        //bắt sự kiện click để mở numpad
        holder.itemView.setOnClickListener(v -> listener.onBudgetClick(item));
    }

    @Override
    public int getItemCount() { return budgetList.size(); }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvSpentAndLimit, tvPercent, tvAlertMessage;
        ProgressBar pbBudget;
        ImageView ivCategoryIcon, ivEdit;
        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvSpentAndLimit = itemView.findViewById(R.id.tvSpentAndLimit);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            tvAlertMessage = itemView.findViewById(R.id.tvAlertMessage);
            pbBudget = itemView.findViewById(R.id.pbBudget);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            ivEdit = itemView.findViewById(R.id.ivEdit);
        }
    }
}