package com.example.cashify.ui.feed;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CommunityFeedAdapter extends ListAdapter<FeedItem, RecyclerView.ViewHolder> {
    private final Set<String> expandedItemIds = new HashSet<>();
    private final OnPostClickListener postClickListener;

    public interface OnPostClickListener {
        void onPostClick(FeedItem item);
    }

    public CommunityFeedAdapter() {
        this(null);
    }

    public CommunityFeedAdapter(OnPostClickListener postClickListener) {
        super(DIFF_CALLBACK);
        this.postClickListener = postClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == FeedItem.TYPE_MILESTONE) {
            View view = inflater.inflate(R.layout.item_post_milestone, parent, false);
            return new MilestoneViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_post_normal, parent, false);
        return new NormalPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FeedItem item = getItem(position);
        boolean expanded = expandedItemIds.contains(item.getId());
        if (holder instanceof NormalPostViewHolder && item instanceof FeedItem.NormalPost) {
            ((NormalPostViewHolder) holder).bind((FeedItem.NormalPost) item, expanded);
        } else if (holder instanceof MilestoneViewHolder && item instanceof FeedItem.MilestonePost) {
            ((MilestoneViewHolder) holder).bind((FeedItem.MilestonePost) item, expanded);
        }
    }

    private void toggleExpanded(String itemId, int position) {
        if (expandedItemIds.contains(itemId)) {
            expandedItemIds.remove(itemId);
        } else {
            expandedItemIds.add(itemId);
        }
        notifyItemChanged(position);
    }

    private void showCardMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.getMenu().add("Chỉnh sửa");
        popupMenu.getMenu().add("Xóa");
        popupMenu.setOnMenuItemClickListener(item -> {
            Toast.makeText(anchor.getContext(), item.getTitle() + " bài viết", Toast.LENGTH_SHORT).show();
            return true;
        });
        popupMenu.show();
    }

    class NormalPostViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatar;
        private final TextView name;
        private final TextView time;
        private final TextView content;
        private final TextView seeMore;
        private final View imagePlaceholder;
        private final ImageButton menuButton;

        NormalPostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.txtAvatar);
            name = itemView.findViewById(R.id.txtUserName);
            time = itemView.findViewById(R.id.txtPostTime);
            content = itemView.findViewById(R.id.txtPostContent);
            seeMore = itemView.findViewById(R.id.txtSeeMore);
            imagePlaceholder = itemView.findViewById(R.id.postImagePlaceholder);
            menuButton = itemView.findViewById(R.id.btnPostMenu);
        }

        void bind(FeedItem.NormalPost post, boolean expanded) {
            itemView.setOnClickListener(v -> notifyPostClick(post));
            avatar.setText(post.initials);
            avatar.setBackground(makeAvatarBlock(post.avatarColor));
            name.setText(post.userName);
            time.setText(post.time);
            content.setText(post.text);
            content.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            imagePlaceholder.setVisibility(post.hasImage ? View.VISIBLE : View.GONE);
            bindSeeMore(seeMore, content, post.getId(), post.expandable, expanded, this);
            menuButton.setOnClickListener(CommunityFeedAdapter.this::showCardMenu);
        }
    }

    class MilestoneViewHolder extends RecyclerView.ViewHolder {
        private final TextView icon;
        private final TextView title;
        private final TextView description;
        private final TextView seeMore;
        private final TextView month;
        private final TextView amount;
        private final ProgressBar progressBar;
        private final ImageButton menuButton;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.txtMilestoneIcon);
            title = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            seeMore = itemView.findViewById(R.id.txtMilestoneSeeMore);
            month = itemView.findViewById(R.id.txtMilestoneMonth);
            amount = itemView.findViewById(R.id.txtMilestoneAmount);
            progressBar = itemView.findViewById(R.id.progressMilestone);
            menuButton = itemView.findViewById(R.id.btnMilestoneMenu);
        }

        void bind(FeedItem.MilestonePost milestone, boolean expanded) {
            itemView.setOnClickListener(v -> notifyPostClick(milestone));
            icon.setText(milestone.iconText);
            title.setText(milestone.title);
            description.setText(milestone.description);
            description.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            month.setText(milestone.month);
            amount.setText(milestone.amount);
            progressBar.setProgress(milestone.progress);
            bindSeeMore(seeMore, description, milestone.getId(), milestone.expandable, expanded, this);
            menuButton.setOnClickListener(CommunityFeedAdapter.this::showCardMenu);
        }
    }

    private void notifyPostClick(FeedItem item) {
        if (postClickListener != null) {
            postClickListener.onPostClick(item);
        }
    }

    private void bindSeeMore(
            TextView button,
            TextView content,
            String itemId,
            boolean expandable,
            boolean expanded,
            RecyclerView.ViewHolder holder
    ) {
        if (!expandable) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        button.setVisibility(View.VISIBLE);
        button.setText(expanded ? "Thu gọn" : "Xem thêm...");
        button.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                toggleExpanded(itemId, position);
            }
        });
    }

    private GradientDrawable makeAvatarBlock(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(12 * itemViewDensity());
        drawable.setColor(color);
        drawable.setStroke((int) (3 * itemViewDensity()), Color.BLACK);
        return drawable;
    }

    private float itemViewDensity() {
        return 1f;
    }

    private static final DiffUtil.ItemCallback<FeedItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<FeedItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FeedItem oldItem, @NonNull FeedItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FeedItem oldItem, @NonNull FeedItem newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
}
