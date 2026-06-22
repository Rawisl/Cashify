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

public class WorkspaceMemberAdapter extends RecyclerView.Adapter<WorkspaceMemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private String ownerId = "";

    public WorkspaceMemberAdapter(List<User> memberList) {
        this.memberList = memberList != null ? memberList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_avatar, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User currentMember = memberList.get(position);

        if (currentMember != null) {
            String fullName = currentMember.getDisplayName();
            holder.tvName.setText(fullName != null ? fullName : "Anonymous");
            ImageHelper.loadAvatar(currentMember.getAvatarUrl(), holder.imgAvatar, fullName);

            boolean isOwner = currentMember.getUid() != null && currentMember.getUid().equals(ownerId);
            holder.imgOwnerBadge.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMembers(List<User> newMembers) {
        this.memberList = newMembers != null ? newMembers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId != null ? ownerId : "";
        notifyDataSetChanged();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        ImageView imgOwnerBadge;
        TextView tvName;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgMemberAvatar);
            imgOwnerBadge = itemView.findViewById(R.id.imgOwnerBadge);
            tvName = itemView.findViewById(R.id.tvMemberName);
        }
    }
}