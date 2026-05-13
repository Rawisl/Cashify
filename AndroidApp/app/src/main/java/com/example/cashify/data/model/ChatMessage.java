package com.example.cashify.data.model;

import com.google.firebase.firestore.PropertyName;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String text;
    private long timestamp;
    @PropertyName("isRecalled")
    private boolean recalled = false;
    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String senderAvatar, String text, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    @PropertyName("isRecalled")
    public boolean isRecalled() {
        return recalled;
    }
    @PropertyName("isRecalled")
    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }
}