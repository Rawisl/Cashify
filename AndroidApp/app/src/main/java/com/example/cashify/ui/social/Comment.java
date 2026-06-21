package com.example.cashify.ui.social;

public class Comment {
    private String id;
    private String username;
    private String avatarUrl;
    private String content;
    private long timestamp;

    // Constructor trống (bắt buộc nếu bạn dùng Firebase hoặc một số thư viện Parse JSON)
    public Comment() {
    }

    // Constructor đầy đủ tham số
    public Comment(String id, String username, String avatarUrl, String content, long timestamp) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.timestamp = timestamp;
    }

    // --- Các hàm Getter và Setter ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}