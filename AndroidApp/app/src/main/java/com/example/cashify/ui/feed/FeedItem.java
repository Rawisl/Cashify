package com.example.cashify.ui.feed;

import java.util.Objects;

public abstract class FeedItem {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_MILESTONE = 2;

    private final String id;

    protected FeedItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
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

        public NormalPost(
                String id,
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
            super(id);
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

    public static class MilestonePost extends FeedItem {
        public final String title;
        public final String description;
        public final String month;
        public final String amount;
        public final String iconText;
        public final int progress;
        public final boolean expandable;

        public MilestonePost(
                String id,
                String title,
                String description,
                String month,
                String amount,
                String iconText,
                int progress,
                boolean expandable
        ) {
            super(id);
            this.title = title;
            this.description = description;
            this.month = month;
            this.amount = amount;
            this.iconText = iconText;
            this.progress = progress;
            this.expandable = expandable;
        }

        @Override
        public int getType() {
            return TYPE_MILESTONE;
        }
    }
}
