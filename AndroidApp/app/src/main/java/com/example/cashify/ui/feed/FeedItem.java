package com.example.cashify.ui.feed;

import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private String id;
    private String userId; // Mandatory for Edit/Delete permission validation
    private int likeCount;
    private int commentCount;
    private boolean isLiked;

    protected FeedItem(String id, String userId) {
        this.id = id;
        this.userId = userId != null ? userId : "";
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isLiked() { return isLiked; }

    // --- Setters ---
    public void setId(String id) { this.id = id; } // Cho phép gán ID dễ dàng ở PostDetailActivity
    public void setUserId(String userId) { this.userId = userId != null ? userId : ""; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public abstract int getType();

    // =========================================================================
    // EQUALS & HASHCODE (Dùng cho DiffUtil của RecyclerView)
    // =========================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedItem feedItem = (FeedItem) o;
        return Objects.equals(id, feedItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // =========================================================================
    // NORMAL POST (Đã nâng cấp có thêm Title và đổi text -> description)
    // =========================================================================
    public static class NormalPost extends FeedItem {
        public String userName;
        public String time;
        public String title;       // <-- MỚI THÊM
        public String description; // <-- ĐỔI TÊN TỪ 'text' CHO ĐỒNG BỘ
        public boolean hasImage;
        public String imageUrl;
        public String avatarUrl;
        public String initials;
        public boolean expandable;

        // Constructor rỗng
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
    // MILESTONE POST (Đã sửa lỗi Cú pháp)
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
        public String milestoneJson; // <-- JSON ĐỂ MANG ĐI EDIT
        public String avatarUrl;
        public String initials;

        // Constructor rỗng
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