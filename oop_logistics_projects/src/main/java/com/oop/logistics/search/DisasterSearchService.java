package com.oop.logistics.search;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class DisasterSearchService {

    private static final List<String> DOMAINS = List.of(
            "thanhnien.vn", "vnexpress.net", "dantri.com.vn", "tuoitre.vn"
    );
    private static final String OUTPUT = "URL.csv";
    
    // The list of strategies
    private final List<SearchStrategy> strategies;

    public DisasterSearchService() {
        this.strategies = List.of(
            new BingRssStrategy(),
            new GoogleNewsRssStrategy(),
            new BingDirectStrategy(),
            new DuckDuckGoStrategy()
        );
    }
    public void searchFacebookUrls(String keyword) {
        System.out.println("Searching Facebook URLs for: " + keyword);
        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        // We use site:facebook.com to find posts related to the disaster
        String domain = "facebook.com";
        System.out.println("\n=== Searching domain: " + domain + " ===");
        
        for (SearchStrategy strategy : strategies) {
            // Bing and DuckDuckGo strategies are usually best for FB links
            strategy.search(domain, keyword, urlMap);
        }
        
        writeCsv(urlMap);
        System.out.println("\n=== DONE: total FB URLs = " + urlMap.size() + " ===");
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
            System.out.println("\n=== Searching domain: " + domain + " ===");
            for (String keyword : keywords) {
                System.out.println(" -> Using query: " + keyword);
                for (SearchStrategy strategy : strategies) {
                    strategy.search(domain, keyword, urlMap);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        writeCsv(urlMap);
        System.out.println("\n=== DONE: total URLs = " + urlMap.size() + " ===");
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
            e.printStackTrace(); 
        }
    }
}