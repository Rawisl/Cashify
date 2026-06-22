package com.example.cashify.ui.FriendsActivity;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.common.AvatarImageView;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_SEE_ALL = 1;

    public interface ActionListener {
        void onAddFriend(User user);
        void onCancelRequest(User user);
        void onSeeAll();
        void onAvatarClick(User user);
    }

    private List<User> users;
    private final ActionListener listener;
    private boolean showSeeAllCard;

    public SuggestionAdapter(List<User> users, ActionListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
        this.showSeeAllCard = false;
    }

    public SuggestionAdapter(List<User> users, boolean showSeeAllCard, ActionListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.showSeeAllCard = showSeeAllCard;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position < userCount() ? TYPE_USER : TYPE_SEE_ALL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SEE_ALL
                ? R.layout.item_friend_suggestion_see_all
                : R.layout.item_friend_suggestion;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return viewType == TYPE_SEE_ALL ? new SeeAllViewHolder(view) : new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder instanceof SeeAllViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSeeAll();
            });
            return;
        }

        User user = users.get(position);
        UserViewHolder userHolder = (UserViewHolder) holder;

        userHolder.tvFriendName.setText(user.getNameToShow());
        userHolder.tvStatus.setText(user.getEmail() != null ? user.getEmail() : "Cashify User");
        ImageHelper.loadAvatar(user.getAvatarUrl(), userHolder.imgAvatar, user.getNameToShow());

        userHolder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAvatarClick(user);
        });

        // Status: 2 = Request Sent (Pending)
        boolean isRequestPending = user.getFriendStatus() == 2;

        userHolder.btnAddFriend.setVisibility(isRequestPending ? View.GONE : View.VISIBLE);
        userHolder.tvSentRequest.setVisibility(isRequestPending ? View.VISIBLE : View.GONE);

        userHolder.btnAddFriend.setOnClickListener(v -> {
            if (listener != null) listener.onAddFriend(user);
        });

        userHolder.tvSentRequest.setOnClickListener(v -> {
            if (listener != null) listener.onCancelRequest(user);
        });
    }

    @Override
    public int getItemCount() {
        return userCount() + (showSeeAllCard ? 1 : 0);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<User> newList) {
        this.users = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setShowSeeAllCard(boolean showSeeAllCard) {
        this.showSeeAllCard = showSeeAllCard;
        notifyDataSetChanged();
    }

    private int userCount() {
        return users != null ? users.size() : 0;
    }

    // =========================================================================
    // VIEW HOLDERS
    // =========================================================================

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class UserViewHolder extends ViewHolder {
        AvatarImageView imgAvatar;
        TextView tvFriendName, tvStatus, tvSentRequest;
        MaterialButton btnAddFriend;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSentRequest = itemView.findViewById(R.id.tvSentRequest);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }
    }

    static class SeeAllViewHolder extends ViewHolder {
        SeeAllViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}