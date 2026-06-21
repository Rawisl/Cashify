package com.example.cashify.ui.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.NotificationItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    // =========================================================================
    // CONSTANTS: Notification Types & Colors
    // =========================================================================
    private static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    private static final String TYPE_WORKSPACE_INVITE = "WORKSPACE_INVITE";
    private static final String TYPE_WORKSPACE_TRANS = "WORKSPACE_TRANS";
    private static final String TYPE_WORKSPACE_CHAT = "WORKSPACE_CHAT";

    private static final int COLOR_UNREAD_BG = Color.parseColor("#F9FAFC");
    private static final int COLOR_ICON_FRIEND = Color.parseColor("#E91E63");
    private static final int COLOR_ICON_INVITE = Color.parseColor("#4C6FFF");
    private static final int COLOR_ICON_TRANS = Color.parseColor("#FF9800");
    private static final int COLOR_ICON_CHAT = Color.parseColor("#4CAF50");
    private static final int COLOR_ICON_DEFAULT = Color.parseColor("#757575");

    private List<NotificationItem> list = new ArrayList<>();
    private final Context context;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onClick(NotificationItem notification);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<NotificationItem> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem notif = list.get(position);

        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        holder.tvTime.setText(formatTimeAgo(notif.getTimestamp()));

        // Manage Unread State UI
        holder.viewUnreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
        holder.itemView.setBackgroundColor(notif.isRead() ? Color.TRANSPARENT : COLOR_UNREAD_BG);

        // Map Notification Types to Icons and Colors
        String type = notif.getType() != null ? notif.getType() : "";
        switch (type) {
            case TYPE_FRIEND_REQUEST:
                holder.imgIcon.setImageResource(R.drawable.ic_friends_solid);
                holder.imgIcon.setColorFilter(COLOR_ICON_FRIEND);
                break;
            case TYPE_WORKSPACE_INVITE:
                holder.imgIcon.setImageResource(android.R.drawable.ic_dialog_email);
                holder.imgIcon.setColorFilter(COLOR_ICON_INVITE);
                break;
            case TYPE_WORKSPACE_TRANS:
                holder.imgIcon.setImageResource(R.drawable.ic_salary);
                holder.imgIcon.setColorFilter(COLOR_ICON_TRANS);
                break;
            case TYPE_WORKSPACE_CHAT:
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_send); // Fallback icon
                holder.imgIcon.setColorFilter(COLOR_ICON_CHAT);
                break;
            default:
                holder.imgIcon.setImageResource(R.drawable.ic_notification_regular);
                holder.imgIcon.setColorFilter(COLOR_ICON_DEFAULT);
                break;
        }

        // Delegate click events
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(notif);
        });
    }

    @Override
    public int getItemCount() {
        return list.size(); // Guaranteed non-null by setData
    }

    /**
     * Converts a raw timestamp into a human-readable relative time string (e.g., "5 mins ago").
     */
    private String formatTimeAgo(long timestamp) {
        if (timestamp <= 0) return "";
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return timeAgo.toString();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle, tvMessage, tvTime;
        View viewUnreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}