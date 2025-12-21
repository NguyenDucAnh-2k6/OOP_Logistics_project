package com.oop.logistics.gem_crawler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FacebookCrawler {
    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;
    private String outputCsv = "YagiComments.csv";
    
    private Set<String> crawledIds = new HashSet<>();
    private int unknownIdCounter = 0;

    public FacebookCrawler() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");
        
        this.driver = new ChromeDriver(options);
        // INCREASED WAIT: 20 seconds for slow elements
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.actions = new Actions(driver);
    }

    public void loginWithCookies(String c_user, String xs, String fr) {
        driver.get("https://www.facebook.com/404");
        driver.manage().addCookie(new Cookie("c_user", c_user));
        driver.manage().addCookie(new Cookie("xs", xs));
        if (fr != null && !fr.isEmpty()) driver.manage().addCookie(new Cookie("fr", fr));
        
        driver.get("https://www.facebook.com/");
        sleep(3000);
    }

    public void crawl(String url) {
        driver.get(url);
        sleep(5000); 

        boolean isReel = url.contains("/reel/") || driver.getCurrentUrl().contains("/reel/");
        if (isReel) setupReelContext();

        selectFilterMode();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsv))) {
            writer.println("Date,Comment Text,Type"); 
        } catch (IOException e) { e.printStackTrace(); }

        performDeepCrawl(isReel);
    }

    private void setupReelContext() {
        try {
            String iconXpath = "//div[@aria-label='Comment' or @aria-label='Bình luận']";
            WebElement commentIcon = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(iconXpath)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", commentIcon);
            sleep(4000); 
        } catch (Exception e) {}
    }

    private void selectFilterMode() {
        try {
            WebElement sortTrigger = driver.findElement(By.xpath("//span[contains(text(), 'Most relevant') or contains(text(), 'Phù hợp nhất')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", sortTrigger);
            sleep(2000);
            WebElement allOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'All comments') or contains(text(), 'Tất cả bình luận')]")));
            allOption.click();
            sleep(5000); // Increased wait for feed reload
        } catch (Exception e) {
            System.out.println("Filter selection skipped.");
        }
    }

    private void performDeepCrawl(boolean isReel) {
        System.out.println("Starting Deep Crawl...");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int noNewDataCount = 0;
        int totalCollected = 0;
        
        while (noNewDataCount < 20) {
            
            // 1. Expand Replies
            expandAllVisibleReplies();
            
            // 2. Scrape Data
            int newFound = scrapeVisibleComments();
            totalCollected += newFound;
            
            if (newFound > 0) {
                System.out.println("\nCollected " + totalCollected + " comments so far...");
                noNewDataCount = 0; 
            } else {
                noNewDataCount++;
                System.out.print("."); 
            }

            // 3. Scroll Logic
            if (isReel) {
               scrollReel(js);
            } else {
                js.executeScript("window.scrollBy(0, 600);"); 
                sleep(500);
                js.executeScript("window.scrollBy(0, -100);");
            }
            
            // INCREASED SLEEP: Give 5 seconds for new comments to render
            sleep(5000); 

            // 4. Click Pagination
            clickMainPagination(js);
        }
        System.out.println("\nFinished. Total: " + totalCollected);
    }

    private void expandAllVisibleReplies() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        boolean foundButton = true;
        int safetyLimit = 0;

        while (foundButton && safetyLimit < 5) { 
            foundButton = false;
            safetyLimit++;

            List<WebElement> replyButtons = driver.findElements(By.xpath(
                "//div[@role='button' or @role='article']//span[" +
                "contains(text(), 'phản hồi') or " +
                "contains(text(), 'replies') or " +
                "contains(text(), 'reply') or " +
                "contains(text(), 'đã trả lời') or " +
                "contains(text(), 'replied')" +
                "]"
            ));

            for (WebElement btn : replyButtons) {
                if (btn.isDisplayed()) {
                    try {
                        String txt = btn.getText();
                        if (txt.contains("Xem") || txt.contains("View") || txt.contains("Show") || txt.contains("replied") || txt.contains("trả lời")) {
                             js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", btn);
                             js.executeScript("arguments[0].click();", btn);
                             foundButton = true;
                             // INCREASED SLEEP: 2s per click to prevent rate limiting
                             sleep(2000); 
                        }
                    } catch (Exception e) {}
                }
            }
            if (foundButton) sleep(4000); // Batch load wait
        }
    }

    private int scrapeVisibleComments() {
        int count = 0;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> articles = driver.findElements(By.xpath("//div[@role='article']"));

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsv, true))) { 
            for (WebElement article : articles) {
                try {
                    expandTruncatedCommentText(js, article);

                    // --- Extract Text ---
                    String commentText = "";
                    List<WebElement> textDivs = article.findElements(By.xpath(".//div[@dir='auto']"));
                    for (WebElement div : textDivs) {
                        if (div.findElements(By.tagName("a")).isEmpty()) {
                            commentText = div.getText().trim();
                            if (!commentText.isEmpty()) break;
                        }
                    }
                    if (commentText.isEmpty()) continue;

                    // --- Extract ID ---
                    String permalink = "";
                    WebElement dateLink = null;
                    List<WebElement> links = article.findElements(By.tagName("a"));
                    for (WebElement link : links) {
                        String href = link.getAttribute("href");
                        if (href != null && (href.contains("comment_id=") || href.contains("&cid="))) {
                            permalink = href;
                            dateLink = link;
                            if (href.contains("comment_id=")) break; 
                        }
                    }

                    if (!permalink.isEmpty()) {
                        if (crawledIds.contains(permalink)) continue; 
                        crawledIds.add(permalink);
                    } else {
                        if (commentText.length() < 2) continue; 
                        unknownIdCounter++;
                    }

                    // --- EXTRACT DATE (ROBUST METHOD) ---
                    
                    String dateStr = extractDateFromArticle(article);


                    // --- Type ---
                    String type = permalink.contains("reply_comment_id") ? "Reply" : "Top-level";

                    writer.println(String.format("\"%s\",\"%s\",\"%s\"", dateStr, commentText.replace("\"", "\"\""), type));
                    count++;

                } catch (Exception e) {}
            }
        } catch (IOException e) { e.printStackTrace(); }
        return count;
    }
    private String normalizeDate(String raw) {
    if (raw == null) return "Unknown";
    raw = raw.toLowerCase();

    // VN: 12 tháng 12 năm 2024
    Pattern vn = Pattern.compile("(\\d{1,2})\\s+tháng\\s+(\\d{1,2})\\s+năm\\s+(\\d{4})");
    Matcher m = vn.matcher(raw);
    if (m.find()) {
        return String.format("%02d-%02d-%s",
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                m.group(3));
    }

    // EN: December 12, 2024
    Pattern en = Pattern.compile("(\\w+)\\s+(\\d{1,2}),\\s*(\\d{4})");
    if (en.matcher(raw).find()) return raw;

    return raw;
}


    // NEW ROBUST DATE EXTRACTOR
    private String extractDateFromArticle(WebElement article) {
    try {
        WebElement abbr = article.findElement(By.xpath(".//abbr[@title]"));
        String fullDate = abbr.getAttribute("title");
        return normalizeDate(fullDate);
    } catch (Exception e) {
        return "Unknown";
        }
    }



    private void expandTruncatedCommentText(JavascriptExecutor js, WebElement article) {
        try {
            List<WebElement> seeMoreBtns = article.findElements(By.xpath(".//div[@role='button'][contains(., 'Xem thêm') or contains(., 'See more')]"));
            if (seeMoreBtns.isEmpty()) {
                seeMoreBtns = article.findElements(By.xpath(".//span[contains(text(), 'Xem thêm') or contains(text(), 'See more')]"));
            }
            for (WebElement btn : seeMoreBtns) {
                if (btn.isDisplayed()) {
                    js.executeScript("arguments[0].click();", btn);
                    sleep(500); 
                }
            }
        } catch (Exception e) {}
    }

    private void clickMainPagination(JavascriptExecutor js) {
        try {
            List<WebElement> viewMore = driver.findElements(By.xpath("//span[contains(text(), 'View more comments') or contains(text(), 'Xem thêm bình luận') or contains(text(), 'Xem các bình luận trước')]"));
            for(WebElement vm : viewMore) {
                if(vm.isDisplayed()) {
                    js.executeScript("arguments[0].click();", vm);
                    // INCREASED SLEEP: 5s for heavy load
                    sleep(5000);
                }
            }
        } catch (Exception e) {}
    }

    private void scrollReel(JavascriptExecutor js) {
        try {
            List<WebElement> articles = driver.findElements(By.xpath("//div[@role='article']"));
            if (!articles.isEmpty()) {
                WebElement last = articles.get(articles.size() - 1);
                js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", last);
            } else {
                js.executeScript("window.scrollBy(0, 500);"); 
            }
        } catch (Exception e) { js.executeScript("window.scrollBy(0, 500);"); }
    }

    private String parseDate(String raw) {
        if (raw == null) return "Unknown";
        try {
            // Updated Regex to handle "Thứ..." prefix if present
            Pattern p = Pattern.compile("(\\d+)\\s+tháng\\s+(\\d+)\\s+năm\\s+(\\d+)");
            Matcher m = p.matcher(raw);
            if (m.find()) {
                return String.format("%02d-%02d-%s", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), m.group(3));
            }
        } catch (Exception e) {}
        return "Unknown";
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void tearDown() {
        if (driver != null) driver.quit();
    }
}