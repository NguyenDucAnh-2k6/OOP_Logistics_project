package com.oop.logistics.crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class TestNewsCrawler {

    public static void main(String[] args) {
        // Read URLs from URL.csv
        List<String> urls = readUrlsFromCsv("URL.csv");
        
        System.out.println("📋 Total URLs to crawl: " + urls.size());
        
        int success = 0;
        int failed = 0;
        
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            try {
                System.out.println("\n[" + (i + 1) + "/" + urls.size() + "] 🔗 Crawling: " + url);
                NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
                System.out.println("     Using crawler: " + crawler.getClass().getSimpleName());
                crawler.crawl(url);
                success++;
                System.out.println("     ✅ Success");
            } catch (Exception e) {
                failed++;
                System.err.println("     ❌ Failed: " + e.getMessage());
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 Crawl Summary:");
        System.out.println("   Success: " + success);
        System.out.println("   Failed: " + failed);
        System.out.println("   Total: " + urls.size());
        System.out.println("   Output: YagiNews.csv");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Read URLs from CSV file. Each line is a URL (wrapped in quotes).
     */
    private static List<String> readUrlsFromCsv(String filePath) {
        List<String> urls = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // skip header if present
            boolean first = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (first) {
                    first = false;
                    // if header contains "url" skip it
                    String lower = line.toLowerCase();
                    if (lower.contains("date") && lower.contains("url")) continue;
                }

                // Expected format: "date","url"  -> extract second column safely
                try {
                    if (line.startsWith("\"") && line.contains("\",\"")) {
                        int sep = line.indexOf("\",\"");
                        String urlPart = line.substring(sep + 3);
                        // remove trailing quote if present
                        if (urlPart.endsWith("\"")) urlPart = urlPart.substring(0, urlPart.length() - 1);
                        urlPart = urlPart.trim();
                        if (!urlPart.isEmpty()) urls.add(urlPart);
                        continue;
                    }
                } catch (Exception ignored) {}

                // fallback: if line is a single quoted URL or plain URL
                if (line.startsWith("\"") && line.endsWith("\"")) {
                    line = line.substring(1, line.length() - 1).trim();
                }
                if (!line.isEmpty()) urls.add(line);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error reading URL.csv: " + e.getMessage());
        }
        return urls;
    }
}
