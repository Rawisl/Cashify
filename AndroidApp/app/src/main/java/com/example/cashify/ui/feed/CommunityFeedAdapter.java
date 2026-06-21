package com.example.cashify.ui.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.cashify.ui.common.AvatarImageView;
import com.example.cashify.ui.social.Comment;
import com.example.cashify.utils.ImageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityFeedAdapter extends ListAdapter<FeedItem, RecyclerView.ViewHolder> {

    private static final int COMMENT_PREVIEW_LIMIT = 3;

    private final Set<String> expandedContentIds = new HashSet<>();
    private final Set<String> expandedCommentIds = new HashSet<>();
    private final Map<String, List<Comment>> commentPreviews = new HashMap<>();

    private final OnPostClickListener postClickListener;
    private final OnPostMenuClickListener menuClickListener;
    private OnAvatarClickListener avatarClickListener;
    private OnPostInteractionListener interactionListener;

    public interface OnPostClickListener {
        void onPostClick(FeedItem item);
    }

    public interface OnAvatarClickListener {
        void onAvatarClick(String userId);
    }

    public interface OnPostMenuClickListener {
        void onMenuClick(FeedItem item);
    }

    public interface OnPostInteractionListener {
        void onLikeClicked(FeedItem item);
        void onCommentsExpanded(FeedItem item);
        void onCommentSubmitted(FeedItem item, String content);
        void onShareClicked(FeedItem item);
        void onViewAllComments(FeedItem item);
    }

    public CommunityFeedAdapter(OnPostClickListener postClickListener) {
        this(postClickListener, null);
    }

    public CommunityFeedAdapter(OnPostClickListener postClickListener, OnPostMenuClickListener menuClickListener) {
        super(DIFF_CALLBACK);
        this.postClickListener = postClickListener;
        this.menuClickListener = menuClickListener;
    }

    public void setOnAvatarClickListener(OnAvatarClickListener avatarClickListener) {
        this.avatarClickListener = avatarClickListener;
    }

    public void setOnPostInteractionListener(OnPostInteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void addLikedId(String id) {
        FeedItem item = findItem(id);
        if (item != null) {
            item.setLikedByMe(true);
        }
    }

    public void clearLikedIds() {
        for (FeedItem item : getCurrentList()) {
            item.setLikedByMe(false);
        }
    }

    public void setCommentPreview(String postId, List<Comment> comments) {
        commentPreviews.put(postId, comments == null ? new ArrayList<>() : new ArrayList<>(comments));
        int position = findPosition(postId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    public void expandComments(String postId) {
        expandedCommentIds.add(postId);
        int position = findPosition(postId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
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
            return new MilestoneViewHolder(inflater.inflate(R.layout.item_post_milestone, parent, false));
        }
        return new NormalPostViewHolder(inflater.inflate(R.layout.item_post_normal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FeedItem item = getItem(position);
        boolean contentExpanded = expandedContentIds.contains(item.getId());
        boolean commentsExpanded = expandedCommentIds.contains(item.getId());
        if (holder instanceof NormalPostViewHolder && item instanceof FeedItem.NormalPost) {
            ((NormalPostViewHolder) holder).bind((FeedItem.NormalPost) item, contentExpanded, commentsExpanded);
        } else if (holder instanceof MilestoneViewHolder && item instanceof FeedItem.MilestonePost) {
            ((MilestoneViewHolder) holder).bind((FeedItem.MilestonePost) item, contentExpanded, commentsExpanded);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof MilestoneViewHolder) {
            ((MilestoneViewHolder) holder).stopShineAnimation();
        }
    }

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
        private final TextView engagementSummary;
        private final TextView shareSummary;
        private final View btnLike;
        private final View btnComment;
        private final View btnShare;
        private final LinearLayout inlineComments;
        private final LinearLayout commentPreviewList;
        private final TextView btnViewAllComments;
        private final EditText editInlineComment;
        private final ImageButton btnSendInlineComment;

        NormalPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtAvatar = itemView.findViewById(R.id.txtAvatar);
            name = itemView.findViewById(R.id.txtUserName);
            time = itemView.findViewById(R.id.txtPostTime);
            content = itemView.findViewById(R.id.txtPostContent);
            seeMore = itemView.findViewById(R.id.txtSeeMore);
            imagePlaceholder = itemView.findViewById(R.id.postImagePlaceholder);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            decorCircle = itemView.findViewById(R.id.decorCircle);
            decorIcon = itemView.findViewById(R.id.decorIcon);
            decorCaption = itemView.findViewById(R.id.decorCaption);
            menuButton = itemView.findViewById(R.id.btnPostMenu);
            engagementSummary = itemView.findViewById(R.id.tvPostEngagementSummary);
            shareSummary = itemView.findViewById(R.id.tvPostShareSummary);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            inlineComments = itemView.findViewById(R.id.layoutInlineComments);
            commentPreviewList = itemView.findViewById(R.id.layoutCommentPreviewList);
            btnViewAllComments = itemView.findViewById(R.id.btnViewAllComments);
            editInlineComment = itemView.findViewById(R.id.editInlineComment);
            btnSendInlineComment = itemView.findViewById(R.id.btnSendInlineComment);
        }

        void bind(FeedItem.NormalPost post, boolean contentExpanded, boolean commentsExpanded) {
            bindAuthor(post, imgAvatar, txtAvatar, name);
            time.setText(post.time);
            content.setText(styleHashtags(post.text));
            content.setMaxLines(contentExpanded ? Integer.MAX_VALUE : 3);
            bindSeeMore(seeMore, post.getId(), post.expandable, contentExpanded, this);
            bindMenu(menuButton, post);
            bindNormalImage(post);
            bindActions(post, btnLike, btnComment, btnShare, engagementSummary, shareSummary);
            bindComments(post, commentsExpanded, inlineComments, commentPreviewList,
                    btnViewAllComments, editInlineComment, btnSendInlineComment);
        }

        private void bindNormalImage(FeedItem.NormalPost post) {
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
        }
    }

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
        private final TextView engagementSummary;
        private final TextView shareSummary;
        private final View btnLike;
        private final View btnComment;
        private final View btnShare;
        private final LinearLayout inlineComments;
        private final LinearLayout commentPreviewList;
        private final TextView btnViewAllComments;
        private final EditText editInlineComment;
        private final ImageButton btnSendInlineComment;
        private final View shineView;
        private AnimatorSet shineAnimator;
        private int shineLoopId = 0;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            shineView = itemView.findViewById(R.id.viewMilestoneShine);
            imgAvatar = itemView.findViewById(R.id.imgMilestoneAvatar);
            txtAvatar = itemView.findViewById(R.id.txtMilestoneAvatar);
            name = itemView.findViewById(R.id.txtMilestoneUserName);
            icon = itemView.findViewById(R.id.txtMilestoneIcon);
            title = itemView.findViewById(R.id.txtMilestoneTitle);
            description = itemView.findViewById(R.id.txtMilestoneDescription);
            seeMore = itemView.findViewById(R.id.txtMilestoneSeeMore);
            month = itemView.findViewById(R.id.txtMilestoneMonth);
            amount = itemView.findViewById(R.id.txtMilestoneAmount);
            goalPanel = itemView.findViewById(R.id.layoutMilestoneGoalPanel);
            menuButton = itemView.findViewById(R.id.btnMilestoneMenu);
            engagementSummary = itemView.findViewById(R.id.tvMilestoneEngagementSummary);
            shareSummary = itemView.findViewById(R.id.tvMilestoneShareSummary);
            btnLike = itemView.findViewById(R.id.btnMilestoneLike);
            btnComment = itemView.findViewById(R.id.btnMilestoneComment);
            btnShare = itemView.findViewById(R.id.btnMilestoneShare);
            inlineComments = itemView.findViewById(R.id.layoutMilestoneInlineComments);
            commentPreviewList = itemView.findViewById(R.id.layoutMilestoneCommentPreviewList);
            btnViewAllComments = itemView.findViewById(R.id.btnMilestoneViewAllComments);
            editInlineComment = itemView.findViewById(R.id.editMilestoneInlineComment);
            btnSendInlineComment = itemView.findViewById(R.id.btnMilestoneSendInlineComment);
        }

        void bind(FeedItem.MilestonePost milestone, boolean contentExpanded, boolean commentsExpanded) {
            startShineAnimation();
            icon.setText("");
            bindAuthor(milestone, imgAvatar, txtAvatar, name);
            title.setText(styleHashtags(milestone.title));
            description.setText(styleHashtags(milestone.description));
            description.setVisibility(milestone.description == null || milestone.description.trim().isEmpty()
                    ? View.GONE : View.VISIBLE);
            description.setMaxLines(contentExpanded ? Integer.MAX_VALUE : 3);
            month.setText(milestone.time == null || milestone.time.isEmpty() ? milestone.month : milestone.time);
            amount.setText(milestone.amount);
            goalPanel.setVisibility(hasMeaningfulMilestoneAmount(milestone.amount) ? View.VISIBLE : View.GONE);
            bindSeeMore(seeMore, milestone.getId(), milestone.expandable, contentExpanded, this);
            bindMenu(menuButton, milestone);
            bindActions(milestone, btnLike, btnComment, btnShare, engagementSummary, shareSummary);
            bindComments(milestone, commentsExpanded, inlineComments, commentPreviewList,
                    btnViewAllComments, editInlineComment, btnSendInlineComment);
        }

        void startShineAnimation() {
            if (shineView == null) return;
            stopShineAnimation();
            int loopId = ++shineLoopId;
            shineView.setAlpha(0f);
            shineView.setTranslationX(0f);
            shineView.setTranslationY(0f);
            shineView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            shineView.post(() -> {
                if (shineLoopId != loopId || shineView.getWindowToken() == null) return;
                float cardWidth = Math.max(itemView.getWidth(), 360);
                float cardHeight = Math.max(itemView.getHeight(), 260);
                float shineHeight = Math.max(shineView.getHeight(), cardHeight);
                float startX = -(cardWidth * 0.75f) - shineView.getWidth() - 80f;
                float startY = (cardHeight * 0.65f) + 140f;
                float endX = cardWidth + (shineHeight * 0.45f) + 80f;
                float endY = -cardHeight - 180f;

                shineView.setTranslationX(startX);
                shineView.setTranslationY(startY);
                ObjectAnimator moveX = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_X, startX, endX);
                ObjectAnimator moveY = ObjectAnimator.ofFloat(shineView, View.TRANSLATION_Y, startY, endY);
                ObjectAnimator opacity = ObjectAnimator.ofFloat(shineView, View.ALPHA, 0f, 0f, 0.28f, 0.28f, 0f);

                shineAnimator = new AnimatorSet();
                shineAnimator.playTogether(moveX, moveY, opacity);
                shineAnimator.setDuration(2300L);
                shineAnimator.setStartDelay(50L);
                shineAnimator.setInterpolator(new LinearInterpolator());
                shineAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (shineLoopId == loopId && shineAnimator != null && shineView.getWindowToken() != null) {
                            startShineAnimation();
                        }
                    }
                });
                shineAnimator.start();
            });
        }

        void stopShineAnimation() {
            shineLoopId++;
            if (shineAnimator != null) {
                shineAnimator.removeAllListeners();
                shineAnimator.cancel();
                shineAnimator = null;
            }
            if (shineView != null) {
                shineView.setAlpha(0f);
                shineView.setTranslationX(0f);
                shineView.setTranslationY(0f);
                shineView.clearAnimation();
            }
        }
    }

    private void bindAuthor(FeedItem item, ImageView image, TextView fallback, TextView nameView) {
        String displayName = item instanceof FeedItem.NormalPost
                ? ((FeedItem.NormalPost) item).userName
                : ((FeedItem.MilestonePost) item).userName;
        String avatarUrl = item instanceof FeedItem.NormalPost
                ? ((FeedItem.NormalPost) item).avatarUrl
                : ((FeedItem.MilestonePost) item).avatarUrl;

        image.setVisibility(View.VISIBLE);
        fallback.setVisibility(View.GONE);
        ImageHelper.loadAvatar(avatarUrl, image, displayName);
        nameView.setText(displayName);

        View.OnClickListener clickListener = v -> {
            if (avatarClickListener != null && item.getUserId() != null && !item.getUserId().trim().isEmpty()) {
                avatarClickListener.onAvatarClick(item.getUserId());
            }
        };
        image.setOnClickListener(clickListener);
        fallback.setOnClickListener(clickListener);
        nameView.setOnClickListener(clickListener);
    }

    private void bindMenu(ImageButton button, FeedItem item) {
        button.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick(item);
            }
        });
    }

    private void bindActions(FeedItem item, View likeButton, View commentButton,
                             View shareButton, TextView engagementSummary, TextView shareSummary) {
        // Each action button is now a LinearLayout containing an ImageView + TextView
        ImageView likeIcon = findImageView(likeButton);
        TextView likeLabel = findTextView(likeButton);
        ImageView commentIcon = findImageView(commentButton);
        TextView commentLabel = findTextView(commentButton);
        ImageView shareIcon = findImageView(shareButton);
        TextView shareLabel = findTextView(shareButton);

        applyLikeState(likeButton, likeIcon, likeLabel, item.isLikedByMe());
        applyActionState(commentButton, commentIcon, commentLabel, expandedCommentIds.contains(item.getId()), R.color.notification_blue);
        applyActionState(shareButton, shareIcon, shareLabel, false, R.color.notification_blue);
        likeLabel.setText(formatActionCount(item.getLikeCount(), "Like"));
        commentLabel.setText(formatActionCount(item.getCommentCount(), "Comment"));
        shareLabel.setText(formatActionCount(item.getShareCount(), "Share"));
        engagementSummary.setText(formatEngagementSummary(item));
        shareSummary.setText(item.getShareCount() > 0 ? item.getShareCount() + " shares" : "0 shares");

        likeButton.setOnClickListener(v -> {
            animateActionButton(likeButton);
            applyLikeState(likeButton, likeIcon, likeLabel, !item.isLikedByMe());
            if (interactionListener != null) {
                interactionListener.onLikeClicked(item);
            }
        });
        commentButton.setOnClickListener(v -> {
            animateActionButton(commentButton);
            expandedCommentIds.add(item.getId());
            applyActionState(commentButton, commentIcon, commentLabel, true, R.color.notification_blue);
            if (interactionListener != null) {
                interactionListener.onCommentsExpanded(item);
            }
            notifyItemChanged(findPosition(item.getId()));
        });
        shareButton.setOnClickListener(v -> {
            animateActionButton(shareButton);
            applyActionState(shareButton, shareIcon, shareLabel, true, R.color.notification_blue);
            shareButton.postDelayed(() -> applyActionState(shareButton, shareIcon, shareLabel, false, R.color.notification_blue), 450);
            if (interactionListener != null) {
                interactionListener.onShareClicked(item);
            } else {
                Toast.makeText(v.getContext(), "Post link copied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ImageView findImageView(View parent) {
        if (parent instanceof ImageView) return (ImageView) parent;
        if (parent instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) parent;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ImageView result = findImageView(vg.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private TextView findTextView(View parent) {
        if (parent instanceof TextView) return (TextView) parent;
        if (parent instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) parent;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView result = findTextView(vg.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private void bindComments(FeedItem item, boolean expanded, LinearLayout section,
                              LinearLayout list, TextView viewAll, EditText editor,
                              ImageButton sendButton) {
        section.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!expanded) {
            editor.setText("");
            return;
        }

        List<Comment> comments = commentPreviews.get(item.getId());
        renderCommentPreviews(list, comments);

        int count = item.getCommentCount();
        boolean hasMore = count > COMMENT_PREVIEW_LIMIT;
        viewAll.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        viewAll.setText("View all comments (" + count + ")");
        viewAll.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onViewAllComments(item);
            } else {
                notifyPostClick(item);
            }
        });

        sendButton.setOnClickListener(v -> {
            String content = editor.getText().toString().trim();
            if (content.isEmpty()) return;
            editor.setText("");
            if (interactionListener != null) {
                interactionListener.onCommentSubmitted(item, content);
            }
        });
    }

    private void renderCommentPreviews(LinearLayout list, List<Comment> comments) {
        list.removeAllViews();
        if (comments == null) {
            TextView loading = makeCommentStatusText(list, "Loading comments...");
            loading.setTextColor(ContextCompat.getColor(list.getContext(), R.color.item_description));
            list.addView(loading);
            return;
        }
        if (comments.isEmpty()) {
            TextView empty = makeCommentStatusText(list, "Be the first to comment.");
            empty.setTextColor(ContextCompat.getColor(list.getContext(), R.color.item_description));
            list.addView(empty);
            return;
        }

        int limit = Math.min(COMMENT_PREVIEW_LIMIT, comments.size());
        for (int i = 0; i < limit; i++) {
            Comment comment = comments.get(i);
            String name = comment.getUsername() == null || comment.getUsername().trim().isEmpty()
                    ? "Cashify User"
                    : comment.getUsername().trim();
            list.addView(makeCommentRow(list, comment, name));
        }
    }

    private TextView makeCommentStatusText(ViewGroup parent, String text) {
        TextView view = new TextView(parent.getContext());
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.item_title));
        view.setTextSize(13f);
        view.setLineSpacing(2f, 1f);
        view.setPadding(12, 8, 12, 8);
        return view;
    }

    private View makeCommentRow(ViewGroup parent, Comment comment, String name) {
        android.content.Context context = parent.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        row.setPadding(0, dp(context, 6), 0, dp(context, 6));
        row.setClipToPadding(false);

        AvatarImageView avatar = new AvatarImageView(context);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32));
        avatarParams.setMarginEnd(dp(context, 10));
        avatar.setLayoutParams(avatarParams);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setImageResource(R.drawable.bg_default_avatar_pastel);
        ImageHelper.loadAvatar(comment.getAvatarUrl(), avatar, name);
        row.addView(avatar);

        LinearLayout bubble = new LinearLayout(context);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(Color.parseColor("#F3F6FB"));
        background.setCornerRadius(dp(context, 14));
        bubble.setBackground(background);
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bubble.setLayoutParams(bubbleParams);

        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
        nameView.setTextSize(12f);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setIncludeFontPadding(false);
        bubble.addView(nameView);

        TextView contentView = new TextView(context);
        contentView.setText(comment.getContent() == null ? "" : comment.getContent().trim());
        contentView.setTextColor(ContextCompat.getColor(context, R.color.item_title));
        contentView.setTextSize(13f);
        contentView.setLineSpacing(2f, 1f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = dp(context, 3);
        contentView.setLayoutParams(contentParams);
        bubble.addView(contentView);

        String time = comment.getTime() == null ? "" : comment.getTime().trim();
        if (!time.isEmpty()) {
            TextView timeView = new TextView(context);
            timeView.setText(time);
            timeView.setTextColor(ContextCompat.getColor(context, R.color.item_description));
            timeView.setTextSize(11f);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeParams.topMargin = dp(context, 5);
            timeView.setLayoutParams(timeParams);
            bubble.addView(timeView);
        }

        row.addView(bubble);
        return row;
    }

    private int dp(android.content.Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private SpannableString styleHashtags(String text) {
        String safeText = text == null ? "" : text;
        SpannableString styled = new SpannableString(safeText);
        Matcher matcher = Pattern.compile("#[A-Za-z0-9_]+").matcher(safeText);
        while (matcher.find()) {
            String hashtag = safeText.substring(matcher.start(), matcher.end());
            styled.setSpan(
                    new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            styled.setSpan(
                    new ForegroundColorSpan(colorForHashtag(hashtag)),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return styled;
    }

    private int colorForHashtag(String hashtag) {
        String lower = hashtag.toLowerCase();
        if (lower.equals("#budget") || lower.equals("#budgeting")) {
            return Color.parseColor("#D9A43A");
        }
        if (lower.equals("#savings") || lower.equals("#saving") || lower.equals("#save")) {
            return Color.parseColor("#6FBF73");
        }
        if (lower.equals("#debt") || lower.equals("#debtfree")) {
            return Color.parseColor("#E7808E");
        }
        if (lower.equals("#investment") || lower.equals("#invest") || lower.equals("#investing")) {
            return Color.parseColor("#7FA2F2");
        }
        return Color.parseColor("#1A237E");
    }

    private void bindSeeMore(TextView button, String itemId, boolean expandable,
                             boolean expanded, RecyclerView.ViewHolder holder) {
        if (!expandable) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        button.setVisibility(View.VISIBLE);
        button.setText(expanded ? "Thu gon" : "Xem them...");
        button.setOnClickListener(v -> {
            if (expandedContentIds.contains(itemId)) {
                expandedContentIds.remove(itemId);
            } else {
                expandedContentIds.add(itemId);
            }
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        });
    }

    private void applyLikeState(View button, ImageView icon, TextView label, boolean liked) {
        int colorRes = liked ? R.color.status_red : R.color.icon_inactive;
        int textColorRes = liked ? R.color.status_red : R.color.item_description;
        int iconColor = ContextCompat.getColor(button.getContext(), colorRes);
        int textColor = ContextCompat.getColor(button.getContext(), textColorRes);
        if (icon != null) icon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        if (label != null) label.setTextColor(textColor);
    }

    private void applyActionState(View button, ImageView icon, TextView label, boolean active, int activeColorRes) {
        int colorRes = active ? activeColorRes : R.color.icon_inactive;
        int textColorRes = active ? activeColorRes : R.color.item_description;
        int iconColor = ContextCompat.getColor(button.getContext(), colorRes);
        int textColor = ContextCompat.getColor(button.getContext(), textColorRes);
        if (icon != null) icon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
        if (label != null) label.setTextColor(textColor);
    }

    private void animateActionButton(View button) {
        button.animate().cancel();
        button.setScaleX(0.92f);
        button.setScaleY(0.92f);
        button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .start();
    }

    private String formatActionCount(int count, String fallback) {
        return count > 0 ? String.valueOf(count) : fallback;
    }

    private String formatEngagementSummary(FeedItem item) {
        return item.getLikeCount() + " likes · " + item.getCommentCount() + " comments";
    }

    private boolean hasMeaningfulMilestoneAmount(String value) {
        if (value == null) return false;
        String clean = value.trim();
        return !clean.isEmpty()
                && !clean.equals("100%")
                && !clean.startsWith("http://")
                && !clean.startsWith("https://");
    }

    private void notifyPostClick(FeedItem item) {
        if (postClickListener != null) {
            postClickListener.onPostClick(item);
        }
    }

    private FeedItem findItem(String postId) {
        int position = findPosition(postId);
        return position == RecyclerView.NO_POSITION ? null : getCurrentList().get(position);
    }

    private int findPosition(String postId) {
        if (postId == null) return RecyclerView.NO_POSITION;
        List<FeedItem> current = getCurrentList();
        for (int i = 0; i < current.size(); i++) {
            if (postId.equals(current.get(i).getId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
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
