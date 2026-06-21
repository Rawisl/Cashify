package com.example.cashify.ui.feed;

import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private final String id;
    private final String userId;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private boolean likedByMe;

    protected FeedItem(String id, String userId) {
        this(id, userId, 0, 0, 0, false);
    }

    protected FeedItem(String id, String userId, int likeCount, int commentCount,
                       int shareCount, boolean likedByMe) {
        this.id = id;
        this.userId = userId;
        this.likeCount = Math.max(0, likeCount);
        this.commentCount = Math.max(0, commentCount);
        this.shareCount = Math.max(0, shareCount);
        this.likedByMe = likedByMe;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getShareCount() {
        return shareCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = Math.max(0, likeCount);
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = Math.max(0, commentCount);
    }

    public void setShareCount(int shareCount) {
        this.shareCount = Math.max(0, shareCount);
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    public abstract int getType();

    public static class NormalPost extends FeedItem {
        public final String userName;
        public final String time;
        public final String text;
        public final boolean hasImage;
        public final String imageUrl;
        public final String avatarUrl;
        public final int avatarColor;
        public final String initials;
        public final boolean expandable;

        public NormalPost(String id, String userName, String time, String text,
                          boolean hasImage, String imageUrl, int avatarColor,
                          String initials, boolean expandable, String avatarUrl) {
            this(id, "", userName, time, text, hasImage, imageUrl, avatarColor,
                    initials, expandable, avatarUrl);
        }

        public NormalPost(String id, String userId, String userName, String time,
                          String text, boolean hasImage, String imageUrl,
                          int avatarColor, String initials, boolean expandable,
                          String avatarUrl) {
            this(id, userId, userName, time, text, hasImage, imageUrl, avatarColor,
                    initials, expandable, avatarUrl, 0, 0, 0, false);
        }

        public NormalPost(String id, String userId, String userName, String time,
                          String text, boolean hasImage, String imageUrl,
                          int avatarColor, String initials, boolean expandable,
                          String avatarUrl, int likeCount, int commentCount,
                          int shareCount, boolean likedByMe) {
            super(id, userId, likeCount, commentCount, shareCount, likedByMe);
            this.userName = userName;
            this.time = time;
            this.text = text;
            this.hasImage = hasImage;
            this.imageUrl = imageUrl;
            this.avatarColor = avatarColor;
            this.initials = initials;
            this.expandable = expandable;
            this.avatarUrl = avatarUrl;
        }

        @Override
        public int getType() {
            return TYPE_NORMAL;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NormalPost)) return false;
            NormalPost that = (NormalPost) o;
            return hasImage == that.hasImage
                    && expandable == that.expandable
                    && avatarColor == that.avatarColor
                    && getLikeCount() == that.getLikeCount()
                    && getCommentCount() == that.getCommentCount()
                    && getShareCount() == that.getShareCount()
                    && isLikedByMe() == that.isLikedByMe()
                    && Objects.equals(getId(), that.getId())
                    && Objects.equals(getUserId(), that.getUserId())
                    && Objects.equals(userName, that.userName)
                    && Objects.equals(time, that.time)
                    && Objects.equals(text, that.text)
                    && Objects.equals(imageUrl, that.imageUrl)
                    && Objects.equals(avatarUrl, that.avatarUrl)
                    && Objects.equals(initials, that.initials);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getUserId(), userName, time, text, hasImage,
                    imageUrl, avatarUrl, avatarColor, initials, expandable,
                    getLikeCount(), getCommentCount(), getShareCount(), isLikedByMe());
        }
    }

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

        public MilestonePost(String id, String title, String description, String month,
                             String amount, String iconText, int progress,
                             boolean expandable) {
            this(id, "", "Cashify User", "", title, description, month, amount,
                    iconText, progress, expandable, null, null, "CF");
        }

        public MilestonePost(String id, String userId, String userName, String time,
                             String title, String description, String month,
                             String amount, String iconText, int progress,
                             boolean expandable, String milestoneJson,
                             String avatarUrl, String initials) {
            this(id, userId, userName, time, title, description, month, amount,
                    iconText, progress, expandable, milestoneJson, avatarUrl, initials,
                    0, 0, 0, false);
        }

        public MilestonePost(String id, String userId, String userName, String time,
                             String title, String description, String month,
                             String amount, String iconText, int progress,
                             boolean expandable, String milestoneJson,
                             String avatarUrl, String initials, int likeCount,
                             int commentCount, int shareCount, boolean likedByMe) {
            super(id, userId, likeCount, commentCount, shareCount, likedByMe);
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
        public int getType() {
            return TYPE_MILESTONE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MilestonePost)) return false;
            MilestonePost that = (MilestonePost) o;
            return progress == that.progress
                    && expandable == that.expandable
                    && getLikeCount() == that.getLikeCount()
                    && getCommentCount() == that.getCommentCount()
                    && getShareCount() == that.getShareCount()
                    && isLikedByMe() == that.isLikedByMe()
                    && Objects.equals(getId(), that.getId())
                    && Objects.equals(getUserId(), that.getUserId())
                    && Objects.equals(userName, that.userName)
                    && Objects.equals(time, that.time)
                    && Objects.equals(title, that.title)
                    && Objects.equals(description, that.description)
                    && Objects.equals(month, that.month)
                    && Objects.equals(amount, that.amount)
                    && Objects.equals(iconText, that.iconText)
                    && Objects.equals(milestoneJson, that.milestoneJson)
                    && Objects.equals(avatarUrl, that.avatarUrl)
                    && Objects.equals(initials, that.initials);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getUserId(), userName, time, title, description,
                    month, amount, iconText, progress, expandable, milestoneJson, avatarUrl,
                    initials, getLikeCount(), getCommentCount(), getShareCount(), isLikedByMe());
        }
    }
}
