package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleNewsRssStrategy implements SearchStrategy {
    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            String encoded = URLEncoder.encode(keyword + " site:" + domain, StandardCharsets.UTF_8);
            // Example time range: modify as needed or pass in via constructor
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "+after:2024-09-02+before:2024-09-11&hl=vi&gl=VN&ceid=VN:vi";
            System.out.println("Google News RSS: " + rssUrl);

            Document doc = Jsoup.connect(rssUrl).timeout(15000).get();
            for (Element item : doc.select("item")) {
                String url = UrlUtils.extractActualUrl(item.select("link").text());
                String pubDate = item.select("pubDate").text();
                SearchUtils.processResult(url, domain, pubDate, results);
            }
        } catch (Exception e) {
            System.err.println("Google News Error: " + e.getMessage());
        }
    }
}