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
            case "facebook" -> "facebook.com"; 
            default -> "";
        };

        if (domain.isEmpty()) {
            logger.error("Unknown platform selected for search: {}", platform);
            return;
        }

        logger.info("=== Searching domain: {} ===", domain);
        
        // 1. ADVANCED QUERY VARIATIONS
        List<String> searchVariations;
        if (platform.equalsIgnoreCase("tiktok")) {
            // Using "inurl:video" forces search engines to ONLY return actual video posts
            // rather than useless /tag/ or /@username profile pages.
            searchVariations = List.of(
                keyword,
                "inurl:video " + keyword
            );
        } else if (platform.equalsIgnoreCase("youtube")) {
            searchVariations = List.of(keyword, keyword + " tin tức", keyword + " shorts");
        } else {
            searchVariations = List.of(keyword, keyword + " review", keyword + " thảo luận");
        }

        for (String query : searchVariations) {
            for (SearchStrategy strategy : strategies) {
                strategy.search(domain, query, urlMap);
            }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {} 
        }
        
        // 2. RESILIENT URL FILTERING
        Map<String, UrlWithDate> filteredMap = new LinkedHashMap<>();
        for (Map.Entry<String, UrlWithDate> entry : urlMap.entrySet()) {
            String url = entry.getKey().toLowerCase();
            
            if (platform.equalsIgnoreCase("tiktok")) {
                // Check for standard "/video/" or Bing's URL-encoded "%2Fvideo%2F" tracking links
                if (url.contains("tiktok.com") && (url.contains("/video/") || url.contains("%2fvideo%2f") || url.contains("vm.tiktok.com"))) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else if (platform.equalsIgnoreCase("youtube")) {
                if (url.contains("watch?v=") || url.contains("/shorts/")) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            } else {
                filteredMap.put(entry.getKey(), entry.getValue()); 
            }
        }

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