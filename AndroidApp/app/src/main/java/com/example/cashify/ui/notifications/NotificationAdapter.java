package com.example.cashify.ui.notifications;

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

    public void setData(List<NotificationItem> newList) {
        this.list = newList;
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

        // Dùng hàm tính thời gian tự chế bên dưới để không bị lỗi
        holder.tvTime.setText(formatTimeAgo(notif.getTimestamp()));

        holder.viewUnreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
        holder.itemView.setBackgroundColor(notif.isRead() ? Color.TRANSPARENT : Color.parseColor("#F9FAFC"));

        switch (notif.getType()) {
            case "FRIEND_REQUEST":
                holder.imgIcon.setImageResource(R.drawable.ic_friends_solid);
                holder.imgIcon.setColorFilter(Color.parseColor("#E91E63"));
                break;
            case "WORKSPACE_INVITE":
                holder.imgIcon.setImageResource(android.R.drawable.ic_dialog_email);
                holder.imgIcon.setColorFilter(Color.parseColor("#4C6FFF"));
                break;
            case "WORKSPACE_TRANS":
                holder.imgIcon.setImageResource(R.drawable.ic_salary);
                holder.imgIcon.setColorFilter(Color.parseColor("#FF9800"));
                break;
            case "WORKSPACE_CHAT":
                holder.imgIcon.setImageResource(android.R.drawable.ic_menu_send); // Icon thay thế
                holder.imgIcon.setColorFilter(Color.parseColor("#4CAF50"));
                break;
            default:
                holder.imgIcon.setImageResource(android.R.drawable.ic_popup_reminder);
                holder.imgIcon.setColorFilter(Color.parseColor("#757575"));
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(notif));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    // Hàm tự động tính "Vừa xong", "5 phút trước"...
    private String formatTimeAgo(long timestamp) {
        if (timestamp == 0) return "";
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return timeAgo.toString();
    }

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