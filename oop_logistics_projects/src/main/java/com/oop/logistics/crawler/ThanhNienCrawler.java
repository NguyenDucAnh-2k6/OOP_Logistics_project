package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ThanhNienCrawler extends NewsCrawler {

    @Override
    public void crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            // 1. Try Meta Tag (Standard ISO format)
            String date = getMetaContent(doc, "article:published_time");
            
            // 2. Try time tag attribute
            if (date == null) {
                Element time = doc.selectFirst("time");
                if (time != null) {
                    // Prefer datetime attribute, fallback to text
                    date = time.hasAttr("datetime") ? time.attr("datetime") : time.text();
                }
            }
            
            // 3. Try detail-time div
            if (date == null) {
                 Element detailTime = doc.selectFirst("div.detail-time");
                 if (detailTime != null) date = detailTime.text();
            }

            if (date == null) date = "Unknown";

            StringBuilder text = new StringBuilder();
            for (Element p : doc.select("div.detail-content p")) {
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