package com.example.cashify.ui.social;

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
    private boolean isCommented;
    private boolean isShared;
    
    // Additional fields for achievement evaluation
    private long timestamp;
    private String rawType = "";
    private String category = "";
    public abstract FeedItem cloneWithUpdates(int newLikeCount, int newCommentCount, boolean newIsLiked);
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
    public boolean isCommented() { return isCommented; }
    public boolean isShared() { return isShared; }
    public long getTimestamp() { return timestamp; }
    public String getRawType() { return rawType; }
    public String getCategory() { return category; }

    // --- Setters ---
    public void setId(String id) { this.id = id; } // Cho phép gán ID dễ dàng ở PostDetailActivity
    public void setUserId(String userId) { this.userId = userId != null ? userId : ""; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public void setCommented(boolean commented) { isCommented = commented; }
    public void setShared(boolean shared) { isShared = shared; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRawType(String rawType) { this.rawType = rawType != null ? rawType : ""; }
    public void setCategory(String category) { this.category = category != null ? category : ""; }

    public abstract int getType();

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
                isCommented == feedItem.isCommented &&
                isShared == feedItem.isShared &&
                Objects.equals(id, feedItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, likeCount, commentCount, shareCount, isLiked, isCommented, isShared);
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
        @Override
        public FeedItem cloneWithUpdates(int newLikeCount, int newCommentCount, boolean newIsLiked) {
            NormalPost copy = new NormalPost(
                    getId(), getUserId(), userName, time, title, description,
                    hasImage, imageUrl, initials, expandable, avatarUrl
            );
            // Copy state cũ từ class cha
            copy.setShareCount(this.getShareCount());
            copy.setCommented(this.isCommented());
            copy.setShared(this.isShared());
            copy.setRawType(this.getRawType());
            copy.setCategory(this.getCategory());
            copy.setTimestamp(this.getTimestamp());

            // Ép state mới vào
            copy.setLikeCount(newLikeCount);
            copy.setCommentCount(newCommentCount);
            copy.setLiked(newIsLiked);

            return copy;
        }
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
        @Override
        public FeedItem cloneWithUpdates(int newLikeCount, int newCommentCount, boolean newIsLiked) {
            MilestonePost copy = new MilestonePost(
                    getId(), getUserId(), userName, time, title, description,
                    month, amount, iconText, progress, expandable,
                    milestoneJson, avatarUrl, initials
            );
            // Copy state cũ từ class cha
            copy.setShareCount(this.getShareCount());
            copy.setCommented(this.isCommented());
            copy.setShared(this.isShared());
            copy.setRawType(this.getRawType());
            copy.setCategory(this.getCategory());
            copy.setTimestamp(this.getTimestamp());

            // Ép state mới vào
            copy.setLikeCount(newLikeCount);
            copy.setCommentCount(newCommentCount);
            copy.setLiked(newIsLiked);

            return copy;
        }
    }
}