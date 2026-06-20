package com.example.cashify.data.model;

// POJO map dữ liệu tổng quan của một cuộc hội thoại 1-1
public class DirectConversation {
    private String friendUid;
    private String friendEmail;
    private String friendDisplayName;
    private String friendAvatarUrl;
    private String latestMessageText;
    private long latestMessageTimestamp;
    private int unreadCount;

    public DirectConversation() {
    }

    public String getFriendUid() { return friendUid; }
    public void setFriendUid(String friendUid) { this.friendUid = friendUid; }

    public String getFriendEmail() { return friendEmail; }
    public void setFriendEmail(String friendEmail) { this.friendEmail = friendEmail; }

    public String getFriendDisplayName() { return friendDisplayName; }
    public void setFriendDisplayName(String friendDisplayName) { this.friendDisplayName = friendDisplayName; }

    public String getFriendAvatarUrl() { return friendAvatarUrl; }
    public void setFriendAvatarUrl(String friendAvatarUrl) { this.friendAvatarUrl = friendAvatarUrl; }

    public String getLatestMessageText() { return latestMessageText; }
    public void setLatestMessageText(String latestMessageText) { this.latestMessageText = latestMessageText; }

    public long getLatestMessageTimestamp() { return latestMessageTimestamp; }
    public void setLatestMessageTimestamp(long latestMessageTimestamp) { this.latestMessageTimestamp = latestMessageTimestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    // Fallback an toàn: Ưu tiên Display Name -> Email -> Tên mặc định để tránh hiển thị khoảng trắng trên UI
    public String getNameToShow() {
        if (friendDisplayName != null && !friendDisplayName.isEmpty()) return friendDisplayName;
        if (friendEmail != null && friendEmail.contains("@")) return friendEmail.split("@")[0];
        return "Cashify User";
    }
}