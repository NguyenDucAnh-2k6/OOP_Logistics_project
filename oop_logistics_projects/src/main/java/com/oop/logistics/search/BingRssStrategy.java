package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BingRssStrategy implements SearchStrategy {
    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            String rssUrl = "https://www.bing.com/news/search?q=" + encoded + "&format=rss";
            System.out.println("Bing RSS: " + rssUrl);

            Document doc = Jsoup.connect(rssUrl).timeout(15000).get();
            for (Element item : doc.select("item")) {
                String url = item.select("link").text();
                String pubDate = item.select("pubDate").text();
                
                // Helper method call (assume accessible via static import or utility)
                SearchUtils.processResult(url, domain, pubDate, results);
            }
        } catch (Exception e) {
            System.err.println("Bing RSS Error: " + e.getMessage());
        }
    }
}