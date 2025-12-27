package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class DisasterSearchService {

    private static final List<String> DOMAINS = List.of(
            "thanhnien.vn", "vnexpress.net", "dantri.com.vn", "tuoitre.vn"
    );
    private static final String OUTPUT = "URL.csv";
    
    private final LocalDate startDate;
    private final LocalDate endDate;

    public DisasterSearchService() {
        this(LocalDate.of(2024, 9, 3), LocalDate.of(2025, 1, 31));
    }

    public DisasterSearchService(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void searchNewsUrls(String keyword) {
        System.out.println("Searching news URLs for: " + keyword);
        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        // Define strategies
        List<SearchStrategy> strategies = List.of(
            this::searchViaBingRSS,
            this::searchViaGoogleNewsRSS,
            this::searchViaBingDirect,
            this::searchViaDuckDuckGo
        );

        for (String domain : DOMAINS) {
            System.out.println("\n=== Searching domain: " + domain + " ===");
            for (SearchStrategy strategy : strategies) {
                try {
                    strategy.search(domain, keyword, urlMap);
                } catch (Exception e) {
                    System.out.println("Error in strategy: " + e.getMessage());
                }
            }
            sleep(2000);
        }

        writeCsv(urlMap);
        System.out.println("\n=== DONE: total URLs = " + urlMap.size() + " ===");
    }

    // --- Concrete Strategy Implementations (Could be moved to separate files too) ---

    private void searchViaBingRSS(String domain, String keyword, Map<String, UrlWithDate> output) {
        try {
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            String rssUrl = "https://www.bing.com/news/search?q=" + encoded + "&format=rss";
            System.out.println("Bing RSS: " + rssUrl);

            Document doc = Jsoup.connect(rssUrl).timeout(15000).get();
            for (Element item : doc.select("item")) {
                String url = item.select("link").text();
                String pubDate = item.select("pubDate").text();
                processResult(url, domain, pubDate, output);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void searchViaGoogleNewsRSS(String domain, String keyword, Map<String, UrlWithDate> output) {
        try {
            String encoded = URLEncoder.encode(keyword + " site:" + domain, StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "+after:2024-09-02+before:2024-09-11&hl=vi&gl=VN&ceid=VN:vi";
            
            Document doc = Jsoup.connect(rssUrl).timeout(15000).get();
            for (Element item : doc.select("item")) {
                String url = UrlUtils.extractActualUrl(item.select("link").text());
                String pubDate = item.select("pubDate").text();
                processResult(url, domain, pubDate, output);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void searchViaBingDirect(String domain, String keyword, Map<String, UrlWithDate> output) {
        try {
            for (int page = 1; page <= 3; page++) {
                String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
                String url = "https://www.bing.com/search?q=" + encoded + "&first=" + ((page - 1) * 10 + 1);
                
                Document doc = Jsoup.connect(url).timeout(15000).get();
                for (Element result : doc.select("li.b_algo h2 a")) {
                    processResult(result.attr("href"), domain, null, output);
                }
                sleep(1000);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void searchViaDuckDuckGo(String domain, String keyword, Map<String, UrlWithDate> output) {
        try {
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encoded;
            
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            for (Element result : doc.select("a.result__a")) {
                String cleanUrl = UrlUtils.extractDDGUrl(result.attr("href"));
                processResult(cleanUrl, domain, null, output);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // --- Helper Methods ---

    private void processResult(String url, String domain, String pubDateRaw, Map<String, UrlWithDate> output) {
        if (!UrlUtils.isValidArticleUrl(url, domain)) return;

        // Use DateUtils to handle complex logic
        LocalDate date = DateUtils.determineDate(url, pubDateRaw);

        if (date != null) {
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                output.putIfAbsent(url, new UrlWithDate(url, date));
                System.out.println("  + Found: " + url + " [" + date + "]");
            }
        } else {
            // Keep unknowns? User logic said yes
            output.putIfAbsent(url, new UrlWithDate(url, null));
            System.out.println("  + Found (No Date): " + url);
        }
    }

    private void writeCsv(Map<String, UrlWithDate> urlMap) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT, true))) {
            for (UrlWithDate entry : urlMap.values()) {
                String d = entry.getDate() != null ? entry.getDate().toString() : "unknown";
                pw.println("\"" + d + "\",\"" + entry.getUrl().replace("\"", "\"\"") + "\"");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}