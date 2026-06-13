package com.example.cashify.ui.feed;

import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private final String id;
    private final String userId; // Bắt buộc phải có để check quyền Sửa/Xóa
    private int likeCount;
    private int commentCount;
    private boolean isLiked;

    protected FeedItem(String id, String userId) {
        this.id = id;
        this.userId = userId != null ? userId : ""; // Chống Null
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isLiked() { return isLiked; }

    // Setters (Dùng setter cho các biến có thể thay đổi để constructor bớt dài)
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public abstract int getType();

    // =========================================================================
    // NormalPost
    // =========================================================================
    public static class NormalPost extends FeedItem {
        public final String userName;
        public final String time;
        public final String text;
        public final boolean hasImage;
        public final String imageUrl;
        public final String avatarUrl;
        public final String initials;
        public final boolean expandable;

        public NormalPost(
                String id, String userId, String userName, String time,
                String text, boolean hasImage, String imageUrl,
                String initials, boolean expandable, String avatarUrl
        ) {
            super(id, userId);
            this.userName = userName;
            this.time = time;
            this.text = text;
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
    // MilestonePost
    // =========================================================================
    public static class MilestonePost extends FeedItem {
        public final String userName;
        public final String time;
        public final String title;
        public final String description;
        public final String month;
        public final String amount;
        public final String iconText;
        public final int progress;
        public final boolean expandable;
        public final String milestoneJson;
        public final String avatarUrl;
        public final String initials;

        public MilestonePost(
                String id, String userId, String userName, String time,
                String title, String description, String month, String amount,
                String iconText, int progress, boolean expandable,
                String milestoneJson, String avatarUrl, String initials
        ) {
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