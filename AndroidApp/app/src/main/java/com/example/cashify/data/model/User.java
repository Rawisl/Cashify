package com.example.cashify.data.model;

/**
 * Model đại diện cho người dùng hệ thống.
 * Cấu trúc này khớp chính xác với Document trong Firestore collection "users".
 */
public class User {
    private String uid;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String phoneNumber;

    // Trạng thái bạn bè (Local state, không lưu lên Firestore gốc của User)
    private int friendStatus = 0;

    public User() {}

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }

    public User(String uid, String email, String displayName, String avatarUrl, String phoneNumber) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public int getFriendStatus() { return friendStatus; }
    public void setFriendStatus(int friendStatus) { this.friendStatus = friendStatus; }

    // Fallback UI: Ưu tiên Tên hiển thị -> Tiền tố Email -> Tên mặc định
    public String getNameToShow() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "Cashify User";
    }
}