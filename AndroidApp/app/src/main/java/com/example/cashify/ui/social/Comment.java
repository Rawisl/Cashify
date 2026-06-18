package com.example.cashify.ui.social;

public class Comment {
    private String id;
    private String authorId;
    private String avatarUrl;
    private String username;
    private String content;
    private String time;
    private int likeCount;

    public Comment(String avatarUrl, String username, String content, String time) {
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.content = content;
        this.time = time;
    }

    public Comment(String id, String authorId, String avatarUrl, String username, String content, String time, int likeCount) {
        this.id = id;
        this.authorId = authorId;
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.content = content;
        this.time = time;
        this.likeCount = likeCount;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}