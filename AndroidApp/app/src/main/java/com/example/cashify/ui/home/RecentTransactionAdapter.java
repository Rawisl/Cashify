package com.example.cashify.ui.home;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.TransactionViewHolder> {

    // Pre-allocate SimpleDateFormat to prevent massive object creation during onBindViewHolder (60fps)
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM • hh:mm a", Locale.ENGLISH);

    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Updates the adapter data while automatically filtering out Date Header items.
     * Ensures only actual transactions are displayed on the Home screen.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<TransactionViewModel.HistoryItem> newData) {
        List<TransactionViewModel.HistoryItem> onlyTransactions = new ArrayList<>();
        if (newData != null) {
            for (TransactionViewModel.HistoryItem item : newData) {
                if (item != null && item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                    onlyTransactions.add(item);
                }
            }
        }
        this.items = onlyTransactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);
        Transaction trans = item.getTransaction();

        if (trans == null) return;

        // 1. Set Title (Prioritize user note, fallback to category name)
        if (holder.tvMainTitle != null) {
            String title = (trans.note != null && !trans.note.isEmpty())
                    ? trans.note
                    : item.getCategoryName();
            holder.tvMainTitle.setText(title);
        }

        // 2. Set Subtitle (Timestamp formatting)
        if (holder.tvSubtitle != null) {
            holder.tvSubtitle.setText(DATE_FORMATTER.format(new Date(trans.timestamp)));
        }

        // 3. Set Amount & Text Color based on transaction type
        if (holder.tvAmount != null) {
            if (trans.type == 1) { // Income
                holder.tvAmount.setText("+" + CurrencyFormatter.formatCompactAmount(trans.amount));
                holder.tvAmount.setTextColor(Color.parseColor("#1DB424"));
            } else { // Expense
                holder.tvAmount.setText(CurrencyFormatter.formatCompactAmount(-trans.amount));
                holder.tvAmount.setTextColor(Color.parseColor("#D14040"));
            }
        }

        // 4. Set Category Icon & Dynamic Pastel Background
        if (holder.ivCategoryIcon != null) {
            String iconName = item.getCategoryIcon();
            int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                    iconName != null ? iconName : "", "drawable", holder.itemView.getContext().getPackageName());
            holder.ivCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

            try {
                String colorStr = item.getCategoryColor();
                if (colorStr == null || colorStr.isEmpty()) colorStr = "#4CAF50"; // Fallback Green
                int originColor = Color.parseColor(colorStr);

                // Create a 20% opacity (alpha 51) pastel variant for the background
                int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

                holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
                holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));

            } catch (Exception e) {
                // Fallback to neutral gray on parsing failure
                holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
                holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
            }
        }

        // 5. Click Event Delegation
        if (holder.itemContainer != null) {
            holder.itemContainer.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(trans);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemContainer;
        ShapeableImageView ivCategoryIcon;
        TextView tvMainTitle, tvSubtitle, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}