package com.oop.logistics.search;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisasterSearchService {

    private static final List<String> DOMAINS = List.of(
            "thanhnien.vn", "vnexpress.net", "dantri.com.vn", "tuoitre.vn"
    );
    private static final String OUTPUT = "URL.csv";
    
    // The list of strategies
    private final List<SearchStrategy> strategies;

    private static final Logger logger = LoggerFactory.getLogger(DisasterSearchService.class);

    public DisasterSearchService() {
        this.strategies = List.of(
            new BingRssStrategy(),
            new GoogleNewsRssStrategy(),
            new BingDirectStrategy(),
            new DuckDuckGoStrategy()
        );
    }
    public void searchFacebookUrls(String keyword) {
        logger.info("Searching Facebook URLs for: {}", keyword);
        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        // We use site:facebook.com to find posts related to the disaster
        String domain = "facebook.com";
        logger.info("=== Searching domain: {} ===", domain);
        
        for (SearchStrategy strategy : strategies) {
            // Bing and DuckDuckGo strategies are usually best for FB links
            strategy.search(domain, keyword, urlMap);
        }
        
        writeCsv(urlMap);
        logger.info("=== DONE: total FB URLs = {} ===", urlMap.size());
    }
    public void searchNewsUrls(String baseKeyword) {
        // Create variations of the search term
        List<String> keywords = List.of(
            baseKeyword, 
            "Bão " + baseKeyword.replace("Typhoon", "").trim(), // Translates to Vietnamese
            baseKeyword + " thiệt hại" // Adds "damage" context
        );

        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        for (String domain : DOMAINS) {
            logger.info("=== Searching domain: {} ===", domain);
            for (String keyword : keywords) {
                logger.debug(" -> Using query: {}", keyword);
                for (SearchStrategy strategy : strategies) {
                    strategy.search(domain, keyword, urlMap);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        writeCsv(urlMap);
        logger.info("=== DONE: total URLs = {} ===", urlMap.size());
    }
    // Add this method inside DisasterSearchService.java
    
    public void searchSocialUrls(String keyword, String platform) {
        logger.info("Searching {} URLs for: {}", platform, keyword);
        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        String domain = switch (platform.toLowerCase()) {
            case "youtube" -> "youtube.com";
            case "tiktok" -> "tiktok.com";
            case "voz" -> "voz.vn";
            case "reddit" -> "reddit.com";
            case "facebook", "facebook-debug" -> "facebook.com"; 
            case "twitter" -> "x.com"; // Twitter is now indexed heavily under x.com
            case "instagram" -> "instagram.com";
            case "threads" -> "threads.com";
            default -> "";
        };

        if (domain.isEmpty()) {
            logger.error("Unknown platform selected for search: {}", platform);
            return;
        }

        logger.info("=== Searching domain: {} ===", domain);
        
        List<String> searchVariations;
        if (platform.equalsIgnoreCase("tiktok")) {
            searchVariations = List.of(keyword, "inurl:video " + keyword);
        } else if (platform.equalsIgnoreCase("instagram")) {
            // Forces search engine to only return /p/ (posts) or /reel/ (videos)
            searchVariations = List.of(keyword, "inurl:p OR inurl:reel " + keyword);
        } else if (platform.equalsIgnoreCase("youtube")) {
            searchVariations = List.of(keyword, keyword + " tin tức", keyword + " shorts");
        } else if (platform.equalsIgnoreCase("threads")) {
            // Forces search engine to only return specific Threads posts
            searchVariations = List.of(keyword, "inurl:post " + keyword);
        } else if (platform.equalsIgnoreCase("twitter")) {
            // "inurl:status" ensures we get actual tweets, not profile pages
            searchVariations = List.of(keyword, "inurl:status " + keyword);
        } else {
            searchVariations = List.of(keyword, keyword + " review", keyword + " thảo luận");
        }

        for (String query : searchVariations) {
            for (SearchStrategy strategy : strategies) {
                strategy.search(domain, query, urlMap);
            }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {} 
        }
        
        // 2. STRICT URL FILTERING
        // Search engines often return user profiles, tags, or group hubs.
        // We MUST filter these out so the crawlers actually find specific posts/threads.
        Map<String, UrlWithDate> filteredMap = new LinkedHashMap<>();
        for (Map.Entry<String, UrlWithDate> entry : urlMap.entrySet()) {
            String url = entry.getKey().toLowerCase();
            
            if (platform.equalsIgnoreCase("tiktok")) {
                if (url.contains("tiktok.com") && (url.contains("/video/") || url.contains("%2fvideo%2f") || url.contains("vm.tiktok.com"))) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("instagram")) {
                // STRICT INSTAGRAM FILTER: Must be a specific post or reel.
                // Rejects generic instagram.com/username/ profiles.
                if (url.contains("/p/") || url.contains("/reel/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("youtube")) {
                if (url.contains("watch?v=") || url.contains("/shorts/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("twitter")) {
                if (url.contains("/status/") || url.contains("%2fstatus%2f")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("threads")) {
                // STRICT THREADS FILTER: Must be a specific post.
                // Rejects generic threads.net/@username profiles.
                if (url.contains("/post/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.toLowerCase().startsWith("facebook")) {
                // STRICT FACEBOOK FILTER: Must be a specific post, reel, video, or permalink.
                // Rejects generic /groups/ or /people/ homepages.
                if (url.contains("/posts/") || url.contains("/reel/") || url.contains("/videos/") || url.contains("/watch/") || url.contains("/permalink/") || url.contains("story.php")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("reddit")) {
                // Keep only Reddit comment threads, reject subreddit homepages
                if (url.contains("/comments/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("voz")) {
                // Keep only Voz threads, reject forum categories
                if (url.contains("/t/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else {
                filteredMap.put(entry.getKey(), entry.getValue()); 
            }
        }

        writeCsv(filteredMap);
        logger.info("=== DONE: total {} URLs = {} ===", platform, filteredMap.size());

        writeCsv(filteredMap);
        logger.info("=== DONE: total {} URLs = {} ===", platform, filteredMap.size());
    }
    private void writeCsv(Map<String, UrlWithDate> urlMap) {
        // Removed 'true' to overwrite the file cleanly for the new disaster
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT))) {
            
            // 1. Explicitly write the header row first
            pw.println("\"date\",\"url\""); 
            
            // 2. Write the search results
            for (UrlWithDate entry : urlMap.values()) {
                String d = entry.getDate() != null ? entry.getDate().toString() : "unknown";
                pw.println("\"" + d + "\",\"" + entry.getUrl().replace("\"", "\"\"") + "\"");
            }
        } catch (Exception e) { 
            logger.error("Failed to write CSV {}", OUTPUT, e);
        }
    }
}