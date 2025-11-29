package com.oop.logistics.Facebook;

import com.google.gson.annotations.SerializedName;

/**
 * Data model for a scraped Facebook comment.
 * It stores the raw comment text and the unparsed, localized timestamp.
 */
public class FacebookComment {
    
    private String text;
    private String createdTime; // Raw string (e.g., "Yesterday at 3:00 PM")
    private String authorName;  // Optional: for richer context
    private String postId;      // Which post this comment belongs to

    // Constructors
    public FacebookComment() {}

    public FacebookComment(String text, String createdTime, String postId) {
        this.text = text;
        this.createdTime = createdTime;
        this.postId = postId;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("created_time")
    
    
    private String fromName; // From user's name

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    @Override
    public String toString() {
        return "FacebookComment{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", createdTime='" + createdTime + '\'' +
                ", fromName='" + fromName + '\'' +
                '}';
    }
}