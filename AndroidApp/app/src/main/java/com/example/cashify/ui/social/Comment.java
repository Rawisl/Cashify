package com.example.cashify.ui.social;

public class Comment {
    private String avatarUrl;
    private String username;
    private String content;
    private String time;

    public Comment(String avatarUrl, String username, String content, String time) {
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.content = content;
        this.time = time;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
