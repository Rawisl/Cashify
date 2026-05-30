package com.example.cashify.ui.feed;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.cashify.R;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.ImageHelper;
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

    public CommunityFeedAdapter(OnPostClickListener postClickListener) {
        this(postClickListener, null);
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

    public void clearLikedIds() {
        likedItemIds.clear();
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

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof MilestoneViewHolder) {
            ((MilestoneViewHolder) holder).stopShineAnimation();
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
                ImageHelper.loadAvatar(post.avatarUrl, imgAvatar, post.userName);
            } else {
                imgAvatar.setVisibility(View.VISIBLE);
                txtAvatar.setVisibility(View.GONE);
                ImageHelper.loadAvatar(null, imgAvatar, post.userName);
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
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(1080, 720)
                        .centerCrop()
                        .dontAnimate()
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
        private final ImageView imgAvatar;
        private final TextView txtAvatar;
        private final TextView name;
        private final TextView icon;
        private final TextView title;
        private final TextView description;
        private final TextView seeMore;
        private final TextView month;
        private final TextView amount;
        private final View goalPanel;
        private final ImageButton menuButton;
        private final TextView btnLike;
        private final TextView btnComment;
        private final TextView btnShare;
        private final View shineView;
        private AnimatorSet shineAnimator;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            shineView   = itemView.findViewById(R.id.viewMilestoneShine);
            imgAvatar   = itemView.findViewById(R.id.imgMilestoneAvatar);
            txtAvatar   = itemView.findViewById(R.id.txtMilestoneAvatar);
            name        = itemView.findViewById(R.id.txtMilestoneUserName);
            icon        = itemView.findViewById(R.id.txtMilestoneIcon);
            title       = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            seeMore     = itemView.findViewById(R.id.txtMilestoneSeeMore);
            month       = itemView.findViewById(R.id.txtMilestoneMonth);
            amount      = itemView.findViewById(R.id.txtMilestoneAmount);
            goalPanel   = itemView.findViewById(R.id.layoutMilestoneGoalPanel);
            menuButton  = itemView.findViewById(R.id.btnMilestoneMenu);
            btnLike     = itemView.findViewById(R.id.btnMilestoneLike);
            btnComment  = itemView.findViewById(R.id.btnMilestoneComment);
            btnShare    = itemView.findViewById(R.id.btnMilestoneShare);
        }

        void bind(FeedItem.MilestonePost milestone, boolean expanded) {
            itemView.setOnClickListener(v -> notifyPostClick(milestone));
            startShineAnimation();
            icon.setText("");
            name.setText(milestone.userName);
            title.setText(milestone.title);
            description.setText(milestone.description);
            description.setVisibility(milestone.description == null || milestone.description.trim().isEmpty()
                    ? View.GONE : View.VISIBLE);
            description.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            month.setText(milestone.time == null || milestone.time.isEmpty() ? milestone.month : milestone.time);
            amount.setText(milestone.amount);
            goalPanel.setVisibility(hasMeaningfulMilestoneAmount(milestone.amount) ? View.VISIBLE : View.GONE);
            bindSeeMore(seeMore, description, milestone.getId(), milestone.expandable, expanded, this);
            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(milestone); // post hoặc milestone tùy ViewHolder
            });

            View.OnClickListener onAvatarClicked = v -> {
                if (avatarClickListener != null && milestone.getUserId() != null) {
                    avatarClickListener.onAvatarClick(milestone.getUserId());
                }
            };
            imgAvatar.setVisibility(View.VISIBLE);
            txtAvatar.setVisibility(View.GONE);
            ImageHelper.loadAvatar(milestone.avatarUrl, imgAvatar, milestone.userName);
            imgAvatar.setOnClickListener(onAvatarClicked);
            txtAvatar.setOnClickListener(onAvatarClicked);
            name.setOnClickListener(onAvatarClicked);

            boolean currentlyLiked = likedItemIds.contains(milestone.getId());
            applyLikeState(btnLike, currentlyLiked);
            btnLike.setOnClickListener(v -> {
                boolean nowLiked = !likedItemIds.contains(milestone.getId());
                if (nowLiked) {
                    likedItemIds.add(milestone.getId());
                } else {
                    likedItemIds.remove(milestone.getId());
                }
                applyLikeState(btnLike, nowLiked);

                FirebaseAuth.getInstance().getCurrentUser().getIdToken(false)
                        .addOnSuccessListener(tokenResult -> {
                            String token = "Bearer " + tokenResult.getToken();
                            ApiClient.getClient()
                                    .create(ApiService.class)
                                    .toggleLike(token, new ApiService.LikeActionRequest(milestone.getId(), nowLiked))
                                    .enqueue(new retrofit2.Callback<Object>() {
                                        @Override
                                        public void onResponse(
                                                @NonNull retrofit2.Call<Object> call,
                                                @NonNull retrofit2.Response<Object> response) {
                                            if (!response.isSuccessful()) {
                                                rollback(milestone.getId(), nowLiked, btnLike);
                                            }
                                        }

                                        @Override
                                        public void onFailure(
                                                @NonNull retrofit2.Call<Object> call,
                                                @NonNull Throwable t) {
                                            rollback(milestone.getId(), nowLiked, btnLike);
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> rollback(milestone.getId(), nowLiked, btnLike));
            });
            btnComment.setOnClickListener(v -> notifyPostClick(milestone));
            btnShare.setOnClickListener(v ->
                    Toast.makeText(itemView.getContext(), "Đã sao chép liên kết bài viết", Toast.LENGTH_SHORT).show());
        }

        void startShineAnimation() {
            if (shineView == null) return;
            stopShineAnimation();
            shineView.setAlpha(0f);
            shineView.setTranslationX(0f);
            shineView.setTranslationY(0f);
            shineView.post(() -> {
                if (shineView.getWindowToken() == null) return;
                float travelX = Math.max(itemView.getWidth(), 360);
                ObjectAnimator moveX = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_X, 0f, travelX + 140f);
                ObjectAnimator moveY = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_Y, 0f, -220f);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(shineView, View.ALPHA, 0f, 0.22f, 0f);
                shineAnimator = new AnimatorSet();
                shineAnimator.playTogether(moveX, moveY, fadeIn);
                shineAnimator.setDuration(2600L);
                shineAnimator.setStartDelay(650L);
                shineAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (shineView.getWindowToken() != null) {
                            startShineAnimation();
                        }
                    }
                });
                shineAnimator.start();
            });
        }

        void stopShineAnimation() {
            if (shineAnimator != null) {
                shineAnimator.removeAllListeners();
                shineAnimator.cancel();
                shineAnimator = null;
            }
            if (shineView != null) shineView.clearAnimation();
        }

        private boolean hasMeaningfulMilestoneAmount(String value) {
            if (value == null) return false;
            String clean = value.trim();
            return !clean.isEmpty()
                    && !clean.equals("100%")
                    && !clean.startsWith("http://")
                    && !clean.startsWith("https://");
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
