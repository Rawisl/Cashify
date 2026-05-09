package com.example.cashify.data.model;

import com.google.firebase.firestore.DocumentId;

public class LogItem {
    @DocumentId
    private String id;
    private String userId;
    private String actionType;
    private String message;
    private long timestamp;

    // 1. CONSTRUCTOR TRỐNG (Bắt buộc phải có để Firestore .toObject hoạt động)
    public LogItem() {
    }

    // 2. CONSTRUCTOR ĐẦY ĐỦ THAM SỐ (Để bạn dùng trong Repository)
    public LogItem(String userId, String actionType, String message, long timestamp) {
        this.userId = userId;
        this.actionType = actionType;
        this.message = message;
        this.timestamp = timestamp;
    }

    // 3. GETTERS & SETTERS (Bắt buộc phải có để Firestore đọc/ghi dữ liệu)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}