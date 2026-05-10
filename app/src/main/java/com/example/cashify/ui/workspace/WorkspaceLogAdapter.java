package com.example.cashify.ui.workspace;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.LogItem;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.TimeFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkspaceLogAdapter.java
 *
 * - Fetch avatar: gọi users/{userId} → lấy avatarUrl → ImageHelper.loadAvatar(url, imageView)
 * - Fetch tên:    gọi users/{userId} → lấy displayName → ghép SpannableString
 * - DiffUtil:     smooth update, không blink khi có log mới realtime
 * - Timeline line: ẩn cho item cuối cùng
 */
public class WorkspaceLogAdapter extends RecyclerView.Adapter<WorkspaceLogAdapter.LogViewHolder> {

    private final Context           context;
    private final List<LogItem>     logList;
    private final FirebaseFirestore db;

    public WorkspaceLogAdapter(Context context) {
        this.context = context;
        this.logList = new ArrayList<>();
        this.db      = FirebaseFirestore.getInstance();
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────────
    public void submitList(List<LogItem> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return logList.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String a = logList.get(oldPos).getId();
                String b = newList.get(newPos).getId();
                return a != null && a.equals(b);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                LogItem o = logList.get(oldPos);
                LogItem n = newList.get(newPos);
                return o.getTimestamp() == n.getTimestamp()
                        && safeEquals(o.getMessage(), n.getMessage());
            }
        });
        logList.clear();
        logList.addAll(newList);
        result.dispatchUpdatesTo(this);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    // ── Inflate ───────────────────────────────────────────────────────────────
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_workspace_log, parent, false);
        return new LogViewHolder(view);
    }

    // ── Bind ──────────────────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogItem item   = logList.get(position);
        boolean isLast = position == logList.size() - 1;

        // Timeline line
        holder.viewLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

        // Timestamp
        holder.tvTime.setText(TimeFormatter.format(item.getTimestamp()));

        // Placeholder trước khi fetch xong
        holder.tvMessage.setText(buildSpannable("...", item.getMessage()));
        ImageHelper.loadAvatar(null, holder.imgAvatar); // reset về placeholder

        // Fetch user info (displayName + avatarUrl) từ Firestore
        fetchUserInfo(item.getUserId(), (displayName, avatarUrl) -> {
            // Guard: tránh race condition khi scroll nhanh
            if (holder.getBindingAdapterPosition() != position) return;

            // Cập nhật message với tên thật
            holder.tvMessage.setText(buildSpannable(displayName, item.getMessage()));

            // Load avatar bằng ImageHelper của team (nhận Object url)
            ImageHelper.loadAvatar(avatarUrl, holder.imgAvatar);
        });
    }

    @Override
    public int getItemCount() { return logList.size(); }

    // ── Fetch displayName + avatarUrl từ users/{userId} ───────────────────────
    private void fetchUserInfo(String userId, OnUserInfoFetched callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFetched("Người dùng", null);
            return;
        }
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    String name = doc.getString("displayName");
                    String url  = doc.getString("avatarUrl");
                    callback.onFetched(
                            name != null ? name : "Người dùng",
                            url
                    );
                })
                .addOnFailureListener(e -> callback.onFetched("Người dùng", null));
    }

    private interface OnUserInfoFetched {
        void onFetched(String displayName, String avatarUrl);
    }

    // ── SpannableString: [Tên đậm + màu primary] + [message] ─────────────────
    private SpannableStringBuilder buildSpannable(String userName, String message) {
        String full = userName + " " + (message != null ? message : "");
        SpannableStringBuilder ssb = new SpannableStringBuilder(full);
        int end = userName.length();

        ssb.setSpan(new StyleSpan(Typeface.BOLD),
                0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.black)),
                0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class LogViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView  tvMessage;
        TextView  tvTime;
        View      viewLine;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
            viewLine  = itemView.findViewById(R.id.viewTimelineLine);
        }
    }
}