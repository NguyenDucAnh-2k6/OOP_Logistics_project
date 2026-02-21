package com.oop.logistics.crawler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.NoSuchElementException;

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
    //private String outputCsv = "YagiComments_fixed.csv";
    
    private Set<String> crawledIds = new HashSet<>();
    private int unknownIdCounter = 0;
    private String crawlDate = null; // Date assigned to all comments from this crawl

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
        // Go to a dummy page first to set cookies for the domain
        driver.get("https://www.facebook.com/404");
        driver.manage().addCookie(new Cookie("c_user", c_user));
        driver.manage().addCookie(new Cookie("xs", xs));
        if (fr != null && !fr.isEmpty()) driver.manage().addCookie(new Cookie("fr", fr));
        
        driver.get("https://www.facebook.com/");
        sleep(3000);
    }

    /**
     * Set the date to assign to all comments crawled in this session.
     * Format: dd/mm/yyyy (e.g., "07/09/2024")
     */
    public void setCrawlDate(String date) {
        this.crawlDate = date;
        System.out.println("✓ Crawl date set to: " + date);
    }

    public FacebookResult crawlAndReturn(String url) {
        driver.get(url);
        sleep(5000); 

        boolean isReel = url.contains("/reel/") || driver.getCurrentUrl().contains("/reel/");
        if (isReel) setupReelContext();

        selectFilterMode();

        // No more CSV Overwrite mode here!
        
        return performDeepCrawl(isReel); // Return the result from the deep crawl
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

    private FacebookResult performDeepCrawl(boolean isReel) {
        System.out.println("Starting Deep Crawl...");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int noNewDataCount = 0;
        int totalCollected = 0;
        
        FacebookResult result = new FacebookResult();
        result.content = "Facebook Post/Reel Video"; // Default content text for the main post

        while (noNewDataCount < 20) {
            expandAllVisibleReplies();
            waitForCommentsToStabilize(15);

            // Pass the result object so the scraper can add comments to it
            int newFound = scrapeVisibleComments(result); 
            totalCollected += newFound;

            if (newFound > 0) {
                noNewDataCount = 0;
                System.out.println("\nCollected " + totalCollected);
            } else {
                noNewDataCount++;
                System.out.print(".");
            }

            waitForCommentsToStabilize(10);

            if (isReel) {
                scrollReel(js);
            } else {
                js.executeScript("window.scrollBy(0, 400);");
            }

            clickMainPagination(js);
            waitForCommentsToStabilize(10);
        }

        System.out.println("\nFinished. Total: " + totalCollected);
        return result; // Return the final collected data
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
                // TRY-CATCH BLOCK ADDED HERE
                try {
                    if (btn.isDisplayed()) {
                        String txt = btn.getText();
                        if (txt.contains("Xem") || txt.contains("View") || txt.contains("Show") || txt.contains("replied") || txt.contains("trả lời")) {
                             js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", btn);
                             js.executeScript("arguments[0].click();", btn);
                             foundButton = true;
                             // INCREASED SLEEP: 2s per click to prevent rate limiting
                             sleep(2000); 
                        }
                    }
                } catch (StaleElementReferenceException e) {
                    // Element is gone (DOM updated). We simply skip it and continue.
                    // The next iteration of the outer while-loop will catch any remaining buttons.
                } catch (Exception e) {
                    // Ignore other errors
                }
            }
            if (foundButton) sleep(4000); // Batch load wait
        }
    }

    private int scrapeVisibleComments(FacebookResult result) {
        int count = 0;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> articles = driver.findElements(By.xpath("//div[@role='article']"));

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
                //Extract author
                String author = "Facebook User";
                try {
                    WebElement authorEl = article.findElement(By.xpath(".//span[@dir='auto']//strong | .//h3[@dir='auto'] | .//a[@role='link']/span"));
                    if (authorEl != null) author = authorEl.getText();
                } catch (Exception ignored) {}
                // -----------------------------------------
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

                boolean reply = isReply(article);
                String type = reply ? "Reply" : "Top-level";

                String dateStr = extractDateFromArticle(article);
                String uid = buildUniqueId(commentText, dateStr, type);

                if (crawledIds.contains(uid)) continue;
                crawledIds.add(uid);

                // --- SAVE TO OBJECT INSTEAD OF CSV ---
                // We add the scraped comment to our FacebookResult list
                result.comments.add(new FacebookResult.CommentData(author, commentText, dateStr));
                count++;

            } catch (Exception e) {}
        }
        
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
        // 1️⃣ Find the "a" tag that links to the comment itself. 
        try {
            List<WebElement> links = article.findElements(By.tagName("a"));
            for (WebElement link : links) {
                String href = link.getAttribute("href");
                if (href != null && (href.contains("comment_id=") || href.contains("&cid=") || href.contains("reply_comment_id="))) {
                    String timeText = link.getText().trim();
                    if (!timeText.isEmpty()) {
                        String parsed = parseRelativeTime(timeText.toLowerCase());
                        if (!parsed.equals("Unknown")) return parsed;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2️⃣ Fallback: Look for spans containing time-related Vietnamese keywords
        try {
            WebElement timeSpan = article.findElement(By.xpath(".//span[contains(text(), 'giờ') or contains(text(), 'phút') or contains(text(), 'giây') or contains(text(), 'ngày') or contains(text(), 'hôm qua') or contains(text(), 'tuần') or contains(text(), 'năm')]"));
            if (timeSpan != null) {
                String parsed = parseRelativeTime(timeSpan.getText().trim().toLowerCase());
                if (!parsed.equals("Unknown")) return parsed;
            }
        } catch (Exception ignored) {}

        // 3️⃣ ULTIMATE FALLBACK: Use the user-inputted post date
        if (this.crawlDate != null && !this.crawlDate.isEmpty()) {
            return this.crawlDate;
        }
        
        // Failsafe just in case crawlDate was never set
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String parseRelativeTime(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";

        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            // "vừa xong", "15 phút", "2 giờ"
            if (raw.contains("vừa xong") || raw.contains("phút") || raw.contains("giây") || raw.contains("giờ") || raw.contains("hr") || raw.contains("min") || raw.matches(".*\\d+h.*") || raw.matches(".*\\d+m.*")) {
                return now.format(dtf);
            } 
            // "hôm qua"
            else if (raw.contains("hôm qua") || raw.contains("yesterday")) {
                return now.minusDays(1).format(dtf);
            } 
            // "3 ngày" or "3d"
            else if (raw.contains("ngày") || raw.matches(".*\\d+\\s*d.*")) {
                int d = extractNumber(raw);
                if (d > 0) return now.minusDays(d).format(dtf);
            } 
            // "2 tuần" or "2w"
            else if (raw.contains("tuần") || raw.matches(".*\\d+\\s*w.*")) {
                int w = extractNumber(raw);
                if (w > 0) return now.minusWeeks(w).format(dtf);
            } 
            // "1 năm" or "1y"
            else if (raw.contains("năm") || raw.matches(".*\\d+\\s*y.*")) {
                int y = extractNumber(raw);
                if (y > 0) return now.minusYears(y).format(dtf);
            }
            
            // Explicit dates like "28 tháng 9, 2024", "28 tháng 9", "28/09/2024"
            java.util.regex.Matcher mDate = java.util.regex.Pattern.compile("(\\d{1,2})[\\/\\-\\s]+(?:tháng\\s+)?(\\d{1,2})(?:[\\/\\-\\s,]+(?:năm\\s+)?(\\d{4}))?").matcher(raw);
            if (mDate.find()) {
                int day = Integer.parseInt(mDate.group(1));
                int month = Integer.parseInt(mDate.group(2));
                int year = mDate.group(3) != null ? Integer.parseInt(mDate.group(3)) : now.getYear();
                return String.format("%02d/%02d/%04d", day, month, year);
            }
            
        } catch (Exception e) {}

        // If it failed to parse exactly, return Unknown so the crawler knows to use the crawlDate fallback
        return "Unknown"; 
    }

    private int extractNumber(String s) {
        Matcher m = Pattern.compile("(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
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
            // Broadened XPath to catch <div> buttons and multiple text variations
            List<WebElement> viewMore = driver.findElements(By.xpath(
                "//div[@role='button']//span[contains(text(), 'View more') or contains(text(), 'Xem thêm') or contains(text(), 'bình luận') or contains(text(), 'previous comments') or contains(text(), 'ẩn')]"
            ));
            
            for(WebElement vm : viewMore) {
                if(vm.isDisplayed()) {
                    js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", vm);
                    js.executeScript("arguments[0].click();", vm);
                    // INCREASED SLEEP: 5s for heavy load of hundreds of comments
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

    private boolean isReply(WebElement article) {
        try {
            article.findElement(By.xpath("./ancestor::div[@role='article']"));
            return true; // có cha → reply
        } catch (NoSuchElementException e) {
            return false; // không có cha → top-level
        }
    }

    private String buildUniqueId(String text, String date, String type) {
        return Integer.toHexString(
            Objects.hash(text, date, type)
        );
    }

    private void waitForCommentsToStabilize(int timeoutSec) {
        long start = System.currentTimeMillis();
        int lastCount = -1;

        while ((System.currentTimeMillis() - start) < timeoutSec * 1000L) {
            List<WebElement> articles =
                driver.findElements(By.xpath("//div[@role='article']"));

            int currentCount = articles.size();

            if (currentCount == lastCount) {
                return;
            }

            lastCount = currentCount;
            sleep(1000);
        }
    }
    
    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void tearDown() {
        if (driver != null) driver.quit();
    }
}