package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    /**
     * Main method to get a date from a URL (pattern) or by fetching the page content.
     */
    public static LocalDate determineDate(String url, String pubDateString) {
        // 1. Try RSS pubDate
        if (pubDateString != null && !pubDateString.isEmpty()) {
            LocalDate date = parseRSSDate(pubDateString);
            if (date != null) return date;
        }

        // 2. Try URL Pattern
        LocalDate dateFromUrl = extractDateFromUrl(url);
        if (dateFromUrl != null) return dateFromUrl;

        // 3. Fetch Page
        return fetchDateFromPage(url);
    }

    private static LocalDate fetchDateFromPage(String url) {
        try {
            System.out.println("    Fetching date from page: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // Strategy 1: Time tag
            Elements timeElements = doc.select("time[datetime]");
            if (!timeElements.isEmpty()) {
                LocalDate d = parseDateFromISO(timeElements.first().attr("datetime"));
                if (d != null) return d;
            }

            // Strategy 2: Meta tags
            Elements metaTags = doc.select("meta[property=article:published_time], meta[name=pubdate], meta[itemprop=datePublished]");
            if (!metaTags.isEmpty()) {
                LocalDate d = parseDateFromISO(metaTags.first().attr("content"));
                if (d != null) return d;
            }

            // Strategy 3: Vietnamese specific selectors
            String[] selectors = {
                "span.date", "div.date-time", "time.author-time", "span.author-time"
            };
            for (String sel : selectors) {
                Elements el = doc.select(sel);
                if (!el.isEmpty()) {
                    LocalDate d = parseVietnameseDate(el.text());
                    if (d != null) return d;
                }
            }
            
            // Delay to be polite
            Thread.sleep(500);

        } catch (Exception e) {
            // Log error if needed
        }
        return null;
    }

    private static LocalDate extractDateFromUrl(String url) {
        if (url == null) return null;
        
        // Pattern: /20240905/ or -20240905-
        Pattern p1 = Pattern.compile("[/-](20\\d{2})(\\d{2})(\\d{2})[/-]");
        Matcher m1 = p1.matcher(url);
        if (m1.find()) return LocalDate.of(Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(3)));

        // Pattern: /2024/09/05/
        Pattern p2 = Pattern.compile("/(20\\d{2})/(\\d{2})/(\\d{2})/");
        Matcher m2 = p2.matcher(url);
        if (m2.find()) return LocalDate.of(Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)), Integer.parseInt(m2.group(3)));

        return null;
    }

    private static LocalDate parseRSSDate(String pubDate) {
        try {
            // "Wed, 04 Sep 2024..."
            String[] parts = pubDate.split(" ");
            if (parts.length >= 4) {
                int day = Integer.parseInt(parts[1]);
                int month = getMonthNumber(parts[2]);
                int year = Integer.parseInt(parts[3]);
                if (month > 0) return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static LocalDate parseDateFromISO(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return LocalDate.parse(iso.split("T")[0], DateTimeFormatter.ISO_DATE);
        } catch (Exception e) { return null; }
    }

    private static LocalDate parseVietnameseDate(String text) {
        try {
            Pattern p = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})");
            Matcher m = p.matcher(text);
            if (m.find()) return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        } catch (Exception ignored) {}
        return null;
    }

    private static int getMonthNumber(String abbr) {
        switch (abbr.toLowerCase()) {
            case "jan": return 1; case "feb": return 2; case "mar": return 3; case "apr": return 4;
            case "may": return 5; case "jun": return 6; case "jul": return 7; case "aug": return 8;
            case "sep": return 9; case "oct": return 10; case "nov": return 11; case "dec": return 12;
            default: return -1;
        }
    }
}