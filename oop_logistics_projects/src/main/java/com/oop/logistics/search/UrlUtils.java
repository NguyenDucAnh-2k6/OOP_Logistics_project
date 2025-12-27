package com.oop.logistics.search;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UrlUtils {

    public static boolean isValidArticleUrl(String url, String domain) {
        if (url == null || url.isEmpty()) return false;
        if (!url.contains(domain)) return false;

        return !url.contains("/video")
                && !url.contains("/tag")
                && !url.contains("/tim-kiem")
                && !url.contains("/search")
                && !url.contains("/page/")
                && !url.contains("/category/");
    }

    public static String extractActualUrl(String wrappedUrl) {
        if (wrappedUrl == null) return "";
        // Google News format
        if (wrappedUrl.contains("news.google.com")) {
            int urlIndex = wrappedUrl.indexOf("url=");
            if (urlIndex > 0) {
                String extracted = wrappedUrl.substring(urlIndex + 4);
                int ampIndex = extracted.indexOf("&");
                if (ampIndex > 0) {
                    extracted = extracted.substring(0, ampIndex);
                }
                return extracted;
            }
        }
        return wrappedUrl;
    }

    public static String extractDDGUrl(String ddgUrl) {
        if (ddgUrl == null) return "";
        int uddgIndex = ddgUrl.indexOf("uddg=");
        if (uddgIndex > 0) {
            String extracted = ddgUrl.substring(uddgIndex + 5);
            int ampIndex = extracted.indexOf("&");
            if (ampIndex > 0) {
                extracted = extracted.substring(0, ampIndex);
            }
            try {
                return URLDecoder.decode(extracted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return extracted;
            }
        }
        return ddgUrl;
    }
}