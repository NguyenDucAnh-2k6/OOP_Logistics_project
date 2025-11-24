package com.oop.logistics.datasources;

import com.oop.logistics.core.DataSource;
import com.oop.logistics.core.SourceConfiguration;
import com.oop.logistics.Facebook.FacebookService;
import com.oop.logistics.Facebook.FacebookPost;
import com.oop.logistics.scraping.FacebookScraperSelenium;
import com.oop.logistics.scraping.FacebookScraperJSoup;
import com.oop.logistics.models.DisasterEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Facebook DataSource with both API and Scraping support
 * Can switch between API token and web scraping based on configuration
 */
public class FacebookDataSourceWithScraping implements DataSource {

    private FacebookService facebookService;  // API-based
    private FacebookScraperSelenium seleniumScraper;  // Browser automation
    private FacebookScraperJSoup jsoupScraper;  // HTML parsing
    
    private String pageId;
    private String pageUsername;
    private boolean configured;
    private ScrapingMode mode;
    
    public enum ScrapingMode {
        API,           // Use Facebook Graph API (requires token)
        SELENIUM,      // Use Selenium browser automation
        JSOUP,         // Use JSoup HTML parsing (limited)
        AUTO           // Auto-detect best available method
    }

    public FacebookDataSourceWithScraping() {
        this.configured = false;
        this.mode = ScrapingMode.AUTO;
    }

    @Override
    public void configure(SourceConfiguration config) {
        // Determine scraping mode
        String modeStr = config.getStringProperty("mode", "AUTO");
        this.mode = ScrapingMode.valueOf(modeStr.toUpperCase());
        
        // Get page identifiers
        this.pageId = config.getStringProperty("page_id");
        this.pageUsername = config.getStringProperty("page_username");
        
        if (pageUsername == null && pageId == null) {
            throw new IllegalArgumentException(
                "Facebook requires either 'page_username' or 'page_id'"
            );
        }
        
        // Configure based on mode
        configureByMode(config);
    }

    /**
     * Configure based on selected mode
     */
    private void configureByMode(SourceConfiguration config) {
        String accessToken = config.getStringProperty("access_token");
        String email = config.getStringProperty("email");
        String password = config.getStringProperty("password");
        String cookies = config.getStringProperty("cookies");
        boolean headless = config.getIntProperty("headless", 1) == 1;
        
        switch (mode) {
            case API:
                configureApiMode(accessToken);
                break;
                
            case SELENIUM:
                configureSeleniumMode(email, password, headless);
                break;
                
            case JSOUP:
                configureJSoupMode(cookies);
                break;
                
            case AUTO:
            default:
                configureAutoMode(accessToken, email, password, cookies, headless);
                break;
        }
    }

    /**
     * Configure API mode
     */
    private void configureApiMode(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("API mode requires access_token");
        }
        
        this.facebookService = new FacebookService(accessToken);
        this.configured = true;
        this.mode = ScrapingMode.API;
        
