package com.example.mad_edumatch.firebaseModels;

public class ChatList {
    public String id;
    public String lastMessage;
    public long timestamp;
    public boolean isSeen; // <--- NEW FIELD

    public ChatList() { }

    public ChatList(String id, String lastMessage, long timestamp, boolean isSeen) {
        this.id = id;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.isSeen = isSeen;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public boolean isSeen() { return isSeen; } // Getter
}