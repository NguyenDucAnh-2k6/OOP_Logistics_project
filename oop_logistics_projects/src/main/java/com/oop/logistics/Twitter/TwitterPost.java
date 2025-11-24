package com.oop.logistics.Twitter;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a Twitter/X post (tweet)
 */
public class TwitterPost {

    @SerializedName("id")
    private String id;

    @SerializedName("text")
    private String text;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("lang")
    private String lang;

    // Author information
    @SerializedName("author_id")
    private String authorId;
    
    private String authorName;
    private String authorUsername;
    private String authorLocation;

    // Location information
    private String location;
    private String country;

    // Engagement metrics
    @SerializedName("retweet_count")
    private int retweetCount;

    @SerializedName("reply_count")
    private int replyCount;

    @SerializedName("like_count")
    private int likeCount;

    @SerializedName("quote_count")
    private int quoteCount;

    // Constructors
    public TwitterPost() {}

    public TwitterPost(String id, String text, String createdAt) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorLocation() { return authorLocation; }
    public void setAuthorLocation(String authorLocation) { this.authorLocation = authorLocation; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public int getRetweetCount() { return retweetCount; }
    public void setRetweetCount(int retweetCount) { this.retweetCount = retweetCount; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getQuoteCount() { return quoteCount; }
    public void setQuoteCount(int quoteCount) { this.quoteCount = quoteCount; }

    /**
     * Calculate total engagement
     */
    public int getTotalEngagement() {
        return retweetCount + replyCount + likeCount + quoteCount;
    }

    /**
     * Get best available location (tweet location or author location)
     */
    public String getBestLocation() {
        if (location != null && !location.isEmpty()) {
            return location;
        }
        if (authorLocation != null && !authorLocation.isEmpty()) {
            return authorLocation;
        }
        return null;
    }

    /**
     * Check if tweet is in Vietnamese
     */
    public boolean isVietnamese() {
        return "vi".equals(lang);
    }

    @Override
    public String toString() {
        return "TwitterPost{" +
                "id='" + id + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(50, text.length())) + "..." : null) + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", authorUsername='" + authorUsername + '\'' +
                ", location='" + location + '\'' +
                ", engagement=" + getTotalEngagement() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TwitterPost that = (TwitterPost) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}