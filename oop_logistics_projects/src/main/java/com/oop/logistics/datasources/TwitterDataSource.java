package com.oop.logistics.datasources;

import com.oop.logistics.core.DataSource;
import com.oop.logistics.core.SourceConfiguration;
import com.oop.logistics.config.KeywordManager;
import com.oop.logistics.Twitter.TwitterService;
import com.oop.logistics.Twitter.TwitterPost;
import com.oop.logistics.models.DisasterEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Twitter/X implementation of DataSource interface
 * Collects disaster-related data from Twitter API v2
 */
public class TwitterDataSource implements DataSource {

    private TwitterService twitterService;
    private KeywordManager keywordManager;
    private boolean configured;
    private int defaultMaxResults;
    private int viralEngagementThreshold;

    public TwitterDataSource() {
        this.configured = false;
        this.defaultMaxResults = 100;
        this.viralEngagementThreshold = 100;
    }

    public TwitterDataSource(KeywordManager keywordManager) {
        this();
        this.keywordManager = keywordManager;
    }

    @Override
    public void configure(SourceConfiguration config) {
        String bearerToken = config.getStringProperty("bearer_token");

        if (bearerToken == null || bearerToken.isEmpty()) {
            throw new IllegalArgumentException(
                "Twitter requires 'bearer_token' in configuration"
            );
        }

        // Initialize keyword manager if not provided
        if (this.keywordManager == null) {
            this.keywordManager = new KeywordManager();
        }

        // Create Twitter service
        this.twitterService = new TwitterService(bearerToken, keywordManager);

        // Load optional configuration
        this.defaultMaxResults = config.getIntProperty("max_results", 100);
        this.viralEngagementThreshold = config.getIntProperty("viral_threshold", 100);

        // Add custom keywords if provided
        @SuppressWarnings("unchecked")
        List<String> customKeywords = (List<String>) config.getProperty("keywords");
        if (customKeywords != null) {
            for (String keyword : customKeywords) {
                twitterService.addSearchKeyword(keyword);
            }
        }

        // Add monitored accounts if provided
        @SuppressWarnings("unchecked")
        List<String> accounts = (List<String>) config.getProperty("monitored_accounts");
        if (accounts != null) {
            for (String account : accounts) {
                twitterService.addMonitoredAccount(account);
            }
        }

        // Verify API access
        if (twitterService.isApiAccessible()) {
            this.configured = true;
            System.out.println("Twitter data source configured successfully");
        } else {
            System.err.println("Warning: Twitter API may not be accessible. Check your bearer token.");
            this.configured = true; // Still mark as configured to allow retry
        }
    }

    @Override
    public List<DisasterEvent> fetchDisasterEvents() {
        if (!isAvailable()) {
            System.err.println("Twitter data source not configured");
            return new ArrayList<>();
        }

        try {
            List<DisasterEvent> events = twitterService.extractDisasterEvents();
            System.out.println("Fetched " + events.size() + " disaster events from Twitter");
            return events;
        } catch (Exception e) {
            System.err.println("Error fetching disaster events from Twitter: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<DisasterEvent> fetchViralEvents(int minEngagement) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            List<TwitterPost> viralTweets = twitterService.getViralDisasterTweets(minEngagement);
            return twitterService.convertToDisasterEvents(viralTweets);
        } catch (Exception e) {
            System.err.println("Error fetching viral events from Twitter: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String getSourceName() {
        return "Twitter";
    }

    @Override
    public boolean isAvailable() {
        return configured && twitterService != null;
    }

    /**
     * Search tweets by specific disaster type
     */
    public List<DisasterEvent> fetchByDisasterType(String disasterType) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            List<TwitterPost> tweets = twitterService.searchByDisasterType(disasterType);
            return twitterService.convertToDisasterEvents(tweets);
        } catch (Exception e) {
            System.err.println("Error fetching by disaster type: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Add custom search keyword at runtime
     */
    public void addKeyword(String keyword) {
        if (twitterService != null) {
            twitterService.addSearchKeyword(keyword);
        }
    }

    /**
     * Add monitored account at runtime
     */
    public void addMonitoredAccount(String username) {
        if (twitterService != null) {
            twitterService.addMonitoredAccount(username);
        }
    }

    /**
     * Get current configuration details
     */
    public String getConfigurationDetails() {
        if (!isAvailable()) {
            return "Not configured";
        }

        StringBuilder details = new StringBuilder();
        details.append("Twitter Data Source Configuration:\n");
        details.append("  - Default max results: ").append(defaultMaxResults).append("\n");
        details.append("  - Viral threshold: ").append(viralEngagementThreshold).append("\n");
        details.append("  - Search keywords: ").append(twitterService.getSearchKeywords().size()).append("\n");
        details.append("  - Monitored accounts: ").append(twitterService.getMonitoredAccounts()).append("\n");
        
        return details.toString();
    }

    /**
     * Test API connectivity
     */
    public boolean testConnection() {
        if (twitterService == null) {
            return false;
        }
        return twitterService.isApiAccessible();
    }

    /**
     * Get underlying Twitter service for advanced operations
     */
    public TwitterService getTwitterService() {
        return twitterService;
    }
}