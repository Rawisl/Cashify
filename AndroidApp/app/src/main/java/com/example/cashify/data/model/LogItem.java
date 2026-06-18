package com.example.cashify.data.model;

import com.google.firebase.firestore.DocumentId;

// POJO map dữ liệu Audit Log từ Firestore
public class LogItem {
    // Tự động map Document ID của Firestore vào biến này
    @DocumentId
    private String id;

    private String userId;
    private String actionType;
    private String message;
    private long timestamp;

    // Bắt buộc có constructor rỗng để Firestore .toObject() có thể khởi tạo
    public LogItem() {}

    public LogItem(String userId, String actionType, String message, long timestamp) {
        this.userId = userId;
        this.actionType = actionType;
        this.message = message;
        this.timestamp = timestamp;
    }

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