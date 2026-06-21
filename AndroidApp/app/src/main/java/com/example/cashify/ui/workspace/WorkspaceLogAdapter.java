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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkspaceLogAdapter extends RecyclerView.Adapter<WorkspaceLogAdapter.LogViewHolder> {

    private static final String DEFAULT_USERNAME = "Cashify User";

    private final Context context;
    private final List<LogItem> logList;
    private final FirebaseFirestore db;

    // Memory Cache map to prevent redundant Firestore calls for the same user profile during scrolls
    private final Map<String, UserProfileCache> userCache = new HashMap<>();

    public WorkspaceLogAdapter(Context context) {
        this.context = context;
        this.logList = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    // =========================================================================
    // DIFFUTIL OPTIMIZATION
    // =========================================================================
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
        logList.addAll(newList != null ? newList : new ArrayList<>());
        result.dispatchUpdatesTo(this);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workspace_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogItem item = logList.get(position);
        boolean isLast = position == logList.size() - 1;

        // Manage timeline indicator state
        holder.viewLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        holder.tvTime.setText(TimeFormatter.format(item.getTimestamp()));

        String userId = item.getUserId();

        // Check if the user profile is already stored in the local memory cache
        if (userCache.containsKey(userId)) {
            UserProfileCache cachedProfile = userCache.get(userId);
            if (cachedProfile != null) {
                holder.tvMessage.setText(buildSpannable(cachedProfile.displayName, item.getMessage()));
                ImageHelper.loadAvatar(cachedProfile.avatarUrl, holder.imgAvatar, cachedProfile.displayName);
            }
            return;
        }

        // Placeholder fallback while asynchronous database fetch is processing
        holder.tvMessage.setText(buildSpannable("...", item.getMessage()));
        ImageHelper.loadAvatar(null, holder.imgAvatar, DEFAULT_USERNAME);

        // Execute profile fetch (sparks network request only on cache miss)
        fetchUserInfo(userId, (displayName, avatarUrl) -> {
            // Guard clause: Prevent race conditions inside quick-recycled views
            if (holder.getBindingAdapterPosition() != position) return;

            holder.tvMessage.setText(buildSpannable(displayName, item.getMessage()));
            ImageHelper.loadAvatar(avatarUrl, holder.imgAvatar, displayName);
        });
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    // =========================================================================
    // FIREBASE METADATA FETCHERS WITH CACHE PERSISTENCE
    // =========================================================================
    private void fetchUserInfo(String userId, OnUserInfoFetched callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFetched(DEFAULT_USERNAME, null);
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    String name = doc.getString("displayName");
                    String url = doc.getString("avatarUrl");
                    String resolvedName = (name != null && !name.trim().isEmpty()) ? name.trim() : DEFAULT_USERNAME;

                    // Store details in local memory map cache
                    userCache.put(userId, new UserProfileCache(resolvedName, url));
                    callback.onFetched(resolvedName, url);
                })
                .addOnFailureListener(e -> {
                    // Fallback configuration on task exception
                    callback.onFetched(DEFAULT_USERNAME, null);
                });
    }

    private interface OnUserInfoFetched {
        void onFetched(String displayName, String avatarUrl);
    }

    // Helper model to encapsulate stored user profile contexts
    private static class UserProfileCache {
        final String displayName;
        final String avatarUrl;

        UserProfileCache(String displayName, String avatarUrl) {
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }
    }

    // =========================================================================
    // SPANNABLE TEXT BUILDER
    // =========================================================================
    private SpannableStringBuilder buildSpannable(String userName, String message) {
        String full = userName + " " + (message != null ? message : "");
        SpannableStringBuilder ssb = new SpannableStringBuilder(full);
        int end = userName.length();

        // Enforce strong typography styling on actor names
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    static class LogViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvMessage, tvTime;
        View viewLine;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewLine = itemView.findViewById(R.id.viewTimelineLine);
        }
    }
}