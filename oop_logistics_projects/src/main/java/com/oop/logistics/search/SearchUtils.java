package com.oop.logistics.search;

import java.time.LocalDate;
import java.util.Map;

public class SearchUtils {
    // Hardcoded date range for filtering (or you can pass these in)
    private static final LocalDate START_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate END_DATE = LocalDate.now();

    public static void processResult(String url, String domain, String pubDateRaw, Map<String, UrlWithDate> output) {
        url = UrlUtils.cleanFacebookUrl(url);
        if (!UrlUtils.isValidArticleUrl(url, domain)) return;

        LocalDate date = DateUtils.determineDate(url, pubDateRaw);

        if (date != null) {
            // Now it accepts almost any valid date
            if (!date.isBefore(START_DATE) && !date.isAfter(END_DATE)) {
                if (output.putIfAbsent(url, new UrlWithDate(url, date)) == null) {
                    System.out.println("  + NEW URL Added: " + url + " [" + date + "]");
                }
            }
        } else {
            output.putIfAbsent(url, new UrlWithDate(url, null));
            System.out.println("  + Found (No Date): " + url);
        }
    }
    
}