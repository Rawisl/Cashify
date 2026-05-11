package com.example.cashify.ui.workspace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Set;

public class SelectFriendAdapter extends RecyclerView.Adapter<SelectFriendAdapter.ViewHolder> {

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<User> list;
    private final Set<String> selectedUids;
    private final OnSelectionChangeListener listener;

    public SelectFriendAdapter(List<User> list, Set<String> selectedUids, OnSelectionChangeListener listener) {
        this.list = list;
        this.selectedUids = selectedUids;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = list.get(position);
        holder.tvName.setText(user.getNameToShow());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_user);
        }

        // Gỡ listener trước khi set trạng thái để không bị lặp vô tận
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedUids.contains(user.getUid()));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedUids.add(user.getUid());
            else selectedUids.remove(user.getUid());
            listener.onSelectionChanged(selectedUids.size()); // Báo về Activity/Fragment cha
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelect.setChecked(!holder.cbSelect.isChecked()));
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgAvatar;
        TextView tvName;
        CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}