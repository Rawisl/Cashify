package com.example.cashify.ui.social;

import com.example.cashify.data.model.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private String id;
    private String userId; // Mandatory for Edit/Delete permission validation
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private boolean isLiked;
    private boolean isCommentExpanded = false;
    private boolean isPreviewCommentsLoaded = false;
    private List<Comment> previewComments = new ArrayList<>();

    protected FeedItem(String id, String userId) {
        this.id = id;
        this.userId = userId != null ? userId : "";
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public int getShareCount() { return shareCount; }
    public boolean isLiked() { return isLiked; }
    public boolean isCommentExpanded() { return isCommentExpanded; }
    public boolean isPreviewCommentsLoaded() { return isPreviewCommentsLoaded; }
    public List<Comment> getPreviewComments() { return previewComments; }

    // --- Setters ---
    public void setId(String id) { this.id = id; } // Cho phép gán ID dễ dàng ở PostDetailActivity
    public void setUserId(String userId) { this.userId = userId != null ? userId : ""; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public void setCommentExpanded(boolean commentExpanded) { isCommentExpanded = commentExpanded; }
    public void setPreviewCommentsLoaded(boolean previewCommentsLoaded) { isPreviewCommentsLoaded = previewCommentsLoaded; }
    public void setPreviewComments(List<Comment> previewComments) { this.previewComments = previewComments; }

    public abstract int getType();

    // =========================================================================
    // CLONING
    // =========================================================================
    public FeedItem cloneItem() {
        FeedItem cloned;
        if (this instanceof NormalPost) {
            NormalPost np = (NormalPost) this;
            cloned = new NormalPost(np.getId(), np.getUserId(), np.userName, np.time, np.title, np.description, np.hasImage, np.imageUrl, np.initials, np.expandable, np.avatarUrl);
        } else {
            MilestonePost mp = (MilestonePost) this;
            cloned = new MilestonePost(mp.getId(), mp.getUserId(), mp.userName, mp.time, mp.title, mp.description, mp.month, mp.amount, mp.iconText, mp.progress, mp.expandable, mp.milestoneJson, mp.avatarUrl, mp.initials);
        }
        cloned.setLikeCount(this.getLikeCount());
        cloned.setCommentCount(this.getCommentCount());
        cloned.setShareCount(this.getShareCount());
        cloned.setLiked(this.isLiked());
        cloned.setCommentExpanded(this.isCommentExpanded());
        cloned.setPreviewCommentsLoaded(this.isPreviewCommentsLoaded());
        cloned.setPreviewComments(new ArrayList<>(this.getPreviewComments()));
        return cloned;
    }

    // =========================================================================
    // EQUALS & HASHCODE
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedItem feedItem = (FeedItem) o;

        // BẮT BUỘC PHẢI SO SÁNH CẢ TRẠNG THÁI TƯƠNG TÁC
        return likeCount == feedItem.likeCount &&
                commentCount == feedItem.commentCount &&
                shareCount == feedItem.shareCount &&
                isLiked == feedItem.isLiked &&
                isCommentExpanded == feedItem.isCommentExpanded &&
                isPreviewCommentsLoaded == feedItem.isPreviewCommentsLoaded &&
                Objects.equals(id, feedItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, likeCount, commentCount, shareCount, isLiked, isCommentExpanded, isPreviewCommentsLoaded);
    }

    // =========================================================================
    // NORMAL POST
    // =========================================================================
    public static class NormalPost extends FeedItem {
        public String userName;
        public String time;
        public String title;
        public String description;
        public boolean hasImage;
        public String imageUrl;
        public String avatarUrl;
        public String initials;
        public boolean expandable;

        public NormalPost() { super("", ""); }

        public NormalPost(String id, String userId, String userName, String time, String title, String description, boolean hasImage, String imageUrl, String initials, boolean expandable, String avatarUrl) {
            super(id, userId);
            this.userName = userName;
            this.time = time;
            this.title = title;
            this.description = description;
            this.hasImage = hasImage;
            this.imageUrl = imageUrl;
            this.initials = initials;
            this.expandable = expandable;
            this.avatarUrl = avatarUrl;
        }

        @Override
        public int getType() { return TYPE_NORMAL; }
    }

    // =========================================================================
    // MILESTONE POST
    // =========================================================================
    public static class MilestonePost extends FeedItem {
        public String userName;
        public String time;
        public String title;
        public String description;
        public String month;
        public String amount;
        public String iconText;
        public int progress;
        public boolean expandable;
        public String milestoneJson;
        public String avatarUrl;
        public String initials;

        public MilestonePost() { super("", ""); }

        public MilestonePost(String id, String userId, String userName, String time, String title, String description, String month, String amount, String iconText, int progress, boolean expandable, String milestoneJson, String avatarUrl, String initials) {
            super(id, userId);
            this.userName = userName;
            this.time = time;
            this.title = title;
            this.description = description;
            this.month = month;
            this.amount = amount;
            this.iconText = iconText;
            this.progress = progress;
            this.expandable = expandable;
            this.milestoneJson = milestoneJson;
            this.avatarUrl = avatarUrl;
            this.initials = initials;
        }

        @Override
        public int getType() { return TYPE_MILESTONE; }
    }
}