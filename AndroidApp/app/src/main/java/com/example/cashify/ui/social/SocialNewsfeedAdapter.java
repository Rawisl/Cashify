package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.cashify.R;
import com.example.cashify.utils.HeartAnimation;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ShineAnimationHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SocialNewsfeedAdapter extends ListAdapter<FeedItem, RecyclerView.ViewHolder> {

    private final Set<String> expandedItemIds = new HashSet<>();
    private final Set<String> likedItemIds = new HashSet<>();

    private final OnPostClickListener postClickListener;
    private final OnPostMenuClickListener menuClickListener;
    private OnAvatarClickListener avatarClickListener;
    private OnLikeClickListener likeClickListener;

    // --- CỜ HIỆU PHÂN CHIA PHÂN CẢNH: FEED VS DETAIL ---
    private final boolean isDetailMode;

    public interface OnPostClickListener { void onPostClick(FeedItem item); }
    public interface OnAvatarClickListener { void onAvatarClick(String userId); }
    public interface OnPostMenuClickListener { void onMenuClick(FeedItem item); }

    public interface OnLikeClickListener {
        void onLikeClick(String postId, boolean isLiked, LikeResultCallback callback);
    }

    public interface LikeResultCallback {
        void onResult(boolean success);
    }

    // Constructor dùng ngoài Newsfeed (Mặc định cắt chữ, có click)
    public SocialNewsfeedAdapter(OnPostClickListener postClickListener, OnPostMenuClickListener menuClickListener) {
        super(DIFF_CALLBACK);
        this.postClickListener = postClickListener;
        this.menuClickListener = menuClickListener;
        this.isDetailMode = false;
    }

    // Constructor chuyên dụng dùng trong màn PostDetailActivity
    public SocialNewsfeedAdapter(OnPostMenuClickListener menuClickListener) {
        super(DIFF_CALLBACK);
        this.postClickListener = null;
        this.menuClickListener = menuClickListener;
        this.isDetailMode = true; // Bật chế độ hiển thị chi tiết bài viết
    }

    public void setOnAvatarClickListener(OnAvatarClickListener avatarClickListener) {
        this.avatarClickListener = avatarClickListener;
    }

    public void setOnLikeClickListener(OnLikeClickListener likeClickListener) {
        this.likeClickListener = likeClickListener;
    }

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

        // Nếu ở màn Detail thì luôn ép trạng thái mở rộng chữ, ngược lại thì kiểm tra mảng ID rộng
        boolean expanded = isDetailMode || expandedItemIds.contains(item.getId());

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
            ((MilestoneViewHolder) holder).stopAnimation();
        }
    }

    private void toggleExpanded(String itemId, int position) {
        if (isDetailMode) return; // Màn detail không cho phép thu gọn chữ
        if (expandedItemIds.contains(itemId)) {
            expandedItemIds.remove(itemId);
        } else {
            expandedItemIds.add(itemId);
        }
        notifyItemChanged(position);
    }

    private void applyLikeState(ImageView icon, TextView text, boolean liked) {
        int colorRes = liked ? R.color.status_red : R.color.icon_inactive;
        int textColorRes = liked ? R.color.status_red : R.color.item_description;

        int color = ContextCompat.getColor(icon.getContext(), colorRes);
        int textColor = ContextCompat.getColor(icon.getContext(), textColorRes);

        icon.setColorFilter(color);
        if (text != null) {
            text.setTextColor(textColor);
        }
    }

    private void handleLikeClick(FeedItem post, ImageView imgLikeHeart, TextView tvLikeCount) {
        boolean nowLiked = !likedItemIds.contains(post.getId());

        if (nowLiked) {
            likedItemIds.add(post.getId());
            post.setLikeCount(post.getLikeCount() + 1);
            HeartAnimation.playRubberBand(imgLikeHeart);
        } else {
            likedItemIds.remove(post.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        }

        tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        applyLikeState(imgLikeHeart, tvLikeCount, nowLiked);

        if (likeClickListener != null) {
            likeClickListener.onLikeClick(post.getId(), nowLiked, success -> {
                if (!success) {
                    if (nowLiked) {
                        likedItemIds.remove(post.getId());
                        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                    } else {
                        likedItemIds.add(post.getId());
                        post.setLikeCount(post.getLikeCount() + 1);
                    }
                    tvLikeCount.setText(String.valueOf(post.getLikeCount()));
                    applyLikeState(imgLikeHeart, tvLikeCount, !nowLiked);
                }
            });
        }
    }

    private void handleCommentClick(FeedItem post, ImageView imgCommentIcon, TextView tvCommentCount) {
        if (!post.isCommented()) {
            post.setCommented(true);
            HeartAnimation.playRubberBand(imgCommentIcon);
            applyCommentState(imgCommentIcon, tvCommentCount, true);
        }
        if (!isDetailMode && postClickListener != null) {
            postClickListener.onPostClick(post);
        }
    }

    private void handleShareClick(FeedItem post, ImageView imgShareIcon, TextView tvShareCount) {
        if (!post.isShared()) {
            post.setShareCount(post.getShareCount() + 1);
            post.setShared(true);
            tvShareCount.setText(String.valueOf(post.getShareCount()));
            HeartAnimation.playRubberBand(imgShareIcon);
            applyShareState(imgShareIcon, tvShareCount, true);
        }
        Toast.makeText(imgShareIcon.getContext(), "Post link copied", Toast.LENGTH_SHORT).show();
    }

    private void applyCommentState(ImageView imgCommentIcon, TextView tvCommentCount, boolean isCommented) {
        if (imgCommentIcon == null) return;
        if (isCommented) {
            int skyBlue = android.graphics.Color.parseColor("#3498DB");
            imgCommentIcon.setColorFilter(skyBlue);
            if (tvCommentCount != null) tvCommentCount.setTextColor(skyBlue);
        } else {
            imgCommentIcon.setColorFilter(ContextCompat.getColor(imgCommentIcon.getContext(), R.color.icon_inactive));
            if (tvCommentCount != null) tvCommentCount.setTextColor(ContextCompat.getColor(tvCommentCount.getContext(), R.color.icon_inactive));
        }
    }

    private void applyShareState(ImageView imgShareIcon, TextView tvShareCount, boolean isShared) {
        if (imgShareIcon == null) return;
        if (isShared) {
            imgShareIcon.setColorFilter(ContextCompat.getColor(imgShareIcon.getContext(), R.color.item_title));
            if (tvShareCount != null) tvShareCount.setTextColor(ContextCompat.getColor(tvShareCount.getContext(), R.color.item_title));
        } else {
            imgShareIcon.setColorFilter(ContextCompat.getColor(imgShareIcon.getContext(), R.color.icon_inactive));
            if (tvShareCount != null) tvShareCount.setTextColor(ContextCompat.getColor(tvShareCount.getContext(), R.color.icon_inactive));
        }
    }

    // =========================================================================
    // NORMAL POST VIEWHOLDER
    // =========================================================================
    class NormalPostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar, imgPostImage, decorIcon, imgLikeHeart, imgCommentIcon, imgShareIcon;
        private final TextView txtAvatar, name, time, title, content, seeMore, decorCaption, tvLikeCount; 
        private final TextView tvCommentCount, tvShareCount;
        private final View imagePlaceholder, btnLike, btnComment, btnShare;
        private final ImageButton menuButton;

        NormalPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            name = itemView.findViewById(R.id.txtUserName);
            time = itemView.findViewById(R.id.txtPostTime);

            // ÁNH XẠ TIÊU ĐỀ & NỘI DUNG
            title = itemView.findViewById(R.id.txtPostTitle);
            content = itemView.findViewById(R.id.txtPostContent);

            seeMore = itemView.findViewById(R.id.txtSeeMore);
            imagePlaceholder = itemView.findViewById(R.id.postImagePlaceholder);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            decorIcon = itemView.findViewById(R.id.decorIcon);
            decorCaption = itemView.findViewById(R.id.decorCaption);
            menuButton = itemView.findViewById(R.id.btnPostMenu);

            btnLike = itemView.findViewById(R.id.btnLike);
            imgLikeHeart = itemView.findViewById(R.id.imgLikeHeart);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);

            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            imgCommentIcon = itemView.findViewById(R.id.imgCommentIcon);

            btnShare = itemView.findViewById(R.id.btnShare);
            tvShareCount = itemView.findViewById(R.id.tvShareCount);
            imgShareIcon = itemView.findViewById(R.id.imgShareIcon);
        }

        void bind(FeedItem.NormalPost post, boolean expanded) {
            if (!isDetailMode) {
                itemView.setOnClickListener(v -> { if (postClickListener != null) postClickListener.onPostClick(post); });
            } else {
                itemView.setOnClickListener(null);
            }

            View.OnClickListener onAvatarClicked = v -> {
                if (avatarClickListener != null && post.getUserId() != null) {
                    avatarClickListener.onAvatarClick(post.getUserId());
                }
            };

            if (post.avatarUrl != null && !post.avatarUrl.isEmpty()) {
                imgAvatar.setVisibility(View.VISIBLE);
                txtAvatar.setVisibility(View.GONE);
                ImageHelper.loadAvatar(post.avatarUrl, imgAvatar, post.userName);
            } else {
                imgAvatar.setVisibility(View.VISIBLE);
                txtAvatar.setVisibility(View.GONE);
                ImageHelper.loadAvatar(null, imgAvatar, post.userName);
            }

            imgAvatar.setOnClickListener(onAvatarClicked);
            txtAvatar.setOnClickListener(onAvatarClicked);
            name.setOnClickListener(onAvatarClicked);

            name.setText(post.userName);
            time.setText(post.time);

            // LOGIC HIỂN THỊ TIÊU ĐỀ: Nếu rỗng thì ẩn hẳn đi để không bị trống một mảng trắng
            if (post.title != null && !post.title.isEmpty()) {
                title.setVisibility(View.VISIBLE);
                title.setText(post.title);
            } else {
                title.setVisibility(View.GONE);
            }

            content.setText(colorizeHashtags(post.description));
            content.setVisibility(post.description == null || post.description.trim().isEmpty() ? View.GONE : View.VISIBLE);
            content.setMaxLines(isDetailMode ? Integer.MAX_VALUE : (expanded ? Integer.MAX_VALUE : 3));

            // Xử lý nút Xem thêm...
            bindSeeMore(seeMore, content, post.getId(), post.expandable, expanded, this);

            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(post);
            });

            if (post.hasImage && post.imageUrl != null && !post.imageUrl.isEmpty()) {
                imagePlaceholder.setVisibility(View.VISIBLE);
                imgPostImage.setVisibility(View.VISIBLE);
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
                decorIcon.setVisibility(View.VISIBLE);
                decorCaption.setVisibility(View.VISIBLE);
            }

            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            if(tvCommentCount != null) tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            if(tvShareCount != null) tvShareCount.setText(String.valueOf(post.getShareCount()));

            if (post.isLiked()) {
                likedItemIds.add(post.getId());
            } else {
                likedItemIds.remove(post.getId());
            }

            boolean currentlyLiked = likedItemIds.contains(post.getId());
            applyLikeState(imgLikeHeart, tvLikeCount, currentlyLiked);
            applyCommentState(imgCommentIcon, tvCommentCount, post.isCommented());
            applyShareState(imgShareIcon, tvShareCount, post.isShared());

            btnLike.setOnClickListener(v -> handleLikeClick(post, imgLikeHeart, tvLikeCount));
            if(btnComment != null && imgCommentIcon != null) btnComment.setOnClickListener(v -> handleCommentClick(post, imgCommentIcon, tvCommentCount));
            if(btnShare != null && imgShareIcon != null) btnShare.setOnClickListener(v -> handleShareClick(post, imgShareIcon, tvShareCount));
        }
    }
    // =========================================================================
    // MILESTONE POST VIEWHOLDER
    // =========================================================================
    class MilestoneViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar, imgLikeHeart, imgCommentIcon, imgShareIcon;
        private final TextView txtAvatar, name, time, icon, title, description, month, amount, tvLikeCount;
        private final TextView tvCommentCount, tvShareCount;
        private final ProgressBar pbProgress;
        private final View goalPanel, btnLike, btnComment, btnShare;
        private final ImageButton menuButton;
        private final TextView seeMore;
        private final ShineAnimationHelper shineHelper;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgMilestoneAvatar);
            txtAvatar = itemView.findViewById(R.id.txtMilestoneAvatar);
            name = itemView.findViewById(R.id.txtMilestoneUserName);
            time = itemView.findViewById(R.id.txtPostTime);
            icon = itemView.findViewById(R.id.txtMilestoneIcon);
            title = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            month = itemView.findViewById(R.id.txtMilestoneMonth);
            amount = itemView.findViewById(R.id.txtMilestoneAmount);
            pbProgress = itemView.findViewById(R.id.pbMilestoneProgress);
            goalPanel = itemView.findViewById(R.id.layoutMilestoneGoalPanel);
            menuButton = itemView.findViewById(R.id.btnMilestoneMenu);
            seeMore = itemView.findViewById(R.id.txtMilestoneSeeMore);

            btnLike = itemView.findViewById(R.id.btnMilestoneLike);
            imgLikeHeart = itemView.findViewById(R.id.imgLikeHeart);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);

            btnComment = itemView.findViewById(R.id.btnMilestoneComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            imgCommentIcon = itemView.findViewById(R.id.imgCommentIcon);

            btnShare = itemView.findViewById(R.id.btnMilestoneShare);
            tvShareCount = itemView.findViewById(R.id.tvShareCount);
            imgShareIcon = itemView.findViewById(R.id.imgShareIcon);

            View shineView = itemView.findViewById(R.id.viewMilestoneShine);
            shineHelper = new ShineAnimationHelper(shineView, itemView);
        }

        void bind(FeedItem.MilestonePost milestone, boolean expanded) {
            if (!isDetailMode) {
                itemView.setOnClickListener(v -> { if (postClickListener != null) postClickListener.onPostClick(milestone); });
            } else {
                itemView.setOnClickListener(null);
            }

            icon.setText(milestone.iconText != null ? milestone.iconText : "🏆");
            name.setText(milestone.userName);
            time.setText(milestone.time);
            month.setText(milestone.month);
            title.setText(milestone.title);
            description.setText(colorizeHashtags(milestone.description));

            description.setVisibility(milestone.description == null || milestone.description.trim().isEmpty() ? View.GONE : View.VISIBLE);

            // Ép buộc hiển thị vô hạn dòng nếu ở Detail Mode
            description.setMaxLines(isDetailMode ? Integer.MAX_VALUE : (expanded ? Integer.MAX_VALUE : 3));
            amount.setText(milestone.amount);

            if (pbProgress != null) {
                if (milestone.progress <= 0) {
                    pbProgress.setVisibility(View.GONE);
                } else {
                    pbProgress.setVisibility(View.VISIBLE);
                    pbProgress.setProgress(milestone.progress);
                }
            }

            goalPanel.setVisibility(hasMeaningfulMilestoneAmount(milestone.amount) ? View.VISIBLE : View.GONE);
            bindSeeMore(seeMore, description, milestone.getId(), milestone.expandable, expanded, this);

            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(milestone);
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

            tvLikeCount.setText(String.valueOf(milestone.getLikeCount()));
            if(tvCommentCount != null) tvCommentCount.setText(String.valueOf(milestone.getCommentCount()));
            if(tvShareCount != null) tvShareCount.setText(String.valueOf(milestone.getShareCount()));

            if (milestone.isLiked()) {
                likedItemIds.add(milestone.getId());
            } else {
                likedItemIds.remove(milestone.getId());
            }

            boolean currentlyLiked = likedItemIds.contains(milestone.getId());
            applyLikeState(imgLikeHeart, tvLikeCount, currentlyLiked);
            applyCommentState(imgCommentIcon, tvCommentCount, milestone.isCommented());
            applyShareState(imgShareIcon, tvShareCount, milestone.isShared());

            btnLike.setOnClickListener(v -> handleLikeClick(milestone, imgLikeHeart, tvLikeCount));
            if(btnComment != null && imgCommentIcon != null) btnComment.setOnClickListener(v -> handleCommentClick(milestone, imgCommentIcon, tvCommentCount));
            if(btnShare != null && imgShareIcon != null) btnShare.setOnClickListener(v -> handleShareClick(milestone, imgShareIcon, tvShareCount));

            shineHelper.start();
        }

        void stopAnimation() {
            shineHelper.stop();
        }

        private boolean hasMeaningfulMilestoneAmount(String value) {
            if (value == null) return false;
            String clean = value.trim();
            return !clean.isEmpty() && !clean.equals("100%") && !clean.startsWith("http://") && !clean.startsWith("https://");
        }
    }

    private void notifyPostClick(FeedItem item) {
        if (!isDetailMode && postClickListener != null) postClickListener.onPostClick(item);
    }

    private void bindSeeMore(TextView button, TextView content, String itemId, boolean expandable, boolean expanded, RecyclerView.ViewHolder holder) {
        // Nếu ở màn Detail Mode -> Giấu nút "Xem thêm..." vô điều kiện
        if (isDetailMode || !expandable) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        button.setVisibility(View.VISIBLE);
        button.setText(expanded ? "Show less" : "See more...");
        button.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                toggleExpanded(itemId, position);
            }
        });
    }

    private android.text.SpannableString colorizeHashtags(String text) {
        if (text == null) return new android.text.SpannableString("");
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)#[\\p{L}\\p{N}_]+").matcher(text);
        
        String[] pastelColors = new String[] { "#F3B61F", "#FF6B6B", "#4BA3E3", "#6BCB77" };
        int colorIndex = 0;
        
        while (matcher.find()) {
            int color = android.graphics.Color.parseColor(pastelColors[colorIndex % pastelColors.length]);
            colorIndex++;
            spannable.setSpan(new android.text.style.ForegroundColorSpan(color), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
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