        System.out.println("✓ Facebook configured in API mode");
    }

    /**
     * Configure Selenium mode
     */
    private void configureSeleniumMode(String email, String password, boolean headless) {
        try {
            this.seleniumScraper = new FacebookScraperSelenium(headless);
            
            // Optional login for better access
            if (email != null && password != null) {
                seleniumScraper.login(email, password);
                System.out.println("✓ Facebook configured in Selenium mode (authenticated)");
            } else {
                System.out.println("✓ Facebook configured in Selenium mode (public access)");
            }
            
            this.configured = true;
            this.mode = ScrapingMode.SELENIUM;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure Selenium: " + e.getMessage());
        }
    }

    /**
     * Configure JSoup mode
     */
    private void configureJSoupMode(String cookies) {
        this.jsoupScraper = new FacebookScraperJSoup();
        
        if (cookies != null && !cookies.isEmpty()) {
            jsoupScraper.setCookies(cookies);
            System.out.println("✓ Facebook configured in JSoup mode (with cookies)");
        } else {
            System.out.println("✓ Facebook configured in JSoup mode (public only)");
        }
        
        this.configured = true;
        this.mode = ScrapingMode.JSOUP;
    }

    /**
     * Auto-detect best available mode
     */
    private void configureAutoMode(String accessToken, String email, 
                                   String password, String cookies, boolean headless) {
        // Try API first (fastest and most reliable)
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                configureApiMode(accessToken);
                return;
            } catch (Exception e) {
                System.out.println("API mode failed, trying alternatives...");
            }
        }
        
        // Try Selenium (most powerful but slower)
        try {
            configureSeleniumMode(email, password, headless);
            return;
        } catch (Exception e) {
            System.out.println("Selenium mode failed, trying JSoup...");
        }
        
        // Fallback to JSoup (limited but no dependencies)
        try {
            configureJSoupMode(cookies);
            return;
        } catch (Exception e) {
            System.out.println("JSoup mode failed");
        }
        
        throw new RuntimeException(
            "Could not configure any Facebook scraping method. " +
            "Provide access_token, credentials, or cookies."
        );
    }

    @Override
    public List<DisasterEvent> fetchDisasterEvents() {
        if (!isAvailable()) {
            System.err.println("Facebook data source not configured");
            return new ArrayList<>();
        }

        List<FacebookPost> posts = fetchPosts();
        return convertToDisasterEvents(posts);
    }

    /**
     * Fetch posts using configured method
     */
    private List<FacebookPost> fetchPosts() {
        String identifier = pageUsername != null ? pageUsername : pageId;
        int maxPosts = 50; // Default
        
        switch (mode) {
            case API:
                return facebookService.fetchPagePosts(pageId);
                
            case SELENIUM:
                return seleniumScraper.scrapePagePosts(identifier, maxPosts);
                
            case JSOUP:
                return jsoupScraper.scrapePublicPagePosts(identifier, maxPosts);
                
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public List<DisasterEvent> fetchViralEvents(int minEngagement) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            if (mode == ScrapingMode.API && facebookService != null) {
                return convertToDisasterEvents(
                    facebookService.getViralDisasterPosts(pageId, minEngagement)
                );
            } else {
                // For scraping, fetch all and filter
                List<FacebookPost> posts = fetchPosts();
                List<FacebookPost> viralPosts = new ArrayList<>();
                
                for (FacebookPost post : posts) {
                    int engagement = post.getLikes() + post.getComments() + post.getShares();
                    if (engagement >= minEngagement) {
                        viralPosts.add(post);
                    }
                }
                
                return convertToDisasterEvents(viralPosts);
            }
        } catch (Exception e) {
            System.err.println("Error fetching viral events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert Facebook posts to disaster events
     */
    private List<DisasterEvent> convertToDisasterEvents(List<FacebookPost> posts) {
        List<DisasterEvent> events = new ArrayList<>();
        
        for (FacebookPost post : posts) {
            DisasterEvent event = new DisasterEvent();
            event.setSourceId(post.getId());
            event.setDescription(post.getMessage());
            event.setTimestamp(post.getCreatedTime());
            event.setSource("Facebook");
            event.setEngagement(post.getLikes() + post.getComments() + post.getShares());
            events.add(event);
        }
        
        return events;
    }

    @Override
    public String getSourceName() {
        return "Facebook (" + mode + ")";
    }

    @Override
    public boolean isAvailable() {
        if (!configured) return false;
        
        switch (mode) {
            case API:
                return facebookService != null;
            case SELENIUM:
                return seleniumScraper != null && seleniumScraper.isReady();
            case JSOUP:
                return jsoupScraper != null && jsoupScraper.isAccessible();
            default:
                return false;
        }
    }

    /**
     * Get current scraping mode
     */
    public ScrapingMode getMode() {
        return mode;
    }

    /**
     * Search for posts by keyword
     */
    public List<DisasterEvent> searchPosts(String keyword, int maxResults) {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            switch (mode) {
                case API:
                    posts = facebookService.searchPosts(pageId, keyword);
                    break;
                case SELENIUM:
                    posts = seleniumScraper.searchPosts(keyword, maxResults);
                    break;
                case JSOUP:
                    posts = jsoupScraper.searchPosts(keyword, maxResults);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error searching posts: " + e.getMessage());
        }
        
        return convertToDisasterEvents(posts);
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (seleniumScraper != null) {
            seleniumScraper.close();
        }
    }

    /**
     * Take screenshot (Selenium only, for debugging)
     */
    public void takeScreenshot(String filename) {
        if (mode == ScrapingMode.SELENIUM && seleniumScraper != null) {
            seleniumScraper.takeScreenshot(filename);
        }
    }
}