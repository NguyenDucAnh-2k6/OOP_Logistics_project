// File: src/main/java/com/oop/logistics/crawler/SocialResult.java
package com.oop.logistics.crawler;

public class SocialResult {
    private String platform;
    private String author;
    private String content;
    private int likes;
    private String timestamp;

    public SocialResult(String platform, String author, String content, int likes, String timestamp) {
        this.platform = platform;
        this.author = author;
        this.content = content;
        this.likes = likes;
        this.timestamp = timestamp;
    }

    public String getPlatform() {
        return platform;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public int getLikes() {
        return likes;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
}