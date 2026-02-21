package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DanTriCrawler extends NewsCrawler {

    private static final Logger logger = LoggerFactory.getLogger(DanTriCrawler.class);

    @Override
    public NewsResult crawl(String url) {
        logger.info("Starting crawl for URL: {}", url);
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            String title = doc.title(); // <-- Get the title

            String date = getMetaContent(doc, "pubdate");
            if (date == null) {
                Element time = doc.selectFirst("time.author-time, span.dt-news__time, .author-wrap time");
                if (time != null) date = time.text();
            }

            // Update the content selector:
            StringBuilder text = new StringBuilder();
            for (Element p : doc.select("div.singular-content p, div.dt-news__content p")) {
                text.append(p.text()).append("\n");
            }

            if (text.length() > 10) {
                // <-- RETURN THE DATA INSTEAD OF WRITING TO CSV
                logger.info("Successfully extracted data from URL.");
                return new NewsResult(url, title, date, text.toString().trim()); 
            }
        
        } catch (Exception e) {
            logger.error("Error crawling URL {}", url, e);
        }
        return null; // <-- Return null if it fails
    }
}