package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BingDirectStrategy implements SearchStrategy {
    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            // Limit to 3 pages for example
            for (int page = 1; page <= 3; page++) {
                String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
                String url = "https://www.bing.com/search?q=" + encoded + "&first=" + ((page - 1) * 10 + 1);
                
                Document doc = Jsoup.connect(url).timeout(15000).get();
                for (Element result : doc.select("li.b_algo h2 a")) {
                    SearchUtils.processResult(result.attr("href"), domain, null, results);
                }
                
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Bing Direct Error: " + e.getMessage());
        }
    }
}