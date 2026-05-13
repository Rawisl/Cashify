package com.example.cashify.ui.workspace;

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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class WorkspaceMemberListAdapter extends RecyclerView.Adapter<WorkspaceMemberListAdapter.ViewHolder> {

    public static final int MODE_VIEW_ONLY = 0;
    public static final int MODE_MANAGE_KICK = 1;
    public static final int MODE_TRANSFER_OWNER = 2;

    private List<User> memberList;
    private int mode;
    private final String currentUserId;
    private final OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onKickClicked(User targetUser);
        void onTransferClicked(User targetUser);
    }

    public WorkspaceMemberListAdapter(List<User> memberList, int mode, String currentUserId, OnMemberActionListener listener) {
        this.memberList = memberList;
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

        // Thêm chữ (Owner) hoặc (You) cho dễ nhìn
        String displayName = user.getDisplayName();
        if (user.getUid().equals(currentUserId)) {
            displayName += " (You)";
        }
        holder.tvName.setText(displayName);
        ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar);

        // ==========================================
        // LOGIC BIẾN HÌNH THEO MODE
        // ==========================================
        if (mode == MODE_MANAGE_KICK) { // Đây giờ là chế độ duy nhất cho Owner
            if (!user.getUid().equals(currentUserId)) {
                holder.btnKick.setVisibility(View.VISIBLE);
                holder.btnTransfer.setVisibility(View.VISIBLE); // Hiện nút transfer

                holder.btnKick.setOnClickListener(v -> listener.onKickClicked(user));
                holder.btnTransfer.setOnClickListener(v -> listener.onTransferClicked(user));
            } else {
                holder.btnKick.setVisibility(View.GONE);
                holder.btnTransfer.setVisibility(View.GONE);
            }
        } else { // MODE_VIEW_ONLY
            holder.btnKick.setVisibility(View.GONE);
            holder.btnTransfer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    public void setMembers(List<User> newMembers) {
        this.memberList = newMembers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgAvatar;
        TextView tvName;
        ImageView btnKick;
        ImageView btnTransfer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgListAvatar);
            tvName = itemView.findViewById(R.id.tvListName);
            btnKick = itemView.findViewById(R.id.btnKick);
            btnTransfer = itemView.findViewById(R.id.btnTransfer);
        }
    }
    public void updateMode(int newMode) {
        if (this.mode != newMode) {
            this.mode = newMode;
            notifyDataSetChanged(); // Yêu cầu vẽ lại toàn bộ danh sách
        }
    }
}