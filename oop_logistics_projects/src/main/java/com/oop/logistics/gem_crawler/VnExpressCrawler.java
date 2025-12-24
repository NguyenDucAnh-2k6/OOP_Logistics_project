package com.oop.logistics.gem_crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class VnExpressCrawler extends NewsCrawler {

    @Override
    public void crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            // 1. Try Meta Tags (VnExpress uses 'pubdate' and 'datePublished')
            String date = getMetaContent(doc, "pubdate");
            
            if (date == null) {
                date = getMetaContent(doc, "article:published_time");
            }

            // 2. Try span.date
            if (date == null) {
                Element time = doc.selectFirst("span.date");
                if (time != null) date = time.text();
            }
            
            // 3. Header content date
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
                writeCsv(date, text.toString().trim());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}