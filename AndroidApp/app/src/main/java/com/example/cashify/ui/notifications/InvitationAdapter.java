package com.example.cashify.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cashify.R;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    private List<WorkspaceInvitation> list = new ArrayList<>();
    private final OnInviteClickListener listener;

    public interface OnInviteClickListener {
        void onAccept(WorkspaceInvitation invitation);
        void onDecline(WorkspaceInvitation invitation);
    }

    public InvitationAdapter(OnInviteClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<WorkspaceInvitation> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkspaceInvitation invite = list.get(position);

        holder.tvInviterName.setText(invite.getInviterName());
        holder.tvWorkspaceName.setText("Has invited you to: " + invite.getWorkspaceName());

        if (invite.getInviterAvatar() != null && !invite.getInviterAvatar().isEmpty()) {
            com.example.cashify.utils.ImageHelper.loadAvatar(invite.getInviterAvatar(), holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_user);
        }

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(invite));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(invite));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInviterName, tvWorkspaceName;
        MaterialButton btnAccept, btnDecline;
        android.widget.ImageView imgAvatar;

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