package com.oop.logistics.search;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class BingDirectStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BingDirectStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            for (int page = 1; page <= 5; page++) {
                String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
                String url = "https://www.bing.com/search?q=" + encoded + "&first=" + ((page - 1) * 10 + 1);

                logger.info("Bing Debug Chrome searching page {}: {}", page, url);
                driver.get(url);
                Thread.sleep(3000); // Give Bing time to load

                List<WebElement> links = driver.findElements(By.cssSelector("li.b_algo h2 a"));
                if (links.isEmpty()) {
                    logger.info("No more Bing results found on page {}.", page);
                    break;
                }

                for (WebElement result : links) {
                    String href = result.getAttribute("href");
                    if (href != null) {
                        // --- ADDED VISIBILITY LOG ---
                        logger.info(" -> Bing Direct Found URL: {}", href);
                        
                        SearchUtils.processResult(href, domain, null, results);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Bing Debug Selenium Error for domain {} keyword {}", domain, e);
        } finally {
            // CRITICAL: Do NOT call driver.quit() here, or it will close your active Chrome window!
        }
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        // Connect to the existing Chrome instance opened with --remote-debugging-port=9222
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        return new ChromeDriver(options);
    }
}