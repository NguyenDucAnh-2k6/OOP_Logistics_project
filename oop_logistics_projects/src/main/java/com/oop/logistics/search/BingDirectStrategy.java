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
import java.util.HashMap;
public class BingDirectStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BingDirectStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            // We search 5 pages deep (usually enough to exhaust relevant news/social posts)
            for (int page = 1; page <= 5; page++) {
                String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
                String url = "https://www.bing.com/search?q=" + encoded + "&first=" + ((page - 1) * 10 + 1);

                logger.info("Bing Selenium searching page {}: {}", page, url);
                driver.get(url);
                Thread.sleep(3000); // Wait for Bing's JavaScript to render results

                List<WebElement> links = driver.findElements(By.cssSelector("li.b_algo h2 a"));
                if (links.isEmpty()) {
                    logger.info("No more Bing results found on page {}.", page);
                    break;
                }

                for (WebElement result : links) {
                    String href = result.getAttribute("href");
                    if (href != null) {
                        SearchUtils.processResult(href, domain, null, results);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Bing Selenium Error for domain {} keyword {}", domain, e);
        } finally {
            if (driver != null) {
                driver.quit(); // Clean up the browser instance
            }
        }
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        
        // Standard stealth flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");
        
        ChromeDriver driver = new ChromeDriver(options);
        
        // --- THE MAGIC TRICK (CDP) ---
        // This hides the fact that Selenium is running before Bing can detect it
        java.util.Map<String, Object> params = new HashMap<>();
        params.put("source", "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);
        
        return driver;
    }
}