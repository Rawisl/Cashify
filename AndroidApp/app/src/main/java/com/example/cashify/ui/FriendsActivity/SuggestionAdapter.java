package com.example.cashify.ui.FriendsActivity;

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

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
    private static final int TYPE_USER = 0;
    private static final int TYPE_SEE_ALL = 1;

    public interface ActionListener {
        void onAddFriend(User user);
        void onCancelRequest(User user);
        void onSeeAll();
    }

    private List<User> users;
    private final ActionListener listener;
    private boolean showSeeAllCard;

    public SuggestionAdapter(List<User> users, ActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public SuggestionAdapter(List<User> users, boolean showSeeAllCard, ActionListener listener) {
        this.users = users;
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
            holder.itemView.setOnClickListener(v -> listener.onSeeAll());
            return;
        }

        User user = users.get(position);
        UserViewHolder userHolder = (UserViewHolder) holder;
        userHolder.tvFriendName.setText(user.getNameToShow());
        userHolder.tvStatus.setText(user.getEmail() != null ? user.getEmail() : "Cashify");
        ImageHelper.loadAvatar(user.getAvatarUrl(), userHolder.imgAvatar, user.getNameToShow());

        boolean pending = user.getFriendStatus() == 2;
        userHolder.btnAddFriend.setVisibility(pending ? View.GONE : View.VISIBLE);
        userHolder.tvSentRequest.setVisibility(pending ? View.VISIBLE : View.GONE);
        userHolder.btnAddFriend.setOnClickListener(v -> listener.onAddFriend(user));
        userHolder.tvSentRequest.setOnClickListener(v -> listener.onCancelRequest(user));
    }

    @Override
    public int getItemCount() {
        return userCount() + (showSeeAllCard ? 1 : 0);
    }

    public void updateList(List<User> newList) {
        users = newList;
        notifyDataSetChanged();
    }

    public void setShowSeeAllCard(boolean showSeeAllCard) {
        this.showSeeAllCard = showSeeAllCard;
        notifyDataSetChanged();
    }

    private int userCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class UserViewHolder extends ViewHolder {
        AvatarImageView imgAvatar;
        TextView tvFriendName;
        TextView tvStatus;
        TextView tvSentRequest;
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
