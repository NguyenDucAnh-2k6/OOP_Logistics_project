package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DuckDuckGoStrategy implements SearchStrategy {
    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encoded;
            System.out.println("DuckDuckGo: " + url);
            
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            for (Element result : doc.select("a.result__a")) {
                String cleanUrl = UrlUtils.extractDDGUrl(result.attr("href"));
                SearchUtils.processResult(cleanUrl, domain, null, results);
            }
        } catch (Exception e) {
            System.err.println("DDG Error: " + e.getMessage());
        }
    }
}