package com.oop.logistics.Facebook;

import com.google.gson.annotations.SerializedName;

public class FacebookPost {
    @SerializedName("id")
    private String id;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("created_time")
    private String createdTime;
    
    // Optional but useful fields
    @SerializedName("likes")
    private int likes;
    
    @SerializedName("comments")
    private int comments;
    
    @SerializedName("shares")
    private int shares;

    // Constructors
    public FacebookPost() {}

    public FacebookPost(String id, String message, String createdTime) {
        this.id = id;
        this.message = message;
        this.createdTime = createdTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    @Override
    public String toString() {
        return "FacebookPost{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", createdTime='" + createdTime + '\'' +
                ", likes=" + likes +
                ", comments=" + comments +
                ", shares=" + shares +
                '}';
    }
    
}