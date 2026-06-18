package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ImageHelper;

import java.util.ArrayList;
import java.util.HashSet;
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
        this.list = list != null ? list : new ArrayList<>();
        this.selectedUids = selectedUids != null ? selectedUids : new HashSet<>();
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<User> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
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

        ImageHelper.loadAvatar(user.getAvatarUrl(), holder.imgAvatar, user.getNameToShow());

        // CRITICAL: Detach the listener before setting the checked state to prevent infinite loops during view recycling
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedUids.contains(user.getUid()));

        // Reattach listener to handle user interactions
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUids.add(user.getUid());
            } else {
                selectedUids.remove(user.getUid());
            }

            // Dispatch callback to parent View
            if (listener != null) {
                listener.onSelectionChanged(selectedUids.size());
            }
        });

        // Expand tap target to the entire row
        holder.itemView.setOnClickListener(v -> holder.cbSelect.setChecked(!holder.cbSelect.isChecked()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // =========================================================================
    // VIEW HOLDER
    // =========================================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
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