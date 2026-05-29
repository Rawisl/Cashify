package com.example.cashify.ui.feed;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// FIX 1: Bỏ "import okhttp3.Callback" — xung đột với retrofit2.Callback
//         dùng retrofit2.Call / retrofit2.Callback / retrofit2.Response trực tiếp

public class CommunityFeedAdapter extends ListAdapter<FeedItem, RecyclerView.ViewHolder> {

    private final Set<String> expandedItemIds = new HashSet<>();

    // FIX 2: Khai báo likedItemIds — trước đây dùng khắp nơi nhưng chưa có field
    private final Set<String> likedItemIds = new HashSet<>();

    private final OnPostClickListener postClickListener;
    private final OnPostMenuClickListener menuClickListener; // Khai báo biến

    // THÊM MỚI: Lắng nghe sự kiện bấm vào Avatar từ Bảng tin
    private OnAvatarClickListener avatarClickListener;

    public interface OnPostClickListener {
        void onPostClick(FeedItem item);
    }

    // THÊM MỚI: Interface định nghĩa sự kiện click Avatar
    public interface OnAvatarClickListener {
        void onAvatarClick(String userId);
    }

    public interface OnPostMenuClickListener {
        void onMenuClick(FeedItem item);
    }

    public CommunityFeedAdapter(OnPostClickListener postClickListener, OnPostMenuClickListener menuClickListener) {
        super(DIFF_CALLBACK);
        this.postClickListener = postClickListener;
        this.menuClickListener = menuClickListener;
    }

    // THÊM MỚI: Setter để Fragment cấu hình hành động điều hướng sang Profile
    public void setOnAvatarClickListener(OnAvatarClickListener avatarClickListener) {
        this.avatarClickListener = avatarClickListener;
    }

    /** Gọi từ Fragment sau khi parse feed để đánh dấu các post đã liked */
    public void addLikedId(String id) {
        likedItemIds.add(id);
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

    // =========================================================================
    // FIX 3: Implement applyLikeState() — trước đây được gọi nhưng không tồn tại
    // =========================================================================
    private void applyLikeState(TextView btn, boolean liked) {
        Log.d("LIKE_DEBUG", "applyLikeState called: liked=" + liked + " | caller=" + getCallerInfo());
        int colorRes = liked ? R.color.status_red : R.color.item_description;
        int color = androidx.core.content.ContextCompat.getColor(btn.getContext(), colorRes);
        btn.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        btn.setTextColor(color);
    }

    // =========================================================================
    // FIX 4: rollback() dùng applyLikeState() đã được implement ở trên
    // =========================================================================
    private void rollback(String postId, boolean failedState, TextView btn) {
        Log.d("LIKE_DEBUG", "ROLLBACK called: postId=" + postId + " failedState=" + failedState);
        if (failedState) {
            likedItemIds.remove(postId);
        } else {
            likedItemIds.add(postId);
        }
        applyLikeState(btn, !failedState);
    }

    private String getCallerInfo() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[4];
        return caller.getMethodName() + ":" + caller.getLineNumber();
    }

    // =========================================================================
    // NormalPostViewHolder
    // =========================================================================
    class NormalPostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar;
        private final TextView txtAvatar;
        private final TextView name;
        private final TextView time;
        private final TextView content;
        private final TextView seeMore;
        private final View imagePlaceholder;
        private final ImageView imgPostImage;
        private final View decorCircle;
        private final ImageView decorIcon;
        private final TextView decorCaption;
        private final ImageButton menuButton;
        private final TextView btnLike;
        private final TextView tvLikeCount;

        NormalPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar        = itemView.findViewById(R.id.imgAvatar);
            txtAvatar        = itemView.findViewById(R.id.txtAvatar);
            name             = itemView.findViewById(R.id.txtUserName);
            time             = itemView.findViewById(R.id.txtPostTime);
            content          = itemView.findViewById(R.id.txtPostContent);
            seeMore          = itemView.findViewById(R.id.txtSeeMore);
            imagePlaceholder = itemView.findViewById(R.id.postImagePlaceholder);
            imgPostImage     = itemView.findViewById(R.id.imgPostImage);
            decorCircle      = itemView.findViewById(R.id.decorCircle);
            decorIcon        = itemView.findViewById(R.id.decorIcon);
            decorCaption     = itemView.findViewById(R.id.decorCaption);
            menuButton       = itemView.findViewById(R.id.btnPostMenu);
            btnLike          = itemView.findViewById(R.id.btnLike);
            tvLikeCount      = itemView.findViewById(R.id.tvLikeCount);
        }

        void bind(FeedItem.NormalPost post, boolean expanded) {
            itemView.setOnClickListener(v -> notifyPostClick(post));

            // THÊM MỚI: Tách hàm xử lý click Avatar (gọi an toàn từ instance context)
            View.OnClickListener onAvatarClicked = v -> {
                if (avatarClickListener != null && post.getUserId() != null) {
                    avatarClickListener.onAvatarClick(post.getUserId());
                }
            };

            // Avatar
            if (post.avatarUrl != null && !post.avatarUrl.isEmpty()) {
                imgAvatar.setVisibility(View.VISIBLE);
                txtAvatar.setVisibility(View.GONE);
                Glide.with(itemView.getContext())
                        .load(post.avatarUrl)
                        .placeholder(R.drawable.ic_default_user)
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                imgAvatar.setVisibility(View.GONE);
                txtAvatar.setVisibility(View.VISIBLE);
                txtAvatar.setText(post.initials);
                // FIX 5: truyền context vào makeAvatarBlock để lấy density thật
                txtAvatar.setBackground(makeAvatarBlock(post.avatarColor, itemView.getContext()));
            }

            // THÊM MỚI: Đăng ký click sự kiện cho cả ảnh Avatar hoặc Text Avatar đại diện
            imgAvatar.setOnClickListener(onAvatarClicked);
            txtAvatar.setOnClickListener(onAvatarClicked);
            name.setOnClickListener(onAvatarClicked); // Bấm vào tên tác giả cũng mở được trang cá nhân

            name.setText(post.userName);
            time.setText(post.time);
            content.setText(post.text);
            content.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            bindSeeMore(seeMore, content, post.getId(), post.expandable, expanded, this);
            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(post); // post hoặc milestone tùy ViewHolder
            });
            // Ảnh bài viết
            if (post.hasImage && post.imageUrl != null && !post.imageUrl.isEmpty()) {
                imagePlaceholder.setVisibility(View.VISIBLE);
                imgPostImage.setVisibility(View.VISIBLE);
                decorCircle.setVisibility(View.GONE);
                decorIcon.setVisibility(View.GONE);
                decorCaption.setVisibility(View.GONE);
                Glide.with(itemView.getContext())
                        .load(post.imageUrl)
                        .placeholder(R.drawable.bg_feed_image_placeholder)
                        .error(R.drawable.bg_feed_image_placeholder)
                        .centerCrop()
                        .into(imgPostImage);
            } else {
                imagePlaceholder.setVisibility(View.GONE);
                imgPostImage.setVisibility(View.GONE);
                decorCircle.setVisibility(View.VISIBLE);
                decorIcon.setVisibility(View.VISIBLE);
                decorCaption.setVisibility(View.VISIBLE);
            }

            // FIX 6: Đọc trạng thái like ban đầu từ likedItemIds thay vì luôn reset false
            boolean currentlyLiked = likedItemIds.contains(post.getId());
            applyLikeState(btnLike, currentlyLiked);

            btnLike.setOnClickListener(v -> {
                boolean nowLiked = !likedItemIds.contains(post.getId());

                // Optimistic UI — đổi màu ngay không chờ API
                if (nowLiked) {
                    likedItemIds.add(post.getId());
                } else {
                    likedItemIds.remove(post.getId());
                }
                applyLikeState(btnLike, nowLiked);

                // Lấy token và gọi API
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(false)
                        .addOnSuccessListener(tokenResult -> {
                            String token = "Bearer " + tokenResult.getToken();

                            // FIX 7: Callback đầy đủ signature — trước đây dùng "..." placeholder
                            ApiClient.getClient()
                                    .create(ApiService.class)
                                    .toggleLike(token, new ApiService.LikeActionRequest(post.getId(), nowLiked))
                                    .enqueue(new retrofit2.Callback<Object>() {
                                        @Override
                                        public void onResponse(
                                                @NonNull retrofit2.Call<Object> call,
                                                @NonNull retrofit2.Response<Object> response) {
                                            if (!response.isSuccessful()) {
                                                rollback(post.getId(), nowLiked, btnLike);
                                            }
                                        }

                                        @Override
                                        public void onFailure(
                                                @NonNull retrofit2.Call<Object> call,
                                                @NonNull Throwable t) {
                                            rollback(post.getId(), nowLiked, btnLike);
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> rollback(post.getId(), nowLiked, btnLike));
            });
        }
    }

    // =========================================================================
    // MilestoneViewHolder
    // =========================================================================
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
            icon        = itemView.findViewById(R.id.txtMilestoneIcon);
            title       = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            seeMore     = itemView.findViewById(R.id.txtMilestoneSeeMore);
            month       = itemView.findViewById(R.id.txtMilestoneMonth);
            amount      = itemView.findViewById(R.id.txtMilestoneAmount);
            progressBar = itemView.findViewById(R.id.progressMilestone);
            menuButton  = itemView.findViewById(R.id.btnMilestoneMenu);
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
            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(milestone); // post hoặc milestone tùy ViewHolder
            });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
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

    // FIX 5: Nhận Context để lấy display density thật, không hardcode 1f
    private GradientDrawable makeAvatarBlock(int color, android.content.Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(12 * density);
        drawable.setColor(color);
        drawable.setStroke((int) (3 * density), Color.BLACK);
        return drawable;
    }

    private static final DiffUtil.ItemCallback<FeedItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FeedItem>() {
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