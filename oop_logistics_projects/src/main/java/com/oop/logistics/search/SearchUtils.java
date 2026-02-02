package com.oop.logistics.search;

import java.time.LocalDate;
import java.util.Map;

public class SearchUtils {
    // Hardcoded date range for filtering (or you can pass these in)
    private static final LocalDate START_DATE = LocalDate.of(2024, 9, 3);
    private static final LocalDate END_DATE = LocalDate.of(2025, 1, 31);

    public static void processResult(String url, String domain, String pubDateRaw, Map<String, UrlWithDate> output) {
        if (!UrlUtils.isValidArticleUrl(url, domain)) return;

        LocalDate date = DateUtils.determineDate(url, pubDateRaw);

        if (date != null) {
            if (!date.isBefore(START_DATE) && !date.isAfter(END_DATE)) {
                output.putIfAbsent(url, new UrlWithDate(url, date));
                System.out.println("  + Found: " + url + " [" + date + "]");
            }
        } else {
            output.putIfAbsent(url, new UrlWithDate(url, null));
            System.out.println("  + Found (No Date): " + url);
        }
    }
}