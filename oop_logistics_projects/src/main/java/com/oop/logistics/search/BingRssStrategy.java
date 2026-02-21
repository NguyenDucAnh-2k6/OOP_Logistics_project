package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BingRssStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BingRssStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            String rssUrl = "https://www.bing.com/news/search?q=" + encoded + "&format=rss";
            logger.info("Bing RSS: {}", rssUrl);

            Document doc = Jsoup.connect(rssUrl).timeout(15000).get();
            for (Element item : doc.select("item")) {
                String url = item.select("link").text();
                String pubDate = item.select("pubDate").text();

                // Helper method call (assume accessible via static import or utility)
                SearchUtils.processResult(url, domain, pubDate, results);
            }
        } catch (Exception e) {
            logger.error("Bing RSS Error for domain {} keyword {}", domain, e);
        }
    }
}