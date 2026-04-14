package com.example.cashify.ui.home;

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
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.viewmodel.TransactionViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.TransactionViewHolder> {

    // Nhận dữ liệu xịn từ ViewModel
    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();
    private OnItemClickListener clickListener;

    // Interface y hệt HistoryAdapter nhưng dùng cho Click thường ở màn Home
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // Hàm update data tự động lọc bỏ header ngày tháng
    public void updateData(List<TransactionViewModel.HistoryItem> newData) {
        if (newData != null) {
            List<TransactionViewModel.HistoryItem> onlyTransactions = new ArrayList<>();
            for (TransactionViewModel.HistoryItem item : newData) {
                // Chỉ nhặt những item là giao dịch để hiển thị trên Home
                if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                    onlyTransactions.add(item);
                }
            }
            this.items = onlyTransactions;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);
        Transaction trans = item.getTransaction();

        if (trans == null) return;

        // 1. Gán Note (Tiêu đề)
        if (holder.tvMainTitle != null) {
            holder.tvMainTitle.setText(trans.note != null && !trans.note.isEmpty() ? trans.note : "Giao dịch");
        }

        // 2. Gán Subtitle (Category • Time) - Học y chang HistoryAdapter
        if (holder.tvSubtitle != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.ENGLISH);
            String time = sdf.format(new Date(trans.timestamp));
            holder.tvSubtitle.setText(item.getCategoryName() + " • " + time);
        }

        // 3. Gán Số tiền và Màu sắc - Mix giữa màu của History và Formatter của Cashify
        if (holder.tvAmount != null) {
            String formattedAmount = CurrencyFormatter.formatCompactVND(trans.amount);
            if (trans.type == 1) { // Income
                holder.tvAmount.setText("+" + formattedAmount);
                holder.tvAmount.setTextColor(Color.parseColor("#1DB424"));
            } else { // Expense
                holder.tvAmount.setText("-" + formattedAmount);
                holder.tvAmount.setTextColor(Color.parseColor("#D14040"));
            }
        }

        // 4. Gán Icon và Màu nền Icon - Học chuẩn từ HistoryAdapter
        if (holder.ivCategoryIcon != null) {
            int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                    item.getCategoryIcon(), "drawable", holder.itemView.getContext().getPackageName());
            holder.ivCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

            try {
                String colorStr = item.getCategoryColor();
                if (colorStr == null || colorStr.isEmpty()) colorStr = "#4CAF50"; // Màu cua dự phòng (Xanh lá)
                int originColor = Color.parseColor(colorStr);

                int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
                holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));

                holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));

            } catch (Exception e) {
                holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE"))); // Xám siêu nhẹ
                holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY)); // Icon xám
            }
        }

        // 5. Sự kiện Click
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