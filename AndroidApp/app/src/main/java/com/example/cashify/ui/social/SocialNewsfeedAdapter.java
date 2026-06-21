package com.example.cashify.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.cashify.data.model.Comment;
import com.example.cashify.utils.HeartAnimation;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ShineAnimationHelper;

import java.util.HashSet;
import java.util.List;
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

    public interface OnCommentFetchListener {
        void onFetchComments(String postId);
    }

    private OnCommentFetchListener commentFetchListener;

    public void setOnCommentFetchListener(OnCommentFetchListener listener) {
        this.commentFetchListener = listener;
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

    private void handleLikeClick(FeedItem post, ImageView imgLikeHeart, TextView tvLikeLabel, TextView tvEngagement) {
        boolean nowLiked = !likedItemIds.contains(post.getId());

        if (nowLiked) {
            likedItemIds.add(post.getId());
            post.setLikeCount(post.getLikeCount() + 1);
            HeartAnimation.playRubberBand(imgLikeHeart);
            
            // Send Notification
            if (post.getUserId() != null) {
                String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Someone";
                com.example.cashify.data.remote.FirebaseManager.getInstance().sendSocialNotification(
                        post.getUserId(),
                        "SOCIAL_LIKE",
                        "New Like \u2764\uFE0F",
                        currentUser + " liked your post",
                        post.getId()
                );
            }
        } else {
            likedItemIds.remove(post.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        }

        if (tvEngagement != null) {
            String likeFormatted = formatCount(post.getLikeCount());
            String commentFormatted = formatCount(post.getCommentCount());
            tvEngagement.setText(String.format("%s Likes • %s Comments", likeFormatted, commentFormatted));
        }

        applyLikeState(imgLikeHeart, tvLikeLabel, nowLiked);

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
                    if (tvEngagement != null) {
                        String likeFormatted = formatCount(post.getLikeCount());
                        String commentFormatted = formatCount(post.getCommentCount());
                        tvEngagement.setText(String.format("%s Likes • %s Comments", likeFormatted, commentFormatted));
                    }
                    applyLikeState(imgLikeHeart, tvLikeLabel, !nowLiked);
                }
            });
        }
    }
    private void handleShareClick(FeedItem milestone, View btnShare, TextView tvShareSummary) {
        android.content.Context context = btnShare.getContext();
        HeartAnimation.playRubberBand(btnShare);

        milestone.setShareCount(milestone.getShareCount() + 1);
        if (tvShareSummary != null) {
            String shareFormatted = formatCount(milestone.getShareCount());
            tvShareSummary.setText(String.format("%s Shares", shareFormatted));
        }

        if (milestone.getUserId() != null) {
            String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Someone";
            com.example.cashify.data.remote.FirebaseManager.getInstance().sendSocialNotification(
                    milestone.getUserId(),
                    "SOCIAL_SHARE",
                    "New Share \uD83D\uDD04",
                    currentUser + " shared your post",
                    milestone.getId()
            );
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        View sheetView = android.view.LayoutInflater.from(context).inflate(R.layout.bottom_sheet_share, null);

        // Zalo
        sheetView.findViewById(R.id.btnShareZalo).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Mở ứng dụng Zalo...", Toast.LENGTH_SHORT).show();
        });

        // Messenger
        sheetView.findViewById(R.id.btnShareMessenger).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Mở ứng dụng Messenger...", Toast.LENGTH_SHORT).show();
        });

        // Facebook
        sheetView.findViewById(R.id.btnShareFacebook).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "Mở ứng dụng Facebook...", Toast.LENGTH_SHORT).show();
        });

        // Copy Link
        sheetView.findViewById(R.id.btnShareCopyLink).setOnClickListener(v -> {
            dialog.dismiss();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Share Link", "https://cashify.app/post/" + milestone.getId());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Đã sao chép liên kết", Toast.LENGTH_SHORT).show();
        });

        // More Options (System Intent)
        sheetView.findViewById(R.id.btnShareMore).setOnClickListener(v -> {
            dialog.dismiss();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, "Share this post");

            String shareBody = "Check out this post on Cashify!";
            if (milestone instanceof FeedItem.NormalPost) {
                FeedItem.NormalPost normalPost = (FeedItem.NormalPost) milestone;
                String titlePart = (normalPost.title != null && !normalPost.title.isEmpty()) ? normalPost.title + "\n" : "";
                shareBody = normalPost.userName + " posted on Cashify:\n\n" + titlePart + normalPost.description;
            } else if (milestone instanceof FeedItem.MilestonePost) {
                FeedItem.MilestonePost milestonePost = (FeedItem.MilestonePost) milestone;
                String iconPart = (milestonePost.iconText != null) ? milestonePost.iconText + " " : "";
                shareBody = milestonePost.userName + " just achieved a milestone on Cashify!\n\n" + iconPart + milestonePost.title;
            }
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Cashify Post");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    // =========================================================================
    // NORMAL POST VIEWHOLDER
    // =========================================================================
    class NormalPostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar, imgPostImage, decorIcon, imgLikeHeart;
        private final TextView txtAvatar, name, time, title, content, seeMore, decorCaption, tvLikeLabel, tvEngagementSummary, tvShareSummary;
        private final View imagePlaceholder, btnLike, btnComment, btnShare;
        private final View layoutInlineComments;
        private final EditText editInlineComment;
        private final View btnSendInlineComment;
        private final ImageButton menuButton;
        private final LinearLayout layoutCommentPreviewList;
        private final TextView btnViewAllComments;

        NormalPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            name = itemView.findViewById(R.id.txtUserName);
            time = itemView.findViewById(R.id.txtPostTime);

            // LOGIC TỰ ĐỘNG CHUYỂN ĐỔI ẢNH THEO TRẠNG THÁI GIAO DỊCH
            // imagePlaceholder không còn trong item_post_normal (ui-consistency)
            imagePlaceholder = null;
            imgPostImage = null;
            decorIcon = null;
            decorCaption = null;

            title = itemView.findViewById(R.id.txtPostTitle);
            content = itemView.findViewById(R.id.txtPostContent);
            seeMore = itemView.findViewById(R.id.txtSeeMore);

            menuButton = itemView.findViewById(R.id.btnPostMenu);

            btnLike = itemView.findViewById(R.id.btnLike);
            imgLikeHeart = itemView.findViewById(R.id.imgLikeHeart);
            tvLikeLabel = itemView.findViewById(R.id.tvLikeLabel);
            tvEngagementSummary = itemView.findViewById(R.id.tvPostEngagementSummary);
            tvShareSummary = itemView.findViewById(R.id.tvPostShareSummary);

            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);

            layoutInlineComments = itemView.findViewById(R.id.layoutInlineComments);
            editInlineComment = itemView.findViewById(R.id.editInlineComment);
            btnSendInlineComment = itemView.findViewById(R.id.btnSendInlineComment);
            layoutCommentPreviewList = itemView.findViewById(R.id.layoutCommentPreviewList);
            btnViewAllComments = itemView.findViewById(R.id.btnViewAllComments);
        }

        void bind(FeedItem.NormalPost post, boolean expanded) {
            itemView.setOnClickListener(null);
            itemView.setClickable(false);
            itemView.setFocusable(false);
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) itemView).setRippleColorResource(android.R.color.transparent);
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

            if (name != null) name.setText(post.userName);
            if (time != null) time.setText(post.time);

            // LOGIC HIỂN THỊ TIÊU ĐỀ
            if (title != null) {
                if (post.title != null && !post.title.trim().isEmpty()) {
                    title.setVisibility(View.VISIBLE);
                    title.setText(post.title);
                } else {
                    title.setVisibility(View.GONE);
                }
            }

            // HIỂN THỊ NỘI DUNG
            if (content != null) {
                if (post.description != null && !post.description.trim().isEmpty()) {
                    content.setVisibility(View.VISIBLE);
                    content.setText(formatHashtags(post.description));
                    content.setMaxLines(isDetailMode ? Integer.MAX_VALUE : (expanded ? Integer.MAX_VALUE : 3));
                } else {
                    content.setVisibility(View.GONE);
                }
            }

            // Xử lý nút Xem thêm...
            bindSeeMore(seeMore, content, post.getId(), post.expandable, expanded, this);

            menuButton.setOnClickListener(v -> {
                if (menuClickListener != null) menuClickListener.onMenuClick(post);
            });

            if (tvEngagementSummary != null) {
                String likeFormatted = formatCount(post.getLikeCount());
                String commentFormatted = formatCount(post.getCommentCount());
                tvEngagementSummary.setText(String.format("%s Likes • %s Comments", likeFormatted, commentFormatted));
            }

            if (tvShareSummary != null) {
                String shareFormatted = formatCount(post.getShareCount());
                tvShareSummary.setText(String.format("%s Shares", shareFormatted));
            }

            if (post.isLiked()) {
                likedItemIds.add(post.getId());
            } else {
                likedItemIds.remove(post.getId());
            }

            boolean currentlyLiked = likedItemIds.contains(post.getId());
            applyLikeState(imgLikeHeart, tvLikeLabel, currentlyLiked);

            if (btnLike != null) {
                btnLike.setOnClickListener(v -> {
                    HeartAnimation.playRubberBand(btnLike);
                    handleLikeClick(post, imgLikeHeart, tvLikeLabel, tvEngagementSummary);
                });
            }

            bindInlineComments(post, layoutInlineComments, layoutCommentPreviewList, btnViewAllComments, itemView, editInlineComment, btnComment, btnSendInlineComment, tvEngagementSummary);
        }
    }
    // =========================================================================
    // MILESTONE POST VIEWHOLDER
    // =========================================================================
    class MilestoneViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar, imgLikeHeart;
        private final TextView txtAvatar, name, time, icon, title, description, month, amount, tvLikeLabel, tvEngagementSummary, tvShareSummary;
        private final ProgressBar pbProgress;
        private final View goalPanel, btnLike, btnComment, btnShare;
        private final View layoutInlineComments;
        private final EditText editInlineComment;
        private final View btnSendInlineComment;
        private final ImageButton menuButton;
        private final TextView seeMore;
        private final ShineAnimationHelper shineHelper;
        private final LinearLayout layoutCommentPreviewList;
        private final TextView btnViewAllComments;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgMilestoneAvatar);
            txtAvatar = itemView.findViewById(R.id.txtMilestoneAvatar);
            name = itemView.findViewById(R.id.txtMilestoneUserName);
            time = itemView.findViewById(R.id.txtPostTime);
            icon = itemView.findViewById(R.id.txtMilestoneIcon);
            title = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            month = null;
            amount = itemView.findViewById(R.id.txtMilestoneAmount);
            pbProgress = null;
            goalPanel = itemView.findViewById(R.id.layoutMilestoneGoalPanel);
            seeMore = itemView.findViewById(R.id.txtMilestoneSeeMore);

            menuButton = itemView.findViewById(R.id.btnMilestoneMenu);

            btnLike = itemView.findViewById(R.id.btnMilestoneLike);
            imgLikeHeart = itemView.findViewById(R.id.imgLikeHeart);
            tvLikeLabel = itemView.findViewById(R.id.tvLikeLabel);
            tvEngagementSummary = itemView.findViewById(R.id.tvPostEngagementSummary);
            tvShareSummary = itemView.findViewById(R.id.tvMilestoneShareSummary);

            btnComment = itemView.findViewById(R.id.btnMilestoneComment);
            btnShare = itemView.findViewById(R.id.btnMilestoneShare);

            layoutInlineComments = itemView.findViewById(R.id.layoutMilestoneInlineComments);
            editInlineComment = itemView.findViewById(R.id.editMilestoneInlineComment);
            btnSendInlineComment = itemView.findViewById(R.id.btnMilestoneSendInlineComment);
            layoutCommentPreviewList = itemView.findViewById(R.id.layoutMilestoneCommentPreviewList);
            btnViewAllComments = itemView.findViewById(R.id.btnMilestoneViewAllComments);

            View shineView = itemView.findViewById(R.id.viewMilestoneShine);
            shineHelper = new ShineAnimationHelper(shineView, itemView);
        }

        void bind(FeedItem.MilestonePost milestone, boolean expanded) {
            itemView.setOnClickListener(null);
            itemView.setClickable(false);
            itemView.setFocusable(false);
            if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) itemView).setRippleColorResource(android.R.color.transparent);
            }

            icon.setText(milestone.iconText != null ? milestone.iconText : "🏆");
            name.setText(milestone.userName);
            if (time != null) time.setText(milestone.time);
            if (month != null) month.setText(milestone.month);
            if (title != null) title.setText(milestone.title);
            description.setText(formatHashtags(milestone.description));

            description.setVisibility(milestone.description == null || milestone.description.trim().isEmpty() ? View.GONE : View.VISIBLE);
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

            if (tvEngagementSummary != null) {
                String likeFormatted = formatCount(milestone.getLikeCount());
                String commentFormatted = formatCount(milestone.getCommentCount());
                tvEngagementSummary.setText(String.format("%s Likes • %s Comments", likeFormatted, commentFormatted));
            }

            if (tvShareSummary != null) {
                String shareFormatted = formatCount(milestone.getShareCount());
                tvShareSummary.setText(String.format("%s Shares", shareFormatted));
            }

            if (milestone.isLiked()) {
                likedItemIds.add(milestone.getId());
            } else {
                likedItemIds.remove(milestone.getId());
            }

            boolean currentlyLiked = likedItemIds.contains(milestone.getId());
            applyLikeState(imgLikeHeart, tvLikeLabel, currentlyLiked);

            if (btnLike != null) {
                btnLike.setOnClickListener(v -> {
                    HeartAnimation.playRubberBand(btnLike);
                    handleLikeClick(milestone, imgLikeHeart, tvLikeLabel, tvEngagementSummary);
                });
            }

            if (btnShare != null) {
                btnShare.setOnClickListener(v -> handleShareClick(milestone, btnShare, tvShareSummary));
            }

            bindInlineComments(milestone, layoutInlineComments, layoutCommentPreviewList, btnViewAllComments, itemView, editInlineComment, btnComment, btnSendInlineComment, tvEngagementSummary);
            
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
                FeedItem item = getCurrentList().get(position);
                if (postClickListener != null) postClickListener.onPostClick(item);
            }
        });
    }

    private String formatCount(int count) {
        if (count >= 1000000) return String.format(java.util.Locale.US, "%.1fM", count / 1000000.0).replace(".0M", "M");
        if (count >= 1000) return String.format(java.util.Locale.US, "%.1fK", count / 1000.0).replace(".0K", "K");
        return String.valueOf(count);
    }

    private CharSequence formatHashtags(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#[A-Za-z0-9_]+").matcher(text);
        while (matcher.find()) {
            String hashtag = text.substring(matcher.start(), matcher.end());
            int color;
            if ("#Saving".equalsIgnoreCase(hashtag)) color = android.graphics.Color.parseColor("#66BB6A"); // Pastel Green
            else if ("#Debt".equalsIgnoreCase(hashtag)) color = android.graphics.Color.parseColor("#EF5350"); // Pastel Red
            else if ("#Investing".equalsIgnoreCase(hashtag)) color = android.graphics.Color.parseColor("#42A5F5"); // Pastel Blue
            else color = android.graphics.Color.parseColor("#FFCA28"); // Pastel Yellow
            
            spannable.setSpan(new android.text.style.ForegroundColorSpan(color),
                    matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.RelativeSizeSpan(1.15f),
                    matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    private void bindInlineComments(FeedItem post, View layoutInlineComments, LinearLayout layoutCommentPreviewList, TextView btnViewAllComments, View itemView, EditText editInlineComment, View btnComment, View btnSendInlineComment, TextView tvEngagementSummary) {
        if (btnComment != null) {
            btnComment.setOnClickListener(v -> {
                if (!isDetailMode) {
                    HeartAnimation.playRubberBand(btnComment);
                    if (post.isPreviewCommentsLoaded()) {
                        post.setCommentExpanded(!post.isCommentExpanded());
                        int position = getCurrentList().indexOf(post);
                        if (position >= 0) {
                            notifyItemChanged(position);
                        }
                        if (post.isCommentExpanded() && editInlineComment != null) {
                            editInlineComment.requestFocus();
                        }
                    } else {
                        // Notify listener to fetch comments. View model will expand it when done.
                        if (commentFetchListener != null) {
                            commentFetchListener.onFetchComments(post.getId());
                        }
                        // Optionally show a loading toast if needed, but the UI is usually fast
                    }
                }
            });
        }
        
        if (btnSendInlineComment != null) {
            btnSendInlineComment.setOnClickListener(v -> {
                if (editInlineComment != null) {
                    String text = editInlineComment.getText().toString().trim();
                    if (text.isEmpty()) return;
                    
                    editInlineComment.setText("");
                    if (layoutInlineComments != null) layoutInlineComments.setVisibility(View.GONE);
                    Toast.makeText(itemView.getContext(), "Comment posted", Toast.LENGTH_SHORT).show();
                    
                    // Increment Comment count visually
                    post.setCommentCount(post.getCommentCount() + 1);
                    if (tvEngagementSummary != null) {
                        String likeFormatted = formatCount(post.getLikeCount());
                        String commentFormatted = formatCount(post.getCommentCount());
                        tvEngagementSummary.setText(String.format("%s Likes • %s Comments", likeFormatted, commentFormatted));
                    }
                    
                    // Send Notification
                    if (post.getUserId() != null) {
                        String currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Someone";
                        com.example.cashify.data.remote.FirebaseManager.getInstance().sendSocialNotification(
                                post.getUserId(),
                                "SOCIAL_COMMENT",
                                "New Comment \uD83D\uDCAC",
                                currentUser + " commented: " + text,
                                post.getId()
                        );
                    }
                }
            });
        }
        
        if (layoutInlineComments != null) {
            if (isDetailMode) {
                layoutInlineComments.setVisibility(View.GONE);
            } else {
                layoutInlineComments.setVisibility(post.isCommentExpanded() ? View.VISIBLE : View.GONE);
            }
        }
        
        if (layoutCommentPreviewList != null) {
            layoutCommentPreviewList.removeAllViews();
            List<Comment> previewComments = post.getPreviewComments();
            if (previewComments != null && !previewComments.isEmpty() && !isDetailMode && post.isCommentExpanded()) {
                int count = 0;
                for (Comment c : previewComments) {
                    if (count >= 5) break;
                    View commentView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_comment, layoutCommentPreviewList, false);
                    TextView tvUsername = commentView.findViewById(R.id.tvCommentUsername);
                    TextView tvContent = commentView.findViewById(R.id.tvCommentContent);
                    TextView tvTime = commentView.findViewById(R.id.tvCommentTime);
                    View menu = commentView.findViewById(R.id.imgCommentMenu);
                    ImageView imgAvatar = commentView.findViewById(R.id.imgCommentAvatar);
                    
                    if (tvUsername != null) tvUsername.setText(c.getUsername());
                    if (tvContent != null) tvContent.setText(c.getContent());
                    if (tvTime != null) tvTime.setText(c.getTime());
                    if (menu != null) menu.setVisibility(View.GONE);
                    if (imgAvatar != null) {
                        ImageHelper.loadAvatar(c.getAvatarUrl(), imgAvatar, c.getUsername());
                    }
                    layoutCommentPreviewList.addView(commentView);
                    count++;
                }
            }
        }
        
        if (btnViewAllComments != null) {
            if (!isDetailMode && post.isCommentExpanded() && post.getCommentCount() > 5) {
                btnViewAllComments.setVisibility(View.VISIBLE);
                btnViewAllComments.setOnClickListener(v -> {
                    if (postClickListener != null) postClickListener.onPostClick(post);
                });
            } else {
                btnViewAllComments.setVisibility(View.GONE);
            }
        }
    }
}