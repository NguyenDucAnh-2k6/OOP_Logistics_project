package com.oop.logistics.crawler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NewsCrawlerFactory Tests")
class TestNewsCrawlerFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(TestNewsCrawlerFactory.class);

    @ParameterizedTest
    @ValueSource(strings = {
        "https://thanhnien.vn/some-news-article.html",
        "https://vnexpress.net/another-article.html",
        "https://dantri.com.vn/news.htm",
        "https://tuoitre.vn/breaking-news"
    })
    @DisplayName("Should return correct crawler based on URL domain")
    void testGetCrawlerWithValidUrls(String url) {
        logger.debug("Testing factory with URL: {}", url);
        
        NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
        
        assertNotNull(crawler, "Crawler should be instantiated for valid URL");
        
        // Optional: Check specific instance types based on the URL string
        if (url.contains("thanhnien.vn")) assertTrue(crawler instanceof ThanhNienCrawler);
        if (url.contains("vnexpress.net")) assertTrue(crawler instanceof VnExpressCrawler);
    }

    @Test
    @DisplayName("Should throw exception for unsupported URLs")
    void testGetCrawlerWithInvalidUrl() {
        String badUrl = "https://random-news-site.com/article";
        logger.info("Testing exception handling for unsupported URL: {}", badUrl);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> NewsCrawlerFactory.getCrawler(badUrl)
        );
        
        assertTrue(exception.getMessage().contains("Unsupported news site"));
    }
}