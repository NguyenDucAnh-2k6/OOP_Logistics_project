package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TuoiTreCrawler extends NewsCrawler {

    private static final Logger logger = LoggerFactory.getLogger(TuoiTreCrawler.class);

    @Override
    public NewsResult crawl(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            String title = doc.title(); // <-- Get the title

            String date = getMetaContent(doc, "pubdate");
            // Update the date fallback:
            if (date == null) {
                Element time = doc.selectFirst("div.detail-time, .date-time");
                if (time != null) date = time.text();
            }

            // Update the content selector:
            StringBuilder text = new StringBuilder();
            for (Element p : doc.select("div.detail-content p, div.detail-cmain p")) {
                text.append(p.text()).append("\n");
            }

            if (text.length() > 50) {
                // <-- RETURN THE DATA INSTEAD OF WRITING TO CSV
                return new NewsResult(url, title, date, text.toString().trim()); 
            }

        } catch (Exception e) {
            logger.error("Error crawling URL {}", url, e);
        }
        return null; // <-- Return null if it fails
    }
}