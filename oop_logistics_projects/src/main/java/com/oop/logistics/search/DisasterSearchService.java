package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisasterSearchService {

    private static final List<String> DOMAINS = List.of(
            "thanhnien.vn",
            "vnexpress.net",
            "dantri.com.vn",
            "tuoitre.vn"
    );

    private static final String OUTPUT = "URL.csv";
    
    // Date range for filtering
    private final LocalDate startDate;
    private final LocalDate endDate;

    // Store URLs with their dates
    private static class UrlWithDate {
        String url;
        LocalDate date;
        
        UrlWithDate(String url, LocalDate date) {
            this.url = url;
            this.date = date;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UrlWithDate that = (UrlWithDate) o;
            return Objects.equals(url, that.url);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(url);
        }
    }

    public DisasterSearchService() {
        this(LocalDate.of(2024, 9, 3), LocalDate.of(2025, 1, 31));
    }

    public DisasterSearchService(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void searchNewsUrls(String keyword) {
        System.out.println("Searching news URLs for: " + keyword);
        System.out.println("Date range: " + startDate + " to " + endDate);

        Map<String, UrlWithDate> urlMap = new LinkedHashMap<>();

        for (String domain : DOMAINS) {
            System.out.println("\n=== Searching domain: " + domain + " ===");
            
            // Strategy 1: Bing RSS
            try {
                searchViaBingRSS(domain, keyword, urlMap);
            } catch (Exception e) {
                System.out.println("Bing RSS error for " + domain + ": " + e.getMessage());
            }
            
            // Strategy 2: Google News RSS
            try {
                searchViaGoogleNewsRSS(domain, keyword, urlMap);
            } catch (Exception e) {
                System.out.println("Google News RSS error for " + domain + ": " + e.getMessage());
            }
            
            // Strategy 3: Direct Bing search with pagination
            try {
                searchViaBingDirect(domain, keyword, urlMap);
            } catch (Exception e) {
                System.out.println("Bing direct error for " + domain + ": " + e.getMessage());
            }
            
            // Strategy 4: DuckDuckGo HTML
            try {
                searchViaDuckDuckGo(domain, keyword, urlMap);
            } catch (Exception e) {
                System.out.println("DuckDuckGo error for " + domain + ": " + e.getMessage());
            }

            // Small delay between domains to avoid rate limiting
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        writeCsv(urlMap);
        System.out.println("\n=== DONE: total URLs = " + urlMap.size() + " ===");
    }

    /**
     * Strategy 1: Bing RSS search
     */
    private void searchViaBingRSS(String domain, String keyword, Map<String, UrlWithDate> output) throws Exception {
        String query = "site:" + domain + " " + keyword;
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String rssUrl = "https://www.bing.com/news/search?q=" + encoded + "&format=rss";
        
        System.out.println("Bing RSS: " + rssUrl);

        Document doc = Jsoup.connect(rssUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

        Elements items = doc.select("item");
        int found = 0;

        for (Element item : items) {
            String url = item.select("link").text();
            String pubDate = item.select("pubDate").text();
            
            if (isValidArticleUrl(url, domain)) {
                LocalDate date = extractDate(pubDate, url);
                
                // If we can't extract date from RSS or URL, try fetching the page
                if (date == null) {
                    date = fetchDateFromPage(url);
                }
                
                // Check if within date range or add anyway if we want all results
                if (date != null) {
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        output.putIfAbsent(url, new UrlWithDate(url, date));
                        found++;
                        System.out.println("  + Found: " + url + " [" + date + "]");
                    }
                } else {
                    // Add URLs without dates too
                    output.putIfAbsent(url, new UrlWithDate(url, null));
                    found++;
                    System.out.println("  + Found: " + url + " [date unknown]");
                }
            }
        }
        
        System.out.println("  Bing RSS found: " + found + " URLs");
    }

    /**
     * Strategy 2: Google News RSS
     */
    private void searchViaGoogleNewsRSS(String domain, String keyword, Map<String, UrlWithDate> output) throws Exception {
        String query = keyword + " site:" + domain;
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        
        // Google News RSS with date parameters
        String rssUrl = "https://news.google.com/rss/search?q=" + encoded 
                + "+after:2024-09-02+before:2024-09-11&hl=vi&gl=VN&ceid=VN:vi";
        
        System.out.println("Google News RSS: " + rssUrl);

        Document doc = Jsoup.connect(rssUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

        Elements items = doc.select("item");
        int found = 0;

        for (Element item : items) {
            String url = item.select("link").text();
            String pubDate = item.select("pubDate").text();
            
            // Google News often wraps URLs, extract the actual URL
            url = extractActualUrl(url);
            
            if (isValidArticleUrl(url, domain)) {
                LocalDate date = extractDate(pubDate, url);
                
                if (date == null) {
                    date = fetchDateFromPage(url);
                }
                
                if (date != null) {
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        output.putIfAbsent(url, new UrlWithDate(url, date));
                        found++;
                        System.out.println("  + Found: " + url + " [" + date + "]");
                    }
                } else {
                    output.putIfAbsent(url, new UrlWithDate(url, null));
                    found++;
                    System.out.println("  + Found: " + url + " [date unknown]");
                }
            }
        }
        
        System.out.println("  Google News RSS found: " + found + " URLs");
    }

    /**
     * Strategy 3: Bing direct HTML search with pagination
     */
    private void searchViaBingDirect(String domain, String keyword, Map<String, UrlWithDate> output) throws Exception {
        int found = 0;
        
        // Try multiple pages
        for (int page = 1; page <= 5; page++) {
            try {
                String query = "site:" + domain + " " + keyword;
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                int first = (page - 1) * 10 + 1;
                
                String searchUrl = "https://www.bing.com/search?q=" + encoded + "&first=" + first;
                System.out.println("Bing page " + page + ": " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15000)
                        .get();

                Elements results = doc.select("li.b_algo h2 a");
                
                for (Element result : results) {
                    String url = result.attr("href");
                    if (isValidArticleUrl(url, domain)) {
                        LocalDate date = extractDateFromUrl(url);
                        
                        if (date == null) {
                            date = fetchDateFromPage(url);
                        }
                        
                        if (date != null) {
                            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                                output.putIfAbsent(url, new UrlWithDate(url, date));
                                found++;
                                System.out.println("  + Found: " + url + " [" + date + "]");
                            }
                        } else {
                            output.putIfAbsent(url, new UrlWithDate(url, null));
                            found++;
                            System.out.println("  + Found: " + url + " [date unknown]");
                        }
                    }
                }

                // Delay between pages
                Thread.sleep(1500);
                
            } catch (Exception e) {
                System.out.println("  Error on page " + page + ": " + e.getMessage());
                break;
            }
        }
        
        System.out.println("  Bing direct found: " + found + " URLs");
    }

    /**
     * Strategy 4: DuckDuckGo search
     */
    private void searchViaDuckDuckGo(String domain, String keyword, Map<String, UrlWithDate> output) throws Exception {
        String query = "site:" + domain + " " + keyword;
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded;
        System.out.println("DuckDuckGo: " + searchUrl);

        Document doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

        Elements results = doc.select("a.result__a");
        int found = 0;

        for (Element result : results) {
            String url = result.attr("href");
            
            // DuckDuckGo wraps URLs in redirect
            if (url.contains("uddg=")) {
                url = extractDDGUrl(url);
            }
            
            if (isValidArticleUrl(url, domain)) {
                LocalDate date = extractDateFromUrl(url);
                
                if (date == null) {
                    date = fetchDateFromPage(url);
                }
                
                if (date != null) {
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        output.putIfAbsent(url, new UrlWithDate(url, date));
                        found++;
                        System.out.println("  + Found: " + url + " [" + date + "]");
                    }
                } else {
                    output.putIfAbsent(url, new UrlWithDate(url, null));
                    found++;
                    System.out.println("  + Found: " + url + " [date unknown]");
                }
            }
        }
        
        System.out.println("  DuckDuckGo found: " + found + " URLs");
    }

    /**
     * Fetch date from article page by scraping
     */
    private LocalDate fetchDateFromPage(String url) {
        try {
            System.out.println("    Fetching date from page: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            
            // Try multiple date extraction strategies
            LocalDate date = null;
            
            // Strategy 1: Look for time tag
            Elements timeElements = doc.select("time[datetime]");
            if (!timeElements.isEmpty()) {
                String datetime = timeElements.first().attr("datetime");
                date = parseDateFromISO(datetime);
                if (date != null) return date;
            }
            
            // Strategy 2: Look for meta tags
            Elements metaTags = doc.select("meta[property=article:published_time], meta[name=pubdate], meta[itemprop=datePublished]");
            if (!metaTags.isEmpty()) {
                String content = metaTags.first().attr("content");
                date = parseDateFromISO(content);
                if (date != null) return date;
            }
            
            // Strategy 3: VNExpress specific - look for date in article header
            // Format: Thứ ba, 5/9/2024, 12:00 (GMT+7)
            Elements vnexpressDate = doc.select("span.date");
            if (!vnexpressDate.isEmpty()) {
                date = parseVietnameseDate(vnexpressDate.text());
                if (date != null) return date;
            }
            
            // Strategy 4: Thanh Nien specific
            Elements thanhnienDate = doc.select("div.date-time");
            if (!thanhnienDate.isEmpty()) {
                date = parseVietnameseDate(thanhnienDate.text());
                if (date != null) return date;
            }
            
            // Strategy 5: Dantri specific
            Elements dantriDate = doc.select("time.author-time, span.author-time");
            if (!dantriDate.isEmpty()) {
                date = parseVietnameseDate(dantriDate.text());
                if (date != null) return date;
            }
            
            // Strategy 6: Tuoi Tre specific
            Elements tuoitreDate = doc.select("div.date-time, span.date-time");
            if (!tuoitreDate.isEmpty()) {
                date = parseVietnameseDate(tuoitreDate.text());
                if (date != null) return date;
            }
            
            // Strategy 7: Look in JSON-LD structured data
            Elements scripts = doc.select("script[type=application/ld+json]");
            for (Element script : scripts) {
                String json = script.html();
                Pattern pattern = Pattern.compile("\"datePublished\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(json);
                if (matcher.find()) {
                    date = parseDateFromISO(matcher.group(1));
                    if (date != null) return date;
                }
            }
            
            // Small delay to avoid hammering servers
            Thread.sleep(500);
            
        } catch (Exception e) {
            System.out.println("    Error fetching date: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Parse ISO date format (2024-09-05T12:00:00+07:00)
     */
    private LocalDate parseDateFromISO(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        
        try {
            // Remove timezone and time info, keep only date part
            String datePart = isoDate.split("T")[0];
            return LocalDate.parse(datePart, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse Vietnamese date formats: "5/9/2024", "05/09/2024", "5-9-2024"
     */
    private LocalDate parseVietnameseDate(String dateText) {
        if (dateText == null || dateText.isEmpty()) return null;
        
        try {
            // Remove day of week and other text, keep only date numbers
            // Example: "Thứ ba, 5/9/2024, 12:00 (GMT+7)" -> "5/9/2024"
            Pattern pattern = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})");
            Matcher matcher = pattern.matcher(dateText);
            
            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return null;
    }

    /**
     * Extract date from pubDate or URL
     */
    private LocalDate extractDate(String pubDate, String url) {
        // Try to parse pubDate first (from RSS)
        if (pubDate != null && !pubDate.isEmpty()) {
            LocalDate date = parseRSSDate(pubDate);
            if (date != null) {
                return date;
            }
        }

        // Try to extract date from URL
        return extractDateFromUrl(url);
    }

    /**
     * Extract actual URL from Google News redirect
     */
    private String extractActualUrl(String wrappedUrl) {
        if (wrappedUrl == null) return "";
        
        // Google News format: https://news.google.com/rss/articles/...?url=ACTUAL_URL
        if (wrappedUrl.contains("news.google.com")) {
            int urlIndex = wrappedUrl.indexOf("url=");
            if (urlIndex > 0) {
                String extracted = wrappedUrl.substring(urlIndex + 4);
                // Remove any trailing parameters
                int ampIndex = extracted.indexOf("&");
                if (ampIndex > 0) {
                    extracted = extracted.substring(0, ampIndex);
                }
                return extracted;
            }
        }
        
        return wrappedUrl;
    }

    /**
     * Extract URL from DuckDuckGo redirect
     */
    private String extractDDGUrl(String ddgUrl) {
        if (ddgUrl == null) return "";
        
        int uddgIndex = ddgUrl.indexOf("uddg=");
        if (uddgIndex > 0) {
            String extracted = ddgUrl.substring(uddgIndex + 5);
            int ampIndex = extracted.indexOf("&");
            if (ampIndex > 0) {
                extracted = extracted.substring(0, ampIndex);
            }
            try {
                return java.net.URLDecoder.decode(extracted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return extracted;
            }
        }
        
        return ddgUrl;
    }

    /**
     * Check if URL is valid article URL
     */
    private boolean isValidArticleUrl(String url, String domain) {
        if (url == null || url.isEmpty()) return false;
        if (!url.contains(domain)) return false;

        return !url.contains("/video")
                && !url.contains("/tag")
                && !url.contains("/tim-kiem")
                && !url.contains("/search")
                && !url.contains("/page/")
                && !url.contains("/category/");
    }

    /**
     * Parse RSS date format
     */
    private LocalDate parseRSSDate(String pubDate) {
        try {
            // Example: "Wed, 04 Sep 2024 10:30:00 GMT"
            String[] parts = pubDate.split(" ");
            if (parts.length >= 4) {
                int day = Integer.parseInt(parts[1]);
                String monthStr = parts[2];
                int year = Integer.parseInt(parts[3]);
                
                int month = getMonthNumber(monthStr);
                if (month > 0) {
                    return LocalDate.of(year, month, day);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    /**
     * Get month number from abbreviation
     */
    private int getMonthNumber(String monthAbbr) {
        switch (monthAbbr.toLowerCase()) {
            case "jan": return 1;
            case "feb": return 2;
            case "mar": return 3;
            case "apr": return 4;
            case "may": return 5;
            case "jun": return 6;
            case "jul": return 7;
            case "aug": return 8;
            case "sep": return 9;
            case "oct": return 10;
            case "nov": return 11;
            case "dec": return 12;
            default: return -1;
        }
    }

    /**
     * Extract date from URL patterns commonly used by Vietnamese news sites
     */
    private LocalDate extractDateFromUrl(String url) {
        if (url == null) return null;

        // Pattern 1: /YYYYMMDD/ or -YYYYMMDD-
        Pattern pattern1 = Pattern.compile("/(\\d{8})[/-]");
        Matcher matcher1 = pattern1.matcher(url);
        if (matcher1.find()) {
            try {
                String dateStr = matcher1.group(1);
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException e) {
                // Continue to next pattern
            }
        }

        // Pattern 2: /YYYY/MM/DD/
        Pattern pattern2 = Pattern.compile("/(\\d{4})/(\\d{2})/(\\d{2})/");
        Matcher matcher2 = pattern2.matcher(url);
        if (matcher2.find()) {
            try {
                int year = Integer.parseInt(matcher2.group(1));
                int month = Integer.parseInt(matcher2.group(2));
                int day = Integer.parseInt(matcher2.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Continue to next pattern
            }
        }

        // Pattern 3: -pYYYYMMDD or -tYYYYMMDD (VNExpress style)
        Pattern pattern3 = Pattern.compile("-[pt](\\d{8})");
        Matcher matcher3 = pattern3.matcher(url);
        if (matcher3.find()) {
            try {
                String dateStr = matcher3.group(1);
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException e) {
                // Ignore
            }
        }

        return null;
    }

    /**
     * Write URLs with dates to CSV file
     */
    private void writeCsv(Map<String, UrlWithDate> urlMap) {
        if (urlMap.isEmpty()) return;

        boolean exists = new java.io.File(OUTPUT).exists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT, true))) {
            if (!exists) {
                pw.println("date,url");
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (UrlWithDate entry : urlMap.values()) {
                String dateStr = entry.date != null ? entry.date.format(dateFormatter) : "unknown";
                String escapedUrl = entry.url.replace("\"", "\"\"");
                pw.println("\"" + dateStr + "\",\"" + escapedUrl + "\"");
            }
            
            System.out.println("\nWrote " + urlMap.size() + " URLs to " + OUTPUT);
        } catch (Exception e) {
            System.err.println("Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}