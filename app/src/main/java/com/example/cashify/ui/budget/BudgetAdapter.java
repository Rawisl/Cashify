package com.example.cashify.ui.budget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
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
import com.example.cashify.data.local.BudgetWithSpent;
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

    public List<BudgetWithSpent> getBudgets() { return budgetList; }

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
        Context context = holder.itemView.getContext();

        double spent = item.spentAmount;
        double limit = item.limitAmount;

        // KIỂM TRA KHOẢN CHI NGOÀI KẾ HOẠCH (Hạn mức bằng 0)
        boolean isUnplanned = (limit <= 0);

        int percent = limit > 0 ? (int) ((spent / limit) * 100) : 0;
        double remaining = limit - spent;

        String nameToShow = (item.categoryName != null) ? item.categoryName : ("Danh mục " + item.categoryId);
        holder.tvCategoryName.setText(nameToShow);

         //=== ĐOẠN CODE LÔI ICON TỪ DATABASE LÊN ĐÂY KHI CÓ ICON===
        String iconName = item.categoryIcon;
        if (iconName != null && !iconName.isEmpty()) {
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    iconName, "drawable", holder.itemView.getContext().getPackageName());
            holder.ivCategoryIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_food);
        }

// Thêm màu từ category
        try {
            int originColor = Color.parseColor(item.categoryColor != null ? item.categoryColor : "#000000");
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
            holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
            holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_background_red)));
        }


        // =========================================================
        // === 1. ĐEM LÒ ĐÚC TIỀN VÀO SỬ DỤNG Ở ĐÂY ===
        // =========================================================
        String shortSpent = CurrencyFormatter.formatCompactVND(spent);

        // XỬ LÝ HIỂN THỊ RIÊNG CHO NGOÀI KẾ HOẠCH
        if (isUnplanned) {
            holder.tvSpentAndLimit.setText(shortSpent + " (Unplanned)");
            holder.tvPercent.setText(""); // Không hiện % khi chưa có hạn mức
            holder.pbBudget.setMax(100);
            ObjectAnimator.ofInt(holder.pbBudget, "progress", 0, 100)
                    .setDuration(1000)
                    .start();
            holder.pbBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD"))); // Màu xám (Grey 400)

            holder.tvAlertMessage.setVisibility(View.VISIBLE);
            holder.tvAlertMessage.setText("Tap (+) to set a limit");
            holder.tvAlertMessage.setTextColor(Color.parseColor("#757575"));

            holder.ivEdit.setImageResource(android.R.drawable.ic_input_add); // Hiện dấu (+)
        } else {
            // Giao diện bình thường (Đã lên kế hoạch)
            String shortLimit = CurrencyFormatter.formatCompactVND(limit);
            holder.tvSpentAndLimit.setText(shortSpent + " / " + shortLimit);
            holder.tvPercent.setText(percent + "%");
            holder.pbBudget.setMax(100);
            int targetProgress = Math.min(percent, 100);
            ObjectAnimator.ofInt(holder.pbBudget, "progress", 0, targetProgress)
                    .setDuration(1000)
                    .start();
            holder.ivEdit.setImageResource(android.R.drawable.ic_menu_edit); // Hiện cây bút chì

            // LOGIC ĐỔI MÀU & HIỆN CẢNH BÁO
            holder.tvAlertMessage.setVisibility(View.VISIBLE);
            String formattedRemaining = CurrencyFormatter.formatCompactVND(Math.abs(remaining));

            if (percent > 100) {
                // Mức độ cao hơn (pastel-coral): Ahh c'mon man...
                int colorCoral = ContextCompat.getColor(context, R.color.cat_pastel_coral);
                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(colorCoral));
                holder.tvPercent.setTextColor(colorCoral);
                holder.tvAlertMessage.setText("Ahh c'mon man...");
                holder.tvAlertMessage.setTextColor(colorCoral);

            } else if (percent >= 80) {
                // Mức độ cảnh cáo (pastel-coral): Just <...> VNĐ left!
                int colorCoral = ContextCompat.getColor(context, R.color.cat_pastel_coral);
                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(colorCoral));
                holder.tvPercent.setTextColor(colorCoral);
                holder.tvAlertMessage.setText("Just " + formattedRemaining + " left!");
                holder.tvAlertMessage.setTextColor(colorCoral);

            } else if (percent >= 60) {
                // Mức độ trung bình (pastel-orange): <...> VNĐ available
                int colorOrange = ContextCompat.getColor(context, R.color.cat_pastel_orange);
                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(colorOrange));
                holder.tvPercent.setTextColor(colorOrange);
                holder.tvAlertMessage.setText(formattedRemaining + " available");
                holder.tvAlertMessage.setTextColor(colorOrange);

            } else {
                // Mức độ nhẹ (pastel-green): <...> VNĐ left to spend
                int colorGreen = ContextCompat.getColor(context, R.color.cat_pastel_green);
                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(colorGreen));
                holder.tvPercent.setTextColor(ContextCompat.getColor(context, R.color.black)); // Chữ % để màu đen cho dễ đọc
                holder.tvAlertMessage.setText(formattedRemaining + " left to spend");
                holder.tvAlertMessage.setTextColor(colorGreen);
            }
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