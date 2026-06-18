package com.example.cashify.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

// POJO map dữ liệu thông báo từ Firestore
public class NotificationItem {

    // Loại trừ id khỏi JSON khi push lên Firestore, chỉ dùng ở Local
    @Exclude
    private String id;

    // Loại: "FRIEND_REQUEST", "WORKSPACE_INVITE", "WORKSPACE_TRANS", "WORKSPACE_CHAT"
    private String type;
    private String title;
    private String message;
    private long timestamp;
    private boolean isRead;

    // Reference ID dùng để điều hướng (VD: ID quỹ, ID người gửi)
    private String referenceId;

    public NotificationItem() {}

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Ép kiểu cho Firebase map đúng tên trường "isRead"
    @PropertyName("isRead")
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public void setRead(boolean read) { this.isRead = read; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
}