package com.example.mxnexus.data.model;

/**
 * Model class for Notifications in MX Nexus.
 */
public class Notification {
    private String notificationId;
    private String receiverId; // The ID of the user receiving the notification
    private String postId;     // The ID of the post related to this alert
    private String type;       // Follow, Message, Like, Comment, Mention
    private String message;    // e.g., "John liked your post"
    private long timestamp;    // Time in milliseconds

    // Required empty constructor for Firestore
    public Notification() {}

    public Notification(String notificationId, String receiverId, String postId, String type, String message, long timestamp) {
        this.notificationId = notificationId;
        this.receiverId = receiverId;
        this.postId = postId;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
