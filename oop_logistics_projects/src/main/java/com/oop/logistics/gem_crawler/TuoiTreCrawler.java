package com.oop.logistics.gem_crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TuoiTreCrawler extends NewsCrawler {

    @Override
    public void crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            // 1. Try Meta Tag
            String date = getMetaContent(doc, "article:published_time");

            // 2. Try specific classes
            if (date == null) {
                Element time = doc.selectFirst("div.date-time");
                if (time != null) date = time.text();
            }
            
            // 3. Generic time tag
            if (date == null) {
                Element time = doc.selectFirst("time");
                if (time != null) date = time.text();
            }

            if (date == null) date = "Unknown";

            StringBuilder text = new StringBuilder();
            for (Element p : doc.select("div#main-detail-body p")) {
                text.append(p.text()).append("\n");
            }

            if (text.length() > 50) {
                writeCsv(date, text.toString().trim());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}