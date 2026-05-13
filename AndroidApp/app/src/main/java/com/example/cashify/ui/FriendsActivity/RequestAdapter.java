package com.example.cashify.ui.FriendsActivity;

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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    public interface RequestActionListener {
        void onAccept(User user);

        void onDecline(User user);

        void onCancel(User user);
    }

    private List<User> users;
    private final RequestActionListener listener;
    private final boolean isIncoming; // True = Nhận, False = Gửi

    public RequestAdapter(List<User> users, boolean isIncoming, RequestActionListener listener) {
        this.users = users;
        this.isIncoming = isIncoming;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvFriendName.setText(user.getNameToShow());
        holder.tvStatus.setText(user.getEmail());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_user);
        }

        // Hiện nút theo Tab (KHÔNG cần ẩn mấy nút thừa nữa vì giao diện sếp làm gì có mà ẩn)
        if (isIncoming) {
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.GONE);

            holder.btnAccept.setOnClickListener(v -> listener.onAccept(user));
            holder.btnDecline.setOnClickListener(v -> listener.onDecline(user));
        } else {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);

            holder.btnCancel.setVisibility(View.VISIBLE); // Nút Cancel Request của sếp
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(user));
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public void updateData(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Chỉ giữ lại những biến THỰC SỰ CÓ trong item_request.xml
        TextView tvFriendName, tvStatus;
        ImageView imgAvatar;
        MaterialButton btnAccept, btnDecline, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Ánh xạ chuẩn xác với ID trong item_request.xml
            imgAvatar = itemView.findViewById(R.id.imgAvatar); // Bổ sung ánh xạ Avatar
            tvFriendName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvEmail);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnCancel = itemView.findViewById(R.id.btnCancel); // Tớ đổi luôn thành btnCancel cho đúng nghĩa
        }
    }
}