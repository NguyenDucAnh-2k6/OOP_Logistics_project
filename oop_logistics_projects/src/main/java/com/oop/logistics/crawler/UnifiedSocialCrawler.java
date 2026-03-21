// File: src/main/java/com/oop/logistics/crawler/UnifiedSocialCrawler.java
package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class UnifiedSocialCrawler implements SocialCrawler {
    
    public enum Platform { YOUTUBE, TIKTOK, VOZ, REDDIT }
    private final Platform platform;

    public UnifiedSocialCrawler(Platform platform) {
        this.platform = platform;
    }

    @Override
    public List<SocialResult> crawlComments(String url, int maxComments) {
        return switch (platform) {
            case VOZ -> crawlVozJsoup(url, maxComments);
            case YOUTUBE -> crawlYouTubeSelenium(url, maxComments);
            case TIKTOK -> crawlTikTokSelenium(url, maxComments);
            case REDDIT -> crawlRedditSelenium(url, maxComments);
        };
    }

    // 1. VOZ (Static HTML - Fast with Jsoup)
    private List<SocialResult> crawlVozJsoup(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            for (Element post : doc.select("article.message")) {
                if (results.size() >= maxComments) break;
                String author = post.select("a.username").text();
                String content = post.select("div.bbWrapper").text();
                results.add(new SocialResult("VOZ", author, content, 0, java.time.LocalDate.now().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // 2. YouTube (Dynamic - Needs Selenium to scroll)
    private List<SocialResult> crawlYouTubeSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        WebDriver driver = setupWebDriver();
        try {
            driver.get(url);
            
            // Increase timeout to 20 seconds to account for heavy JS loading
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); 
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for the main video player to initialize before scrolling
            Thread.sleep(3000); 

            // Scroll down deep enough to hit the comment section placeholder
            js.executeScript("window.scrollBy(0, 1000);");
            
            // Wait a moment for YouTube to trigger the network request for comments
            Thread.sleep(3000);

            // Now wait for the actual comment renderer tags to appear in the DOM
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("ytd-comment-thread-renderer")));

            int previousSize = 0;
            int retries = 0;

            while (results.size() < maxComments && retries < 3) {
                // Ensure we are selecting elements inside the loaded comment threads
                List<WebElement> comments = driver.findElements(By.cssSelector("ytd-comment-thread-renderer #content-text"));
                List<WebElement> authors = driver.findElements(By.cssSelector("ytd-comment-thread-renderer #author-text span"));
                
                if (comments.isEmpty()) break; 

                for (int i = results.size(); i < comments.size() && results.size() < maxComments; i++) {
                    try {
                        String authorText = authors.get(i).getText().trim();
                        String commentText = comments.get(i).getText().trim();
                        if (!authorText.isEmpty() && !commentText.isEmpty()) {
                            results.add(new SocialResult("YOUTUBE", authorText, commentText, 0, java.time.LocalDate.now().toString()));
                        }
                    } catch (Exception ignored) {
                        // Skip if a specific element goes stale during extraction
                    }
                }

                // Check if we are stuck and no new comments loaded
                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Scroll down to the bottom of the page to load the next batch
                js.executeScript("window.scrollTo(0, document.documentElement.scrollHeight);");
                Thread.sleep(2000); // Wait for DOM to update with new batch
            }
        } catch (Exception e) {
            System.err.println("YouTube Crawl Error: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }
    // 3. TikTok & Reddit stubs (Follow similar Selenium scrolling logic)
    private List<SocialResult> crawlTikTokSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        WebDriver driver = setupWebDriver();
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for the comment section to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-e2e='comment-level-1']")));

            int previousSize = 0;
            int retries = 0;

            while (results.size() < maxComments && retries < 3) {
                List<WebElement> commentBlocks = driver.findElements(By.cssSelector("[data-e2e='comment-level-1']"));
                
                for (int i = results.size(); i < commentBlocks.size() && results.size() < maxComments; i++) {
                    try {
                        WebElement block = commentBlocks.get(i);
                        String author = block.findElement(By.cssSelector("[data-e2e='comment-username-1']")).getText();
                        // The text is usually in a span inside the level-1 block
                        String content = block.findElement(By.cssSelector("p[data-e2e='comment-level-1'] span")).getText(); 
                        
                        results.add(new SocialResult("TIKTOK", author, content, 0, java.time.LocalDate.now().toString()));
                    } catch (Exception ignored) {
                        // Skip if a specific comment fails to parse
                    }
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Scroll the comment container (TikTok scroll is often on the window or a specific div)
                js.executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(2000); // TikTok needs a bit of time to fetch the API and render
            }
        } catch (Exception e) {
            System.err.println("TikTok Crawl Error: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    // 4. Reddit Crawler
    private List<SocialResult> crawlRedditSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        WebDriver driver = setupWebDriver();
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for Reddit's custom web component for comments
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("shreddit-comment")));

            int previousSize = 0;
            int retries = 0;

            while (results.size() < maxComments && retries < 3) {
                List<WebElement> comments = driver.findElements(By.tagName("shreddit-comment"));

                for (int i = results.size(); i < comments.size() && results.size() < maxComments; i++) {
                    try {
                        WebElement comment = comments.get(i);
                        // Extract author directly from the custom element attribute
                        String author = comment.getAttribute("author");
                        
                        // Content is inside the element, usually in paragraphs
                        String content = comment.findElement(By.cssSelector("div[slot='comment']")).getText();
                        
                        if (author != null && content != null && !content.isEmpty()) {
                            results.add(new SocialResult("REDDIT", author, content, 0, java.time.LocalDate.now().toString()));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                js.executeScript("window.scrollBy(0, 2000);");
                Thread.sleep(1500);
            }
        } catch (Exception e) {
            System.err.println("Reddit Crawl Error: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run without opening a visible browser window
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080"); // Add this: Force a large desktop viewport
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"); // Helps bypass some bot detections
        return new ChromeDriver(options);
    }
}