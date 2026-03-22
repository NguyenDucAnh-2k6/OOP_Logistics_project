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

public class DuckDuckGoStrategy implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DuckDuckGoStrategy.class);

    @Override
    public void search(String domain, String keyword, Map<String, UrlWithDate> results) {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            String encoded = URLEncoder.encode("site:" + domain + " " + keyword, StandardCharsets.UTF_8);
            // Switch from html.duckduckgo to standard duckduckgo for better Selenium rendering
            String url = "https://duckduckgo.com/?q=" + encoded;
            logger.info("DuckDuckGo Selenium searching: {}", url);
            
            driver.get(url);
            Thread.sleep(3000); // Wait for page to load

            // Select the main result links
            List<WebElement> links = driver.findElements(By.cssSelector("a[data-testid='result-title-a']"));
            
            for (WebElement result : links) {
                String href = result.getAttribute("href");
                if (href != null) {
                    String cleanUrl = UrlUtils.extractDDGUrl(href);
                    SearchUtils.processResult(cleanUrl, domain, null, results);
                }
            }
        } catch (Exception e) {
            logger.error("DDG Selenium Error for domain {} keyword {}", domain, e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");
        return new ChromeDriver(options);
    }
}