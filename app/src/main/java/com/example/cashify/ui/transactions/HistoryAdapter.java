package com.example.cashify.ui.transactions;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Transaction;
import com.example.cashify.viewmodel.TransactionViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Transaction transaction);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setHistoryData(List<TransactionViewModel.HistoryItem> newData) {
        if (newData != null) {
            this.items = newData;
            notifyDataSetChanged();
        }
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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);

        if (holder instanceof DateViewHolder) {
            DateViewHolder dHolder = (DateViewHolder) holder;
            if (dHolder.tvDate != null) {
                dHolder.tvDate.setText(item.getDate());
            }
        }
        else if (holder instanceof TransactionViewHolder) {
            TransactionViewHolder tHolder = (TransactionViewHolder) holder;
            Transaction trans = item.getTransaction();
            if (trans == null) return;

            // 1. Gán Note (Tiêu đề)
            if (tHolder.tvTitle != null) {
                tHolder.tvTitle.setText(trans.note != null ? trans.note : "Giao dịch");
            }

            // 2. Gán Subtitle (Category • Time)
            if (tHolder.tvCategory != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.ENGLISH);
                String time = sdf.format(new Date(trans.timestamp));
                tHolder.tvCategory.setText(item.getCategoryName() + " • " + time);
            }

            // 3. Gán Số tiền và Màu sắc
            if (tHolder.tvAmount != null) {
                double amount = trans.amount;
                if (trans.type == 1) { // Income
                    tHolder.tvAmount.setText("+$" + String.format(Locale.US, "%,.2f", amount));
                    tHolder.tvAmount.setTextColor(Color.parseColor("#1DB424"));
                } else { // Expense
                    tHolder.tvAmount.setText("-$" + String.format(Locale.US, "%,.2f", amount));
                    tHolder.tvAmount.setTextColor(Color.parseColor("#D14040"));
                }
            }

            // 4. Gán Icon và Màu nền Icon
            if (tHolder.ivIcon != null) {
                int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                        item.getCategoryIcon(), "drawable", holder.itemView.getContext().getPackageName());
                tHolder.ivIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

                int colorRes = (trans.type == 1) ? R.color.status_background_green : R.color.status_background_red;
                tHolder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), colorRes)));
            }

            // 5. Sự kiện nhấn giữ (Sửa lại đoạn này)
            if (tHolder.itemContainer != null) {
                tHolder.itemContainer.setLongClickable(true);
                tHolder.itemContainer.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onItemLongClick(trans);
                        return true; // Quan trọng: Trả về true để không kích hoạt click thường
                    }
                    return false;
                });
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvAmount;
        ShapeableImageView ivIcon;
        LinearLayout itemContainer; // 1. Thêm dòng này

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvSubtitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            // 2. Ánh xạ cái container này
            itemContainer = itemView.findViewById(R.id.itemContainer);
        }
    }
}