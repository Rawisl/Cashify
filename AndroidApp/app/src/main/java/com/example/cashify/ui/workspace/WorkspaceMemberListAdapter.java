package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceMemberListAdapter extends RecyclerView.Adapter<WorkspaceMemberListAdapter.ViewHolder> {

    // Roles and Permissions Constants
    public static final int MODE_VIEW_ONLY = 0;
    public static final int MODE_MANAGE_KICK = 1;
    public static final int MODE_TRANSFER_OWNER = 2; // Reserved for future scaling

    private List<User> memberList;
    private int mode;
    private final String currentUserId;
    private final OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onKickClicked(User targetUser);
        void onTransferClicked(User targetUser);
    }

    public WorkspaceMemberListAdapter(List<User> memberList, int mode, String currentUserId, OnMemberActionListener listener) {
        this.memberList = memberList != null ? memberList : new ArrayList<>();
        this.mode = mode;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = memberList.get(position);
        if (user == null) return;

        // Visual identifier for the current user
        String displayName = user.getDisplayName();
        if (user.getUid().equals(currentUserId)) {
            displayName += " (You)";
        }

        holder.tvName.setText(displayName);
        ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar, displayName);

        // =========================================================================
        // PERMISSION-BASED UI RENDERING
        // =========================================================================
        if (mode == MODE_MANAGE_KICK) {
            // Admin/Owner Mode: Display management actions for all users EXCEPT themselves
            if (!user.getUid().equals(currentUserId)) {
                holder.btnKick.setVisibility(View.VISIBLE);
                holder.btnTransfer.setVisibility(View.VISIBLE);

                holder.btnKick.setOnClickListener(v -> {
                    if (listener != null) listener.onKickClicked(user);
                });

                holder.btnTransfer.setOnClickListener(v -> {
                    if (listener != null) listener.onTransferClicked(user);
                });
            } else {
                holder.btnKick.setVisibility(View.GONE);
                holder.btnTransfer.setVisibility(View.GONE);
            }
        } else {
            // View-Only Mode: Hide all management actions
            holder.btnKick.setVisibility(View.GONE);
            holder.btnTransfer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMembers(List<User> newMembers) {
        this.memberList = newMembers != null ? newMembers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMode(int newMode) {
        if (this.mode != newMode) {
            this.mode = newMode;
            notifyDataSetChanged();
        }
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnKick, btnTransfer;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgListAvatar);
            tvName = itemView.findViewById(R.id.tvListName);
            btnKick = itemView.findViewById(R.id.btnKick);
            btnTransfer = itemView.findViewById(R.id.btnTransfer);
        }
    }
}