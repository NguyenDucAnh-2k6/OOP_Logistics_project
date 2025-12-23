package com.oop.logistics.gem_crawler;

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

            String date = "Unknown";
            Element time = doc.selectFirst("time");
            if (time != null) date = time.attr("datetime");

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
