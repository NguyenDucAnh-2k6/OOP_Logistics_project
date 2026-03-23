package com.oop.logistics.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleNewsRssStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(GoogleNewsRssStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            String encoded = URLEncoder.encode(keyword + " site:" + domain, StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=vi&gl=VN&ceid=VN:vi";
            logger.info("Google News RSS (Debug Chrome): {}", rssUrl);

            // 1. Load the RSS feed in the trusted debug browser
            driver.get(rssUrl);
            Thread.sleep(3000); // Wait for Chrome to render the XML

            // 2. Extract the raw XML from Chrome's DOM
            String pageSource = driver.getPageSource();
            
            // 3. Pass the unblocked XML string directly into Jsoup for standard parsing
            Document doc = Jsoup.parse(pageSource);
            
            for (Element item : doc.select("item")) {
                String url = UrlUtils.extractActualUrl(item.select("link").text());
                String pubDate = item.select("pubDate").text();
                
                // --- ADDED VISIBILITY LOG ---
                logger.info(" -> Google News Found URL: {}", url);
                
                SearchUtils.processResult(url, domain, pubDate, results);
            }
        } catch (Exception e) {
            logger.error("Google News Debug Error for domain {} keyword {}", domain, e);
        } finally {
            // CRITICAL: Do NOT call driver.quit() here!
        }
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        return new ChromeDriver(options);
    }
}