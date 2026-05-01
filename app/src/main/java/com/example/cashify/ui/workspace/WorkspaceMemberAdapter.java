package com.example.cashify.ui.workspace;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class WorkspaceMemberAdapter extends RecyclerView.Adapter<WorkspaceMemberAdapter.MemberViewHolder> {

    private List<User> memberList;

    public WorkspaceMemberAdapter(List<User> memberList) {
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_avatar, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User currentMember = memberList.get(position);

        if (currentMember != null) {
            // 1. Gán tên (Có thể dùng split để lấy tên thật ngắn, ví dụ "Trần Hài" -> "Hài")
            String fullName = currentMember.getDisplayName();
            holder.tvName.setText(fullName != null ? fullName : "Anonymous");

            ImageHelper.loadAvatar(currentMember.getAvatarUrl(), holder.imgAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    // Lớp nội bộ để ánh xạ View
    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvName;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgMemberAvatar);
            tvName = itemView.findViewById(R.id.tvMemberName);
        }
    }

    public void setMembers(List<User> newMembers) {
        this.memberList = newMembers;
        notifyDataSetChanged();
    }
}