package com.example.cashify.ui.transactions;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.utils.CurrencyFormatter;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Pre-allocate formatter to prevent memory thrashing during fast scrolls
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();
    private OnTransactionClickListener listener;

    // Interface for tap actions (e.g., opening Inline Edit or Details)
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setHistoryData(List<TransactionViewModel.HistoryItem> newData) {
        this.items = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    public TransactionViewModel.HistoryItem getItemAt(int position) {
        return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TransactionViewModel.HistoryItem.TYPE_DATE_HEADER) {
            return new DateViewHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        } else {
            return new TransactionViewHolder(inflater.inflate(R.layout.item_transaction, parent, false));
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);

        if (holder instanceof DateViewHolder) {
            // Process Date Header
            DateViewHolder dHolder = (DateViewHolder) holder;
            if (dHolder.tvDate != null) {
                dHolder.tvDate.setText(item.getDate());
            }
        }
        else if (holder instanceof TransactionViewHolder) {
            // Process Transaction Item
            TransactionViewHolder tHolder = (TransactionViewHolder) holder;
            Transaction trans = item.getTransaction();

            // Guard clause against null records
            if (trans == null) return;

            // 1. Title (Prioritize user note, fallback to category name)
            if (tHolder.tvMainTitle != null) {
                String title = (trans.note != null && !trans.note.isEmpty()) ? trans.note : item.getCategoryName();
                tHolder.tvMainTitle.setText(title);
            }

            // 2. Subtitle (Category • Time • Payment Method)
            if (tHolder.tvCategory != null) {
                String timeStr = TIME_FORMATTER.format(new Date(trans.timestamp));

                String paymentIcon;
                if (trans.paymentMethod == null) {
                    paymentIcon = holder.itemView.getContext().getString(R.string.cash);
                } else {
                    switch (trans.paymentMethod) {
                        case "Card": paymentIcon = holder.itemView.getContext().getString(R.string.card); break;
                        case "Bank": paymentIcon = holder.itemView.getContext().getString(R.string.bank); break;
                        default:     paymentIcon = holder.itemView.getContext().getString(R.string.cash); break;
                    }
                }

                tHolder.tvCategory.setText(
                        String.format("%s • %s • %s", item.getCategoryName(), timeStr, paymentIcon)
                );
            }

            // 3. Amount & Color (Green for Income, Red for Expense)
            if (tHolder.tvAmount != null) {
                boolean isIncome = trans.type == 1;
                int color = ContextCompat.getColor(holder.itemView.getContext(),
                        isIncome ? R.color.status_green : R.color.status_red);

                double signedAmount = isIncome ? trans.amount : -trans.amount;
                String formattedAmount = CurrencyFormatter.formatFullAmount(signedAmount);

                tHolder.tvAmount.setText(isIncome ? "+" + formattedAmount : formattedAmount);
                tHolder.tvAmount.setTextColor(color);
            }

            // 4. Category Icon & Pastel Background
            if (tHolder.ivIcon != null) {
                int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                        item.getCategoryIcon(), "drawable", holder.itemView.getContext().getPackageName());
                tHolder.ivIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

                try {
                    int originColor = Color.parseColor(item.getCategoryColor());
                    int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

                    tHolder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
                    tHolder.ivIcon.setImageTintList(ColorStateList.valueOf(originColor));
                } catch (Exception e) {
                    // Fallback colors if parsing fails
                    int colorRes = (trans.type == 1) ? R.color.status_background_green : R.color.status_background_red;
                    tHolder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), colorRes)));
                }
            }

            // 5. Delegate click events to open Details/Edit screen
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(trans);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // VIEW HOLDERS
    // =========================================================================

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvMainTitle, tvCategory, tvAmount;
        ShapeableImageView ivIcon;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvCategory = itemView.findViewById(R.id.tvSubtitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
        }
    }
}