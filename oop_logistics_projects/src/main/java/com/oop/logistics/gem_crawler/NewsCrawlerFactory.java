package com.oop.logistics.gem_crawler;

public class NewsCrawlerFactory {

    public static NewsCrawler getCrawler(String url) {

        if (url.contains("thanhnien.vn")) {
            return new ThanhNienCrawler();
        }

        if (url.contains("vnexpress.net")) {
            return new VnExpressCrawler();
        }

        if (url.contains("dantri.com.vn")) {
            return new DanTriCrawler();
        }

        if (url.contains("tuoitre.vn")) {
            return new TuoiTreCrawler();
        }

        throw new IllegalArgumentException("Unsupported news site: " + url);
    }
}
