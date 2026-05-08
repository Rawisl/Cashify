package com.example.cashify.ui.workspace;

import android.content.Context;
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
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.CurrencyFormatter;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkspaceTransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final String workspaceId;

    // Đổi kiểu List thành HistoryItem để chứa được cả Header Ngày
    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();

    // Bộ nhớ đệm (Cache) tốc độ cao
    private final Map<String, String> userNameCache = new HashMap<>();
    private final Map<String, Category> categoryCache = new HashMap<>();

    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onClick(Transaction transaction);
    }

    public WorkspaceTransactionAdapter(Context context, String workspaceId, List<TransactionViewModel.HistoryItem> list, OnTransactionClickListener listener) {
        this.context = context;
        this.workspaceId = workspaceId;
        if(list != null) this.items = list;
        this.listener = listener;
    }

    // Hàm set data mới từ ViewModel đổ xuống
    public void setHistoryData(List<TransactionViewModel.HistoryItem> newList) {
        if (newList != null) {
            this.items = newList;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TransactionViewModel.HistoryItem.TYPE_DATE_HEADER) {
            return new DateViewHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        } else {
            return new TransactionViewHolder(inflater.inflate(R.layout.item_workspace_transaction, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);

        if (holder instanceof DateViewHolder) {
            // =====================================
            // XỬ LÝ GIAO DIỆN HEADER (NGÀY THÁNG)
            // =====================================
            DateViewHolder dHolder = (DateViewHolder) holder;
            if (dHolder.tvDate != null) {
                dHolder.tvDate.setText(item.getDate());
            }
        } else if (holder instanceof TransactionViewHolder) {
            // =====================================
            // XỬ LÝ GIAO DIỆN GIAO DỊCH (TRANSACTION)
            // =====================================
            TransactionViewHolder tHolder = (TransactionViewHolder) holder;
            Transaction t = item.getTransaction();

            if (t == null) return;

            // 1. FORMAT THỜI GIAN VÀ EMOJI THANH TOÁN
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            String timeStr = sdf.format(new Date(t.timestamp));
            String paymentIcon;
            if (t.paymentMethod == null) {
                paymentIcon = "💵";
            } else {
                switch (t.paymentMethod) {
                    case "Card": paymentIcon = "💳"; break;
                    case "Bank": paymentIcon = "🏦"; break;
                    default:     paymentIcon = "💵"; break;
                }
            }

            // 2. XỬ LÝ SỐ TIỀN & MÀU CHỮ
            String formattedAmount = CurrencyFormatter.formatDoubleToVND((double) t.amount);
            if (t.type == 0) {
                tHolder.tvAmount.setText("-" + formattedAmount);
                tHolder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_red));
            } else {
                tHolder.tvAmount.setText("+" + formattedAmount);
                tHolder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_green));
            }

            // 3. LOAD TÊN NGƯỜI TẠO (TRÁNH LAG BẰNG CACHE)
            if (t.userId != null) {
                if (userNameCache.containsKey(t.userId)) {
                    tHolder.tvCreatorName.setText("By: " + userNameCache.get(t.userId));
                } else {
                    tHolder.tvCreatorName.setText("Loading...");
                    FirebaseFirestore.getInstance().collection("users").document(t.userId).get()
                            .addOnSuccessListener(doc -> {
                                String name = doc.getString("displayName");
                                if (name == null || name.isEmpty()) name = "Unknown Member";
                                userNameCache.put(t.userId, name);
                                tHolder.tvCreatorName.setText("By: " + name);
                            });
                }
            } else {
                tHolder.tvCreatorName.setText("By: Unknown");
            }

            // 4. LOAD CATEGORY (ICON, TÊN, MÀU SẮC)
            if (t.firestoreCategoryId != null && workspaceId != null) {
                if (categoryCache.containsKey(t.firestoreCategoryId)) {
                    updateCategoryUI(tHolder, t, categoryCache.get(t.firestoreCategoryId), timeStr, paymentIcon);
                } else {
                    FirebaseFirestore.getInstance()
                            .collection("workspaces").document(workspaceId)
                            .collection("categories").document(t.firestoreCategoryId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                Category cat = doc.toObject(Category.class);
                                if (cat != null) {
                                    categoryCache.put(t.firestoreCategoryId, cat);
                                    updateCategoryUI(tHolder, t, cat, timeStr, paymentIcon);
                                }
                            });
                }
            } else {
                updateCategoryUI(tHolder, t, null, timeStr, paymentIcon);
            }

            // 5. BẮT SỰ KIỆN CLICK SỬA/XÓA
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(t);
            });
        }
    }

    // Hàm phụ trợ cập nhật giao diện Category
    private void updateCategoryUI(TransactionViewHolder holder, Transaction t, Category cat, String timeStr, String paymentIcon) {
        String catName = (cat != null) ? cat.name : "Unknown";

        String title = (t.note != null && !t.note.isEmpty()) ? t.note : catName;
        holder.tvMainTitle.setText(title);

        holder.tvSubtitle.setText(String.format("%s • %s • %s", catName, timeStr, paymentIcon));

        if (cat != null) {
            int iconResId = context.getResources().getIdentifier(cat.iconName, "drawable", context.getPackageName());
            holder.ivCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

            try {
                int originColor = Color.parseColor(cat.colorCode);
                int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
                holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
                holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));
            } catch (Exception e) {
                fallbackIconColor(holder, t.type);
            }
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_other);
            fallbackIconColor(holder, t.type);
        }
    }

    private void fallbackIconColor(TransactionViewHolder holder, int type) {
        int colorRes = (type == 1) ? R.color.status_background_green : R.color.status_background_red;
        holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));
        holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ==========================================
    // CÁC LỚP VIEWHOLDER CHO HEADER VÀ ITEM
    // ==========================================
    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivCategoryIcon;
        TextView tvMainTitle, tvSubtitle, tvCreatorName, tvAmount;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvCreatorName = itemView.findViewById(R.id.tvCreatorName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}