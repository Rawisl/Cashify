package com.example.cashify.ui.feed;

import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private final String id;
    private final String userId;

    protected FeedItem(String id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    } // GETTER GỌN GÀNG

    public abstract int getType();

    // =========================================================================
    // NormalPost — Lớp con tĩnh cho bài viết thông thường
    // =========================================================================
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

        public NormalPost(
                String id,
                String userId,
                String userName,
                String time,
                String text,
                boolean hasImage,
                String imageUrl,
                int avatarColor,
                String initials,
                boolean expandable,
                String avatarUrl
        ) {
            super(id, userId); // ĐẨY LÊN LỚP CHA
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
            return Objects.equals(getId(), that.getId())
                    && Objects.equals(text, that.text)
                    && Objects.equals(imageUrl, that.imageUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), text, imageUrl);
        }
    }

    // =========================================================================
    // MilestonePost — Lớp con tĩnh cho bài viết cột mốc
    // =========================================================================
    public static class MilestonePost extends FeedItem {
        public final String title;
        public final String description;
        public final String month;
        public final String amount;
        public final String iconText;
        public final int progress;
        public final boolean expandable;
        public final String milestoneJson; // THÊM CHUỖI JSON ĐỂ TRUYỀN SANG MÀN HÌNH EDIT

        public MilestonePost(
                String id,
                String userId,
                String title,
                String description,
                String month,
                String amount,
                String iconText,
                int progress,
                boolean expandable,
                String milestoneJson // THÊM THAM SỐ NÀY
        ) {
            super(id, userId); // FIX: Truyền userId an toàn thông qua tham số constructor
            this.title = title;
            this.description = description;
            this.month = month;
            this.amount = amount;
            this.iconText = iconText;
            this.progress = progress;
            this.expandable = expandable;
            this.milestoneJson = milestoneJson;
        }

        @Override
        public int getType() {
            return TYPE_MILESTONE;
        }
    }
}