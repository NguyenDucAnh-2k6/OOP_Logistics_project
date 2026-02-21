package com.oop.logistics.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewsCrawlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NewsCrawlerFactory.class);

    public static NewsCrawler getCrawler(String url) {

        if (url.contains("thanhnien.vn")) {
            logger.debug("Creating ThanhNienCrawler for {}", url);
            return new ThanhNienCrawler();
        }

        if (url.contains("vnexpress.net")) {
            logger.debug("Creating VnExpressCrawler for {}", url);
            return new VnExpressCrawler();
        }

        if (url.contains("dantri.com.vn")) {
            logger.debug("Creating DanTriCrawler for {}", url);
            return new DanTriCrawler();
        }

        if (url.contains("tuoitre.vn")) {
            logger.debug("Creating TuoiTreCrawler for {}", url);
            return new TuoiTreCrawler();
        }

        throw new IllegalArgumentException("Unsupported news site: " + url);
    }
}
