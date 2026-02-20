package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TuoiTreCrawler extends NewsCrawler {

    @Override
    public NewsResult crawl(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            String title = doc.title(); // <-- Get the title

            String date = getMetaContent(doc, "pubdate");
            if (date == null) date = getMetaContent(doc, "article:published_time");
            if (date == null) {
                Element time = doc.selectFirst("span.date");
                if (time != null) date = time.text();
            }
            if (date == null) {
                Element headerDate = doc.selectFirst(".header-content .date");
                if (headerDate != null) date = headerDate.text();
            }
            if (date == null) date = "Unknown";

            StringBuilder text = new StringBuilder();
            for (Element p : doc.select("article.fck_detail p")) {
                text.append(p.text()).append("\n");
            }

            if (text.length() > 50) {
                // <-- RETURN THE DATA INSTEAD OF WRITING TO CSV
                return new NewsResult(url, title, date, text.toString().trim()); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // <-- Return null if it fails
    }
}