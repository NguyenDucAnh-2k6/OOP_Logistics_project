package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleNewsRssStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(GoogleNewsRssStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        try {
            String encoded = URLEncoder.encode(keyword + " site:" + domain, StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=vi&gl=VN&ceid=VN:vi";
            logger.info("Google News RSS: {}", rssUrl);

            // UPGRADED JSOUP CONNECTION:
            Document doc = Jsoup.connect(rssUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*; q=0.01")
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .timeout(15000)
                    .get();

            for (Element item : doc.select("item")) {
                String url = UrlUtils.extractActualUrl(item.select("link").text());
                String pubDate = item.select("pubDate").text();
                SearchUtils.processResult(url, domain, pubDate, results);
            }
        } catch (Exception e) {
            logger.error("Google News Error for domain {} keyword {}", domain, e);
        }
    }
}