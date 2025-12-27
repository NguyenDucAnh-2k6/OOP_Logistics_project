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

    public void searchNewsUrls(String keyword) {
        System.out.println("Searching news URLs for: " + keyword);
        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        for (String domain : DOMAINS) {
            System.out.println("\n=== Searching domain: " + domain + " ===");
            
            // Polymorphism in action!
            for (SearchStrategy strategy : strategies) {
                strategy.search(domain, keyword, urlMap);
            }
            
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }

        writeCsv(urlMap);
        System.out.println("\n=== DONE: total URLs = " + urlMap.size() + " ===");
    }

    private void writeCsv(Map<String, UrlWithDate> urlMap) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT, true))) {
            for (UrlWithDate entry : urlMap.values()) {
                String d = entry.getDate() != null ? entry.getDate().toString() : "unknown";
                pw.println("\"" + d + "\",\"" + entry.getUrl().replace("\"", "\"\"") + "\"");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}