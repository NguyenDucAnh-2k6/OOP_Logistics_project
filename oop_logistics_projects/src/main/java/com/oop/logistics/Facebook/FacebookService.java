package com.oop.logistics.Facebook;

import com.oop.logistics.models.DisasterEvent;
import java.util.ArrayList;
import java.util.List;

public class FacebookService {
    
    private final FacebookClient facebookClient;
    private static final String[] DISASTER_KEYWORDS = {
        "lũ lụt", "flood", "bão", "storm", "động đất", "earthquake",
        "cháy", "fire", "sạt lở", "landslide", "thiên tai", "disaster",
        "cứu trợ", "relief", "khẩn cấp", "emergency"
    };

    public FacebookService(String accessToken) {
        this.facebookClient = new FacebookClient(accessToken);
    }

    /**
     * Fetch all posts from a Facebook page
     */
    public List<FacebookPost> fetchPagePosts(String pageId) {
        try {
            return facebookClient.getPagePosts(pageId);
        } catch (Exception e) {
            System.err.println("Error fetching posts: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Detect disaster-related posts
     */
    public List<FacebookPost> detectDisasterPosts(String pageId) {
        List<FacebookPost> allPosts = fetchPagePosts(pageId);
        List<FacebookPost> disasterPosts = new ArrayList<>();

        for (FacebookPost post : allPosts) {
            if (isDisasterRelated(post)) {
                disasterPosts.add(post);
            }
        }

        return disasterPosts;
    }

    /**
     * Check if a post is disaster-related
     */
    private boolean isDisasterRelated(FacebookPost post) {
        if (post.getMessage() == null) {
            return false;
        }

        String message = post.getMessage().toLowerCase();
        for (String keyword : DISASTER_KEYWORDS) {
            if (message.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convert disaster posts to DisasterEvent objects
     */
    public List<DisasterEvent> extractDisasterEvents(String pageId) {
        List<FacebookPost> disasterPosts = detectDisasterPosts(pageId);
        List<DisasterEvent> events = new ArrayList<>();

        for (FacebookPost post : disasterPosts) {
            DisasterEvent event = convertToDisasterEvent(post);
            events.add(event);
        }

        return events;
    }

    /**
     * Convert FacebookPost to DisasterEvent
     */
    private DisasterEvent convertToDisasterEvent(FacebookPost post) {
        DisasterEvent event = new DisasterEvent();
        event.setSourceId(post.getId());
        event.setDescription(post.getMessage());
        event.setTimestamp(post.getCreatedTime());
        event.setSource("Facebook");
        event.setEngagement(post.getLikes() + post.getComments() + post.getShares());
        
        // Extract disaster type from keywords
        event.setDisasterType(detectDisasterType(post.getMessage()));
        
        return event;
    }

    /**
     * Detect the type of disaster from message content
     */
    private String detectDisasterType(String message) {
        if (message == null) return "Unknown";
        
        String msg = message.toLowerCase();
        if (msg.contains("lũ") || msg.contains("flood")) return "Flood";
        if (msg.contains("bão") || msg.contains("storm")) return "Storm";
        if (msg.contains("động đất") || msg.contains("earthquake")) return "Earthquake";
        if (msg.contains("cháy") || msg.contains("fire")) return "Fire";
        if (msg.contains("sạt lở") || msg.contains("landslide")) return "Landslide";
        
        return "Unknown";
    }

    /**
     * Get high-engagement disaster posts (viral posts)
     */
    public List<FacebookPost> getViralDisasterPosts(String pageId, int minEngagement) {
        List<FacebookPost> disasterPosts = detectDisasterPosts(pageId);
        List<FacebookPost> viralPosts = new ArrayList<>();

        for (FacebookPost post : disasterPosts) {
            int totalEngagement = post.getLikes() + post.getComments() + post.getShares();
            if (totalEngagement >= minEngagement) {
                viralPosts.add(post);
            }
        }

        return viralPosts;
    }

    /**
     * Search posts on a page by keyword (case-insensitive).
     * Simple wrapper over fetchPagePosts that filters messages containing the keyword.
     */
    public List<FacebookPost> searchPosts(String pageId, String keyword) {
        if (keyword == null || keyword.isEmpty()) return new ArrayList<>();

        try {
            List<FacebookPost> all = fetchPagePosts(pageId);
            List<FacebookPost> results = new ArrayList<>();
            String q = keyword.toLowerCase();

            for (FacebookPost post : all) {
                if (post.getMessage() == null) continue;
                if (post.getMessage().toLowerCase().contains(q)) {
                    results.add(post);
                }
            }

            return results;
        } catch (Exception e) {
            System.err.println("Error searching posts: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}