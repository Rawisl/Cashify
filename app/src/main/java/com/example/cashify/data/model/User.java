package com.example.cashify.data.model;

/**
 * Model đại diện cho người dùng trong hệ thống.
 * Cấu trúc này phải khớp với Document trong Firestore collection "users".
 */

public class User {
    //Phải thêm các trường: avatarUrl, displayName, phoneNumber vào class này.

    private String uid;          // ID duy nhất từ Firebase Auth
    private String email;
    private String displayName;
    private String avatarUrl;    // Đường dẫn ảnh từ Firebase Storage hoặc Google
    private String phoneNumber; //nhớ cho người dùng nhập số điện thoại để sau này làm tính năng "Tìm bạn qua SĐT" cho tiện

    // ============================================================
    // TODO 1: CONSTRUCTOR KHÔNG THAM SỐ (MANDATORY)
    // - Bắt buộc phải có để Firebase Firestore có thể dùng hàm .toObject(User.class)
    // - Không được xóa hàm này dù nó để trống.
    // ============================================================
    public User() {
    }

    // ============================================================
    // TODO 2: CONSTRUCTOR ĐẦY ĐỦ THÔNG TIN
    // - Dùng khi tạo User mới lần đầu lúc Đăng ký (Register)
    // ============================================================
    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }

    // ============================================================
    // TODO 3: GETTERS & SETTERS
    // - Generate toàn bộ Getter và Setter cho các trường trên.
    // - Lưu ý: Firestore sẽ dựa vào tên Getter (ví dụ: getDisplayName)
    //   để map với field "displayName" trên Database.
    // ============================================================

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
}
