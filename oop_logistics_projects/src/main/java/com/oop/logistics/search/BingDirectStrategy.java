package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BingDirectStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BingDirectStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            // INCREASED: Search 10 pages deep instead of 3
            for (int page = 1; page <= 10; page++) {
                String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
                String url = "https://www.bing.com/search?q=" + encoded + "&first=" + ((page - 1) * 10 + 1);

                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0") // Add a user agent to avoid getting blocked
                    .timeout(15000).get();
                for (Element result : doc.select("li.b_algo h2 a")) {
                    SearchUtils.processResult(result.attr("href"), domain, null, results);
                }

                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            logger.error("Bing Direct Error for domain {} keyword {}", domain, e);
        }
    }
}