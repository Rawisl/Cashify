package com.example.cashify.ui.notifications;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    private List<WorkspaceInvitation> list = new ArrayList<>();
    private final OnInviteClickListener listener;

    // Interface to delegate accept/decline actions to the View/Activity
    public interface OnInviteClickListener {
        void onAccept(WorkspaceInvitation invitation);
        void onDecline(WorkspaceInvitation invitation);
    }

    public InvitationAdapter(OnInviteClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<WorkspaceInvitation> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkspaceInvitation invite = list.get(position);

        holder.tvInviterName.setText(invite.getInviterName());

        holder.tvWorkspaceName.setText("Has invited you to: " + invite.getWorkspaceName());

        ImageHelper.loadAvatar(invite.getInviterAvatar(), holder.imgAvatar, invite.getInviterName());

        // Delegate click events
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(invite);
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(invite);
        });
    }

    @Override
    public int getItemCount() {
        return list.size(); // Guaranteed non-null due to setData logic
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInviterName, tvWorkspaceName;
        MaterialButton btnAccept, btnDecline;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInviterName = itemView.findViewById(R.id.tvInviterName);
            tvWorkspaceName = itemView.findViewById(R.id.tvWorkspaceName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}