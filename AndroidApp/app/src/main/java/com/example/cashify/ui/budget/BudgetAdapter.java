package com.example.cashify.ui.budget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.local.BudgetWithSpent;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<BudgetWithSpent> budgetList = new ArrayList<>();
    private final OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onBudgetClick(BudgetWithSpent item);
    }

    public BudgetAdapter(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    public List<BudgetWithSpent> getBudgets() {
        return budgetList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setBudgets(List<BudgetWithSpent> budgets) {
        this.budgetList = budgets != null ? budgets : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetWithSpent item = budgetList.get(position);
        Context context = holder.itemView.getContext();

        double spent = sanitizeMoney(item.spentAmount);
        double limit = sanitizeMoney(item.limitAmount);

        // Check if this is an unplanned expense (Limit is 0)
        boolean isUnplanned = (limit <= 0);

        int percent = calculateBudgetPercent(spent, limit);
        int targetProgress = clampProgress(percent);
        double remaining = limit - spent;

        String nameToShow = (item.categoryName != null) ? item.categoryName : ("Category " + item.categoryId);
        holder.tvCategoryName.setText(nameToShow);

        // =========================================================
        // CATEGORY ICON & COLOR CONFIGURATION
        // =========================================================
        String iconName = item.categoryIcon;
        if (iconName != null && !iconName.isEmpty()) {
            int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
            holder.ivCategoryIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_food);
        }

        try {
            int originColor = Color.parseColor(item.categoryColor != null ? item.categoryColor : "#000000");
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
            holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_background_red)));
            holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        }

        // =========================================================
        // BUDGET PROGRESS & ALERT LOGIC
        // =========================================================
        String shortSpent = CurrencyFormatter.formatCompactVND(spent);

        if (isUnplanned) {
            // Unplanned Expense UI State
            holder.tvSpentAndLimit.setText(shortSpent + " (Unplanned)");
            holder.tvPercent.setText("");
            holder.tvPercent.setTextColor(ContextCompat.getColor(context, R.color.black));

            holder.pbBudget.setMax(100);
            holder.pbBudget.clearAnimation();
            holder.pbBudget.setProgress(0);
            holder.pbBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD"))); // Grey 400

            holder.tvAlertMessage.setVisibility(View.VISIBLE);
            holder.tvAlertMessage.setText("Tap (+) to set a limit");
            holder.tvAlertMessage.setTextColor(Color.parseColor("#757575"));

            holder.ivEdit.setImageResource(android.R.drawable.ic_input_add);
        } else {
            // Planned Budget UI State
            String shortLimit = CurrencyFormatter.formatCompactVND(limit);
            holder.tvSpentAndLimit.setText(shortSpent + " / " + shortLimit);
            holder.tvPercent.setText(percent + "%");

            holder.pbBudget.setMax(100);
            holder.pbBudget.clearAnimation();
            holder.pbBudget.setProgress(targetProgress);
            holder.ivEdit.setImageResource(android.R.drawable.ic_menu_edit);

            // Dynamic Alert Coloring based on consumption
            holder.tvAlertMessage.setVisibility(View.VISIBLE);
            String formattedRemaining = CurrencyFormatter.formatCompactVND(Math.abs(remaining));

            if (percent > 100) {
                // Over Budget
                int colorCoral = ContextCompat.getColor(context, R.color.cat_pastel_coral);
                applyProgressTint(holder, colorCoral, colorCoral, "Ahh c'mon man...");
            } else if (percent >= 80) {
                // High Warning
                int colorCoral = ContextCompat.getColor(context, R.color.cat_pastel_coral);
                applyProgressTint(holder, colorCoral, colorCoral, "Just " + formattedRemaining + " left!");
            } else if (percent >= 60) {
                // Moderate Warning
                int colorOrange = ContextCompat.getColor(context, R.color.cat_pastel_orange);
                applyProgressTint(holder, colorOrange, colorOrange, formattedRemaining + " available");
            } else {
                // Safe Zone
                int colorGreen = ContextCompat.getColor(context, R.color.status_green);
                applyProgressTint(holder, colorGreen, ContextCompat.getColor(context, R.color.black), formattedRemaining + " left to spend");
            }
        }

        // Trigger listener for Numpad/Edit actions
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBudgetClick(item);
        });
    }

    /**
     * Helper method to apply colors and text to the progress and alert views.
     */
    private void applyProgressTint(BudgetViewHolder holder, int barColor, int textColor, String message) {
        holder.pbBudget.setProgressTintList(ColorStateList.valueOf(barColor));
        holder.tvPercent.setTextColor(textColor);
        holder.tvAlertMessage.setText(message);
        holder.tvAlertMessage.setTextColor(barColor);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    private double sanitizeMoney(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0 : Math.max(0, value);
    }

    private int calculateBudgetPercent(double spent, double limit) {
        if (spent <= 0 || limit <= 0) return 0;
        double percent = (spent * 100.0d) / limit;
        if (Double.isNaN(percent) || Double.isInfinite(percent) || percent <= 0) return 0;
        if (percent >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) percent;
    }

    private int clampProgress(int percent) {
        return Math.max(0, Math.min(percent, 100));
    }

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