package com.example.cashify.ui.transactions;

import android.content.Intent;
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

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();
    private OnTransactionClickListener listener;

    // Interface dùng chung cho cả Click thường và Inline Edit
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
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
            // Xử lý Header ngày tháng
            DateViewHolder dHolder = (DateViewHolder) holder;
            if (dHolder.tvDate != null) {
                dHolder.tvDate.setText(item.getDate());
            }
        }
        else if (holder instanceof TransactionViewHolder) {
            TransactionViewHolder tHolder = (TransactionViewHolder) holder;
            Transaction trans = item.getTransaction();

            // Safety check: Nếu item rỗng thì nghỉ khỏe
            if (trans == null) return;

            // 1. Tiêu đề (Note hoặc Tên danh mục)
            if (tHolder.tvMainTitle != null) {
                String title = (trans.note != null && !trans.note.isEmpty()) ? trans.note : item.getCategoryName();
                tHolder.tvMainTitle.setText(title);
            }

            // 2. Chú thích (Category + Thời gian)
            if (tHolder.tvCategory != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                String timeStr = sdf.format(new Date(trans.timestamp));
                // Lấy icon tương ứng với phương thức thanh toán
                String paymentIcon;
                if (trans.paymentMethod == null) {
                    paymentIcon = "💵";
                } else {
                    switch (trans.paymentMethod) {
                        case "Card": paymentIcon = "💳"; break;
                        case "Bank": paymentIcon = "🏦"; break;
                        default:     paymentIcon = "💵"; break;
                    }
                }
                tHolder.tvCategory.setText(
                        String.format("%s • %s • %s", item.getCategoryName(), timeStr, paymentIcon)
                );
            }


            // 3. Số tiền và Màu sắc (Xanh cho Thu, Đỏ cho Chi)
            if (tHolder.tvAmount != null) {
                boolean isIncome = trans.type == 1;
                String sign = isIncome ? "+" : "-";
                int color = ContextCompat.getColor(holder.itemView.getContext(),
                        isIncome ? R.color.status_green : R.color.status_red);

                tHolder.tvAmount.setText(sign + CurrencyFormatter.formatFullVND((double) trans.amount));
                tHolder.tvAmount.setTextColor(color);
            }

            // 4. Icon danh mục
            if (tHolder.ivIcon != null) {
                int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                        item.getCategoryIcon(), "drawable", holder.itemView.getContext().getPackageName());
                tHolder.ivIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

                try {
                    int originColor = Color.parseColor(item.getCategoryColor()); // ← dùng màu từ category
                    int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
                    tHolder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
                    tHolder.ivIcon.setImageTintList(ColorStateList.valueOf(originColor));
                } catch (Exception e) {
                    // Fallback nếu màu lỗi
                    int colorRes = (trans.type == 1) ? R.color.status_background_green : R.color.status_background_red;
                    tHolder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), colorRes)));
                }
            }

            // 5. [QUAN TRỌNG] Sự kiện Click mở màn hình Edit
            // Gán vào toàn bộ itemView để dù bấm trúng text hay icon đều mở được
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(trans);
                }
            });
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