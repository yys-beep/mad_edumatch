package com.example.mad_edumatch.firebaseModels;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;

    // Required empty constructor for Firebase
    public ChatMessage() { }

    public ChatMessage(String messageId, String senderId, String receiverId, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}