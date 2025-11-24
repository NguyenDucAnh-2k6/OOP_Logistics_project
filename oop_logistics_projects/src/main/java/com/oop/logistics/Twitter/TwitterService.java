package com.oop.logistics.Twitter;

import com.oop.logistics.config.KeywordManager;
import com.oop.logistics.models.DisasterEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Twitter disaster data collection
 * Handles business logic for tweet processing
 */
public class TwitterService {

    private final TwitterClient twitterClient;
    private final KeywordManager keywordManager;
    private final List<String> searchKeywords;
    private final List<String> monitoredAccounts;

    /**
     * Constructor with default keyword manager
     */
    public TwitterService(String bearerToken) {
        this.twitterClient = new TwitterClient(bearerToken);
        this.keywordManager = new KeywordManager();
        this.searchKeywords = initializeDefaultKeywords();
        this.monitoredAccounts = initializeDefaultAccounts();
    }

    /**
     * Constructor with custom keyword manager
     */
    public TwitterService(String bearerToken, KeywordManager keywordManager) {
        this.twitterClient = new TwitterClient(bearerToken);
        this.keywordManager = keywordManager;
        this.searchKeywords = new ArrayList<>(keywordManager.getAllKeywords());
        this.monitoredAccounts = initializeDefaultAccounts();
    }

    /**
     * Initialize default disaster-related keywords for Twitter search
     */
    private List<String> initializeDefaultKeywords() {
        List<String> keywords = new ArrayList<>();
        
        // Vietnamese keywords
        keywords.add("lũ lụt");
        keywords.add("bão");
        keywords.add("động đất");
        keywords.add("thiên tai Việt Nam");
        keywords.add("cứu trợ");
        keywords.add("sạt lở đất");
        keywords.add("ngập lụt");
        
        // Hashtags
        keywords.add("#lũlụt");
        keywords.add("#bão");
        keywords.add("#thiêntai");
        keywords.add("#cứutrợ");
        keywords.add("#VietnamFlood");
        keywords.add("#VietnamDisaster");
        
        // English keywords
        keywords.add("Vietnam flood");
        keywords.add("Vietnam storm");
        keywords.add("Vietnam disaster relief");
        
        return keywords;
    }

    /**
     * Initialize default monitored accounts (disaster relief organizations)
     */
    private List<String> initializeDefaultAccounts() {
        List<String> accounts = new ArrayList<>();
        
        // Vietnamese disaster relief accounts (example)
        accounts.add("RedCrossVietnam");
        accounts.add("UNDP_Vietnam");
        
        return accounts;
    }

    /**
     * Fetch all disaster-related tweets
     */
    public List<TwitterPost> fetchDisasterTweets() {
        List<TwitterPost> allTweets = new ArrayList<>();

        try {
            // Search by keywords
            allTweets.addAll(twitterClient.searchMultipleKeywords(searchKeywords, 20));
            
            // Fetch from monitored accounts
            for (String username : monitoredAccounts) {
                try {
                    String userId = twitterClient.getUserIdByUsername(username);
                    if (userId != null) {
                        List<TwitterPost> userTweets = twitterClient.getUserTweets(userId, 20);
                        // Filter only disaster-related tweets
                        for (TwitterPost tweet : userTweets) {
                            if (isDisasterRelated(tweet)) {
                                allTweets.add(tweet);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching tweets from @" + username + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching disaster tweets: " + e.getMessage());
        }

        // Remove duplicates based on tweet ID
        return allTweets.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Detect disaster-related tweets from a list
     */
    public List<TwitterPost> detectDisasterTweets(List<TwitterPost> tweets) {
        return tweets.stream()
            .filter(this::isDisasterRelated)
            .collect(Collectors.toList());
    }

    /**
     * Check if a tweet is disaster-related
     */
    public boolean isDisasterRelated(TwitterPost tweet) {
        if (tweet.getText() == null) {
            return false;
        }
        return keywordManager.containsDisasterKeyword(tweet.getText());
    }

    /**
     * Extract disaster events from tweets
     */
    public List<DisasterEvent> extractDisasterEvents() {
        List<TwitterPost> disasterTweets = fetchDisasterTweets();
        return convertToDisasterEvents(disasterTweets);
    }

    /**
     * Convert tweets to DisasterEvent objects
     */
    public List<DisasterEvent> convertToDisasterEvents(List<TwitterPost> tweets) {
        return tweets.stream()
            .map(this::convertToDisasterEvent)
            .collect(Collectors.toList());
    }

    /**
     * Convert single tweet to DisasterEvent
     */
    private DisasterEvent convertToDisasterEvent(TwitterPost tweet) {
        DisasterEvent event = new DisasterEvent();
        
        event.setSourceId(tweet.getId());
        event.setDescription(tweet.getText());
        event.setTimestamp(tweet.getCreatedAt());
        event.setSource("Twitter");
        event.setEngagement(tweet.getTotalEngagement());
        
        // Set location
        event.setLocation(tweet.getBestLocation());
        
        // Detect disaster type
        event.setDisasterType(detectDisasterType(tweet.getText()));
        
        return event;
    }

    /**
     * Detect disaster type from tweet text
     */
    private String detectDisasterType(String text) {
        if (text == null) return "Unknown";
        
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("lũ") || lowerText.contains("flood") || lowerText.contains("ngập")) {
            return "Flood";
        }
        if (lowerText.contains("bão") || lowerText.contains("storm") || lowerText.contains("typhoon")) {
            return "Storm";
        }
        if (lowerText.contains("động đất") || lowerText.contains("earthquake")) {
            return "Earthquake";
        }
        if (lowerText.contains("cháy") || lowerText.contains("fire")) {
            return "Fire";
        }
        if (lowerText.contains("sạt lở") || lowerText.contains("landslide")) {
            return "Landslide";
        }
        
        return "Unknown";
    }

    /**
     * Get viral disaster tweets (high engagement)
     */
    public List<TwitterPost> getViralDisasterTweets(int minEngagement) {
        List<TwitterPost> disasterTweets = fetchDisasterTweets();
        
        return disasterTweets.stream()
            .filter(tweet -> tweet.getTotalEngagement() >= minEngagement)
            .sorted((a, b) -> Integer.compare(b.getTotalEngagement(), a.getTotalEngagement()))
            .collect(Collectors.toList());
    }

    /**
     * Search tweets by specific disaster type
     */
    public List<TwitterPost> searchByDisasterType(String disasterType) {
        List<String> typeKeywords = keywordManager.getKeywords(disasterType);
        
        if (typeKeywords.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return twitterClient.searchMultipleKeywords(typeKeywords, 50);
        } catch (Exception e) {
            System.err.println("Error searching by disaster type: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Add custom search keyword
     */
    public void addSearchKeyword(String keyword) {
        if (!searchKeywords.contains(keyword)) {
            searchKeywords.add(keyword);
        }
    }

    /**
     * Add monitored account
     */
    public void addMonitoredAccount(String username) {
        if (!monitoredAccounts.contains(username)) {
            monitoredAccounts.add(username);
        }
    }

    /**
     * Remove monitored account
     */
    public void removeMonitoredAccount(String username) {
        monitoredAccounts.remove(username);
    }

    /**
     * Get current search keywords
     */
    public List<String> getSearchKeywords() {
        return new ArrayList<>(searchKeywords);
    }

    /**
     * Get monitored accounts
     */
    public List<String> getMonitoredAccounts() {
        return new ArrayList<>(monitoredAccounts);
    }

    /**
     * Check if Twitter API is accessible
     */
    public boolean isApiAccessible() {
        return twitterClient.isApiAccessible();
    }
}