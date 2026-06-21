package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ImageHelper;
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

    // [Từ master] Tối ưu hoá memory: Không khởi tạo lại Formatter khi scroll
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

    private final Context context;
    private final String workspaceId;
    private final String currentUserId;
    private String ownerId;
    private List<TransactionViewModel.HistoryItem> items = new ArrayList<>();

    // [Từ UI-Consistency] Cache profile để hiển thị Avatar thay vì chỉ String Name như master
    private final Map<String, CreatorProfile> creatorProfileCache = new HashMap<>();
    private final Map<String, Category> categoryCache = new HashMap<>();
    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onClick(Transaction transaction);
    }

    public WorkspaceTransactionAdapter(Context context, String workspaceId, String currentUserId, String ownerId,
                                       List<TransactionViewModel.HistoryItem> list, OnTransactionClickListener listener) {
        this.context = context;
        this.workspaceId = workspaceId;
        this.currentUserId = currentUserId;
        this.ownerId = ownerId;
        this.items = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        notifyDataSetChanged(); // Forces a re-render to update permissions immediately
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setHistoryData(List<TransactionViewModel.HistoryItem> newList) {
        this.items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
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
        }
        return new TransactionViewHolder(inflater.inflate(R.layout.item_workspace_transaction, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionViewModel.HistoryItem item = items.get(position);

        if (holder instanceof DateViewHolder) {
            DateViewHolder dateHolder = (DateViewHolder) holder;
            if (dateHolder.tvDate != null) dateHolder.tvDate.setText(item.getDate());
            return;
        }

        TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
        Transaction transaction = item.getTransaction();
        if (transaction == null) return;

        // [Từ master] Tagging mechanism để tránh lỗi update nhầm view khi scroll nhanh
        String bindKey = transaction.id != null ? transaction.id : String.valueOf(position);
        transactionHolder.itemView.setTag(bindKey);

        String timeText = TIME_FORMATTER.format(new Date(transaction.timestamp));
        
        // [Từ UI-Consistency] 1 element 2 cách trình bày -> Ưu tiên Text thay vì Emoji của master
        String paymentMethod = transaction.paymentMethod == null || transaction.paymentMethod.trim().isEmpty()
                ? "Cash"
                : transaction.paymentMethod.trim();

        bindAmount(transactionHolder, transaction);
        bindCreator(transactionHolder, transaction, bindKey);
        bindCategory(transactionHolder, transaction, bindKey, timeText, paymentMethod);
        bindPermissions(transactionHolder.itemView, transaction);
    }

    private void bindAmount(TransactionViewHolder holder, Transaction transaction) {
        // [Từ UI-Consistency] Format compact amount (rút gọn) thay vì full số như master
        if (transaction.type == 1) {
            holder.tvAmount.setText("+" + CurrencyFormatter.formatCompactAmount(transaction.amount));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_green));
            holder.tvCreatorName.setText("Income");
        } else {
            holder.tvAmount.setText(CurrencyFormatter.formatCompactAmount(-transaction.amount));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.status_red));
            holder.tvCreatorName.setText("Expense");
        }
    }

    private void bindCreator(TransactionViewHolder holder, Transaction transaction, String bindKey) {
        String signedAmount = transaction.type == 1
                ? "+" + CurrencyFormatter.formatCompactAmount(transaction.amount)
                : CurrencyFormatter.formatCompactAmount(-transaction.amount);

        if (transaction.userId == null || transaction.userId.trim().isEmpty()) {
            bindCreatorProfile(holder, transaction, new CreatorProfile("Unknown Member", null), signedAmount);
            return;
        }

        CreatorProfile cachedProfile = creatorProfileCache.get(transaction.userId);
        if (cachedProfile != null) {
            bindCreatorProfile(holder, transaction, cachedProfile, signedAmount);
            return;
        }

        bindCreatorProfile(holder, transaction, CreatorProfile.loading(), signedAmount);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(transaction.userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!bindKey.equals(holder.itemView.getTag())) return;
                    String name = firstNonEmpty(doc.getString("displayName"), doc.getString("email"), "Unknown Member");
                    String avatarUrl = doc.getString("avatarUrl");
                    CreatorProfile profile = new CreatorProfile(name, avatarUrl);
                    creatorProfileCache.put(transaction.userId, profile);
                    bindCreatorProfile(holder, transaction, profile, signedAmount);
                })
                .addOnFailureListener(e -> {
                    if (!bindKey.equals(holder.itemView.getTag())) return;
                    bindCreatorProfile(holder, transaction, new CreatorProfile("Unknown Member", null), signedAmount);
                });
    }

    private void bindCreatorProfile(TransactionViewHolder holder, Transaction transaction, CreatorProfile profile, String signedAmount) {
        String action = transaction.type == 1 ? "added" : "spent";
        // [Từ UI-Consistency] Hiển thị nguyên câu thay vì chỉ "By: Name"
        holder.tvMainTitle.setText(profile.name + " " + action + " " + signedAmount);
        ImageHelper.loadAvatar(profile.avatarUrl, holder.ivCreatorAvatar, profile.name);
    }

    private void bindCategory(TransactionViewHolder holder, Transaction transaction, String bindKey, String timeText, String paymentMethod) {
        if (transaction.firestoreCategoryId == null || workspaceId == null) {
            updateCategoryUI(holder, transaction, null, timeText, paymentMethod);
            return;
        }

        Category cachedCategory = categoryCache.get(transaction.firestoreCategoryId);
        if (cachedCategory != null) {
            updateCategoryUI(holder, transaction, cachedCategory, timeText, paymentMethod);
            return;
        }

        updateCategoryUI(holder, transaction, null, timeText, paymentMethod);
        FirebaseFirestore.getInstance()
                .collection("workspaces")
                .document(workspaceId)
                .collection("categories")
                .document(transaction.firestoreCategoryId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!bindKey.equals(holder.itemView.getTag())) return;
                    Category category = doc.toObject(Category.class);
                    if (category == null) return;
                    categoryCache.put(transaction.firestoreCategoryId, category);
                    updateCategoryUI(holder, transaction, category, timeText, paymentMethod);
                });
    }

    private void updateCategoryUI(TransactionViewHolder holder, Transaction transaction, Category category, String timeText, String paymentMethod) {
        String categoryName = category != null ? category.name : "Unknown";
        String note = transaction.note != null && !transaction.note.trim().isEmpty()
                ? transaction.note.trim()
                : categoryName;
                
        // [Từ UI-Consistency] Giữ nguyên format gạch ngang " - "
        holder.tvSubtitle.setText(String.format(Locale.getDefault(), "%s - %s - %s", categoryName, timeText, paymentMethod));
        holder.tvSubtitle.setContentDescription(note);

        if (category == null) {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_other);
            fallbackIconColor(holder, transaction.type);
            return;
        }

        String iconName = category.iconName != null ? category.iconName : "";
        int iconResId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        holder.ivCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_food);

        try {
            int originColor = Color.parseColor(category.colorCode != null ? category.colorCode : "#000000");
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
            holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(pastelColor));
            holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception ignored) {
            fallbackIconColor(holder, transaction.type);
        }
    }

    private void fallbackIconColor(TransactionViewHolder holder, int type) {
        int colorRes = type == 1 ? R.color.status_background_green : R.color.status_background_red;
        holder.ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));
        holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
    }

    private void bindPermissions(View itemView, Transaction transaction) {
        boolean canEdit = (currentUserId != null && currentUserId.equals(transaction.userId))
                || (currentUserId != null && currentUserId.equals(ownerId));

        itemView.setAlpha(canEdit ? 1.0f : 0.6f);
        itemView.setOnClickListener(v -> {
            if (canEdit) {
                if (listener != null) listener.onClick(transaction);
            } else {
                Toast.makeText(context, "Access denied. Only the creator or owner can modify this transaction.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCreatorAvatar;
        ShapeableImageView ivCategoryIcon;
        TextView tvMainTitle;
        TextView tvSubtitle;
        TextView tvCreatorName;
        TextView tvAmount;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCreatorAvatar = itemView.findViewById(R.id.ivCreatorAvatar);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvCreatorName = itemView.findViewById(R.id.tvCreatorName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }

    private static class CreatorProfile {
        final String name;
        final String avatarUrl;

        CreatorProfile(String name, String avatarUrl) {
            this.name = name == null || name.trim().isEmpty() ? "Unknown Member" : name.trim();
            this.avatarUrl = avatarUrl;
        }

        static CreatorProfile loading() {
            return new CreatorProfile("Loading member", null);
        }
    }
}