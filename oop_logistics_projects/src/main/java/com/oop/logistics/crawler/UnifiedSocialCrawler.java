// File: src/main/java/com/oop/logistics/crawler/UnifiedSocialCrawler.java
package com.oop.logistics.crawler;

import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UnifiedSocialCrawler implements SocialCrawler {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedSocialCrawler.class);
    private final Platform platform;

    public UnifiedSocialCrawler(Platform platform) {
        this.platform = platform;
    }

    public enum Platform { YOUTUBE, TIKTOK, VOZ, REDDIT, TWITTER, FACEBOOK, INSTAGRAM, THREADS }

    @Override
    public List<SocialResult> crawlComments(String url, int maxComments) {
        return switch (platform) {
            case VOZ -> crawlVozSelenium(url, maxComments);
            case YOUTUBE -> crawlYouTubeSelenium(url, maxComments);
            case TIKTOK -> crawlTikTokSelenium(url, maxComments);
            case REDDIT -> crawlRedditJson(url, maxComments);
            case TWITTER -> crawlTwitterSelenium(url, maxComments);
            case FACEBOOK -> crawlFacebookSelenium(url, maxComments);
            case INSTAGRAM -> crawlInstagramSelenium(url, maxComments);
            case THREADS -> crawlThreadsSelenium(url, maxComments); // <--- Add here
        };
    }
    // 1. VOZ (Using Selenium to bypass Cloudflare 403 and handle pagination)
    private List<SocialResult> crawlVozSelenium(String baseUrl, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        
        if (baseUrl == null || !baseUrl.startsWith("http")) {
            logger.warn("Invalid VOZ URL provided: {}", baseUrl);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            int currentPage = 1;
            String currentUrl = baseUrl;

            while (results.size() < maxComments) {
                logger.info("Fetching VOZ page {} from: {}", currentPage, currentUrl);
                driver.get(currentUrl);
                
                // Wait for posts to load (Cloudflare's JS check might take a few seconds)
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article.message")));
                } catch (Exception e) {
                    logger.warn("Timeout waiting for VOZ posts on page {}. Cloudflare might be blocking the headless browser.", currentPage);
                    break; // Stop crawling if we can't load the page
                }

                List<WebElement> posts = driver.findElements(By.cssSelector("article.message"));
                if (posts.isEmpty()) break;

                for (WebElement post : posts) {
                    if (results.size() >= maxComments) break;
                    try {
                        String author = post.findElement(By.cssSelector("a.username")).getText().trim();
                        String content = post.findElement(By.cssSelector("div.bbWrapper")).getText().trim();
                        
                        if (!author.isEmpty() && !content.isEmpty()) {
                            // Date extraction
                            String date = java.time.LocalDate.now().toString();
                            try {
                                WebElement timeElement = post.findElement(By.cssSelector("time.u-dt"));
                                String datetime = timeElement.getAttribute("datetime");
                                if (datetime != null) {
                                    date = datetime.split("T")[0];
                                }
                            } catch (Exception ignored) {} // Time element not found, use default

                            results.add(new SocialResult("VOZ", author, content, 0, date));
                        }
                    } catch (Exception ignored) {} // Skip malformed posts
                }

                if (results.size() >= maxComments) break;

                // --- Pagination Logic ---
                try {
                    // VOZ uses XenForo, which standardizes the "Next" button with this class
                    WebElement nextButton = driver.findElement(By.cssSelector("a.pageNav-jump--next"));
                    currentUrl = nextButton.getAttribute("href");
                    currentPage++;
                    
                    // Slight pause to behave nicely and avoid getting IP banned
                    Thread.sleep(2000); 
                } catch (Exception e) {
                    // No "Next" button found, meaning we've reached the last page of the thread
                    logger.info("No more pages found on VOZ. Reached end of thread.");
                    break;
                }
            }
            logger.info("Successfully crawled {} comments from VOZ.", results.size());
            
        } catch (Exception e) {
            logger.error("VOZ Selenium Crawl Error for URL [{}]: {}", baseUrl, e.getMessage(), e);
        } finally {
            driver.quit(); // Always ensure the browser closes
        }
        return results;
    }
    // 6. Facebook Crawler (Using Chrome Debug Mode)
    private List<SocialResult> crawlFacebookSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>();
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid Facebook URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching Facebook Post from: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for the main post to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); 
            // --- 0. CLICK REEL/VIDEO COMMENT BUTTON ---
            // If the URL is a Reel or Facebook Watch video, the comments are usually hidden behind a button
            if (url.contains("/reel/") || url.contains("/videos/") || url.contains("/watch/")) {
                logger.info("Detected Reel/Video. Attempting to open the comment sidebar...");
                try {
                    js.executeScript(
                        "var btns = document.querySelectorAll('div[role=\"button\"], span[role=\"button\"], div[aria-label]');" +
                        "for (var i = 0; i < btns.length; i++) {" +
                        "   var label = btns[i].getAttribute('aria-label') ? btns[i].getAttribute('aria-label').toLowerCase() : '';" +
                        "   if (label.includes('bình luận') || label.includes('comment') || label.includes('leave a comment')) {" +
                        "       btns[i].click();" +
                        "       return;" + // Stop searching after clicking the first match
                        "   }" +
                        "}"
                    );
                    Thread.sleep(3000); // Wait for the sidebar animation to slide out
                } catch (Exception e) {
                    logger.warn("Could not find or click the Reel comment button.");
                }
            }

            // --- 1. FILTER TO "TẤT CẢ BÌNH LUẬN" (ALL COMMENTS) ---
            logger.info("Attempting to switch comment filter to 'All Comments'...");
            // --- 1. FILTER TO "TẤT CẢ BÌNH LUẬN" (ALL COMMENTS) ---
            logger.info("Attempting to switch comment filter to 'All Comments'...");
            try {
                js.executeScript(
                    "var spans = document.querySelectorAll('span, div');" +
                    "spans.forEach(s => {" +
                    "   var text = s.innerText ? s.innerText.toLowerCase() : '';" +
                    "   if (text === 'phù hợp nhất' || text === 'most relevant') {" +
                    "       s.click();" +
                    "   }" +
                    "});"
                );
                Thread.sleep(1500); // Wait for the dropdown to open
                
                js.executeScript(
                    "var options = document.querySelectorAll('span, div[role=\"menuitem\"]');" +
                    "options.forEach(o => {" +
                    "   var text = o.innerText ? o.innerText.toLowerCase() : '';" +
                    "   if (text.includes('tất cả bình luận') || text.includes('all comments')) {" +
                    "       o.click();" +
                    "   }" +
                    "});"
                );
                Thread.sleep(3000); // Wait for comments to reload
            } catch (Exception e) {
                logger.warn("Could not change comment filter. Facebook UI might have changed or it's already set.");
            }

            int previousSize = 0;
            int retries = 0;

            // --- 2. EXTRACTION LOOP ---
            while (results.size() < maxComments && retries < 4) {
                
                // Click "Xem thêm phản hồi" (View Replies) and "Xem thêm" (See More text)
                try {
                    js.executeScript(
                        "var btns = document.querySelectorAll('div[role=\"button\"], span');" +
                        "btns.forEach(b => {" +
                        "   var text = b.innerText ? b.innerText.toLowerCase() : '';" +
                        "   if(text.includes('phản hồi') || text.includes('replies') || text.includes('xem thêm') || text.includes('bình luận khác') || text.includes('view more')) {" +
                        "       b.click();" +
                        "   }" +
                        "});"
                    );
                    Thread.sleep(2000); 
                } catch (Exception ignored) {}

                // Facebook groups comments inside elements with role="article"
                List<WebElement> articles = driver.findElements(By.cssSelector("div[role='article']"));
                
                for (WebElement article : articles) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        // Extract Author (Usually the first link or span with text inside the article)
                        String author = "Unknown";
                        try {
                            // Target specific span structure Facebook uses for names
                            author = article.findElement(By.cssSelector("span[dir='auto'] > span:first-child, a[role='link'] span[dir='auto']")).getText().trim();
                        } catch (Exception ignored) {}

                        // Extract Content
                        String content = "";
                        try {
                            // Find the main text block, avoiding the author name span
                            List<WebElement> textBlocks = article.findElements(By.cssSelector("div[dir='auto']"));
                            for (WebElement block : textBlocks) {
                                String text = block.getText().trim();
                                if (!text.isEmpty() && !text.equals(author)) {
                                    content = text;
                                    break;
                                }
                            }
                        } catch (Exception e) { continue; }

                        // Extract Date
                        String dateStr = java.time.LocalDate.now().toString();
                        try {
                            // Facebook usually places timestamps in links under the comment.
                            // The aria-label often contains the exact hover date (e.g., "12 Tháng 3, 2026").
                            WebElement timeLink = article.findElement(By.cssSelector("a[role='link'] span:contains('tuần'), a[role='link']:last-of-type, ul > li:last-child"));
                            String rawDateText = timeLink.getAttribute("innerText");
                            
                            // If there's an aria-label or tooltip, it's the exact date. Otherwise, we parse "1 tuần"
                            String hoverText = timeLink.getAttribute("aria-label");
                            if (hoverText != null && !hoverText.isEmpty()) {
                                rawDateText = hoverText; 
                            }
                            
                            dateStr = parseFacebookDate(rawDateText);
                        } catch (Exception ignored) {}

                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");

                        if (!author.isEmpty() && !content.isEmpty() && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("FACEBOOK", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Scroll down
                js.executeScript("window.scrollBy(0, 1500);");
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} comments from Facebook.", results.size());
            
        } catch (Exception e) {
            logger.error("Facebook Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        }
        return results;
    }
    // --- HELPER METHOD: Parses Facebook's Hover and Relative Dates ---
    private String parseFacebookDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return java.time.LocalDate.now().toString();
        }
        
        rawDate = rawDate.trim().toLowerCase();
        java.time.LocalDate date = java.time.LocalDate.now();
        
        try {
            // 1. Check for Exact Hover Date Formats (e.g., "12 tháng 3, 2026" or "12 tháng 3")
            if (rawDate.contains("tháng")) {
                // Remove the word 'lúc' and times (e.g. "lúc 14:30")
                rawDate = rawDate.replaceAll("lúc.*", "").trim(); 
                String[] parts = rawDate.replace(",", "").split(" ");
                
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[2]);
                int year = date.getYear(); // Default to current year
                
                // If year is present at the end
                if (parts.length > 3) {
                    year = Integer.parseInt(parts[3]);
                }
                return String.format("%04d-%02d-%02d", year, month, day);
            }
            
            // 2. Fallback: Parse Relative Dates ("1 tuần", "2 ngày", "5h")
            if (rawDate.contains("vừa xong") || rawDate.contains("phút") || rawDate.contains("m") || rawDate.contains("giờ") || rawDate.contains("h")) {
                return date.toString(); // Happened today
            }
            
            String numberStr = rawDate.replaceAll("[^0-9]", ""); 
            if (!numberStr.isEmpty()) {
                int amount = Integer.parseInt(numberStr);
                if (rawDate.contains("ngày") || rawDate.contains("d")) date = date.minusDays(amount);
                else if (rawDate.contains("tuần") || rawDate.contains("w")) date = date.minusWeeks(amount);
                else if (rawDate.contains("tháng")) date = date.minusMonths(amount);
                else if (rawDate.contains("năm") || rawDate.contains("y")) date = date.minusYears(amount);
            }
        } catch (Exception e) {
            // Return current date if parsing completely fails
        }
        
        return date.toString();
    }
    // 2. YouTube (Dynamic - Needs Selenium to scroll)
    private List<SocialResult> crawlYouTubeSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>(); // Tracks unique comments to prevent duplicates
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid YouTube URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching YouTube Video from: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); 
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. Wait for the main player to initialize
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000); 

            // 2. PROGRESSIVE SCROLLING: Scroll down to trigger comment section
            logger.info("Scrolling down to trigger YouTube comments...");
            for (int i = 0; i < 4; i++) {
                js.executeScript("window.scrollBy(0, 600);");
                Thread.sleep(1000); 
            }

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("ytd-comment-thread-renderer")));
            } catch (Exception e) {
                logger.warn("Timeout: Comments section never loaded. The video might have comments disabled.");
                return results; 
            }

            int previousSize = 0;
            int retries = 0;

            // 3. Extract comments loop
            while (results.size() < maxComments && retries < 4) {
                
                // --- EXPAND BUTTONS (Using JS to avoid 'Element Not Clickable' errors) ---
                try {
                    // Click "Read more" on long comments
                    js.executeScript("document.querySelectorAll('ytd-comment-renderer tp-yt-paper-button#more').forEach(b => { if(b.offsetParent != null) b.click(); });");
                    
                    // Click "View X replies" (YouTube buttons frequently change classes, this selector catches them)
                    js.executeScript("document.querySelectorAll('#replies ytd-button-renderer button, #replies yt-button-shape button').forEach(b => { if(b.offsetParent != null) b.click(); });");
                    
                    Thread.sleep(2000); // Give the network time to fetch the replies
                } catch (Exception ignored) {}

                // --- EXTRACTION ---
                // Select both the old renderer AND the new view-model tags to survive YouTube A/B testing
                List<WebElement> commentBlocks = driver.findElements(By.cssSelector("ytd-comment-renderer, ytd-comment-view-model"));
                
                for (WebElement block : commentBlocks) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        // Use getAttribute("innerText") instead of getText() to bypass Selenium's strict animation/visibility rules
                        String author = block.findElement(By.cssSelector("#author-text")).getAttribute("innerText").trim();
                        String content = block.findElement(By.cssSelector("#content-text")).getAttribute("innerText").trim();
                        
                        // Extract Date
                        String dateStr = java.time.LocalDate.now().toString();
                        try {
                            String relativeDate = block.findElement(By.cssSelector("#published-time-text")).getAttribute("innerText").trim();
                            dateStr = parseRelativeDate(relativeDate);
                        } catch (Exception ignored) {}

                        // Clean up newlines for the unique ID to prevent slight DOM shifts from duplicating comments
                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");
                        
                        // We check for length > 0 to ensure we aren't grabbing empty skeleton loading blocks
                        if (author.length() > 0 && content.length() > 0 && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("YOUTUBE", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {
                        // Skip if a specific element is missing (e.g., a skeleton loading placeholder)
                    }
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Scroll to the bottom to load the next batch
                js.executeScript("window.scrollTo(0, document.documentElement.scrollHeight);");
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} comments (including replies) from YouTube.", results.size());
            
        } catch (Exception e) {
            logger.error("YouTube Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        return results;
    }
    // 3. TikTok (Dynamic - Uses data-e2e attributes and targets specific scrollable containers)
    // 3. TikTok (Dynamic - Extracts top comments, replies, and parses localized dates)
    private List<SocialResult> crawlTikTokSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>();
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid TikTok URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching TikTok Video from: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); 

            // --- UPGRADED LOGIC: CLICK THE COMMENTS BUTTON ---
            logger.info("Attempting to open the TikTok comments panel...");
            try {
                String jsScript = 
                    "var commentsExist = document.querySelector('[data-e2e=\"comment-level-1\"]'); " +
                    "if (commentsExist && commentsExist.offsetParent != null) { " +
                    "    return 'already_open'; " +
                    "} " +
                    "var selectors = [ " +
                    "    '[data-e2e=\"comment-icon\"]', " +
                    "    '[data-e2e=\"browse-comment-icon\"]', " +
                    "    '[data-e2e=\"feed-comment-icon\"]', " +
                    "    'button[aria-label*=\"comment\" i]', " +
                    "    'div[aria-label*=\"comment\" i]' " +
                    "]; " +
                    "for (var i = 0; i < selectors.length; i++) { " +
                    "    var btn = document.querySelector(selectors[i]); " +
                    "    if (btn) { " +
                    "        btn.click(); " +
                    "        return 'clicked_selector_' + i; " +
                    "    } " +
                    "} " +
                    "return 'not_found'; ";

                String result = (String) js.executeScript(jsScript);
                
                if ("already_open".equals(result)) {
                    logger.info("TikTok comments panel is already visible. Proceeding to extraction.");
                } else if (result != null && result.startsWith("clicked")) {
                    logger.info("Clicked TikTok comments button using fallback strategy. Waiting for animation...");
                    Thread.sleep(3000); 
                }
            } catch (Exception e) {
                logger.error("Error executing TikTok comment button script: {}", e.getMessage());
            }

            int previousSize = 0;
            int retries = 0;

            // 2. Extraction Loop
            while (results.size() < maxComments && retries < 4) {
                
                // --- CLICK "VIEW REPLIES" BUTTONS ---
                try {
                    // Clicks buttons that contain words like "reply", "replies", "trả lời", or "xem thêm"
                    js.executeScript(
                        "var replyBtns = document.querySelectorAll('div[role=\"button\"], p, span');" +
                        "replyBtns.forEach(b => {" +
                        "   var text = b.innerText ? b.innerText.toLowerCase() : '';" +
                        "   if(text.includes('reply') || text.includes('replies') || text.includes('trả lời') || text.includes('xem thêm')) {" +
                        "       b.click();" +
                        "   }" +
                        "});"
                    );
                    Thread.sleep(1500); // Give replies time to load into the DOM
                } catch (Exception ignored) {}

                // Grab BOTH top-level comments and sub-replies
                List<WebElement> comments = driver.findElements(By.cssSelector("[data-e2e='comment-level-1'], [data-e2e='comment-level-2']"));
                
                for (WebElement block : comments) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        String author = "Unknown";
                        try {
                            author = block.findElement(By.cssSelector("[data-e2e='comment-username-1'], [data-e2e='comment-username-2']")).getAttribute("innerText").trim();
                        } catch (Exception ignored) {}

                        String content = "";
                        try {
                            content = block.findElement(By.cssSelector("p[data-e2e='comment-level-1'], p[data-e2e='comment-level-2'], span")).getAttribute("innerText").trim();
                        } catch (Exception e) {
                            continue; 
                        }

                        // Extract and parse the Date
                        String dateStr = java.time.LocalDate.now().toString();
                        try {
                            String rawDate = block.findElement(By.cssSelector("[data-e2e='comment-time-1'], [data-e2e='comment-time-2'], span[class*='SpanCreatedTime']")).getAttribute("innerText").trim();
                            dateStr = parseTikTokDate(rawDate);
                        } catch (Exception ignored) {}

                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");

                        if (!author.isEmpty() && !content.isEmpty() && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("TIKTOK", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // 3. TIKTOK SCROLLING LOGIC
                try {
                    js.executeScript("window.scrollBy(0, 1000);");
                    js.executeScript("var containers = document.querySelectorAll('[class*=\"DivCommentListContainer\"], [class*=\"comment-container\"]'); " +
                                     "containers.forEach(c => c.scrollBy(0, 1000));");
                } catch (Exception ignored) {}
                
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} comments from TikTok.", results.size());
            
        } catch (Exception e) {
            logger.error("TikTok Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        } finally {
            if (driver != null) {
                // driver.quit(); // Keep commented out if using the debugging port 9222 trick
            }
        }
        return results;
    }

    // --- HELPER METHOD: Parses TikTok's specific date formats (e.g., "2024-9-7", "9-7", "1w ago") ---
    // --- HELPER METHOD: Parses TikTok's specific date formats ---
    private String parseTikTokDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return java.time.LocalDate.now().toString();
        }
        
        // 1. AGGRESSIVE SANITIZATION: Remove hidden UI text that TikTok bundles with the date
        rawDate = rawDate.toLowerCase()
                .replace("reply", "")
                .replace("trả lời", "")
                .replace("·", "")
                .replace("liked by creator", "")
                .replace("\n", "")
                .trim();
        
        // 2. Handle exact format like "2024-9-7" -> "2024-09-07"
        if (rawDate.matches(".*\\d{4}-\\d{1,2}-\\d{1,2}.*")) {
            try {
                // Extract just the date part in case there's leftover text
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}").matcher(rawDate);
                if (m.find()) {
                    String[] parts = m.group().split("-");
                    return String.format("%04d-%02d-%02d", 
                        Integer.parseInt(parts[0]), 
                        Integer.parseInt(parts[1]), 
                        Integer.parseInt(parts[2]));
                }
            } catch (Exception e) { return rawDate; }
        }
        
        // 3. Handle format like "9-7" (Assumes current year)
        if (rawDate.matches(".*\\d{1,2}-\\d{1,2}.*")) {
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{1,2}-\\d{1,2}").matcher(rawDate);
                if (m.find()) {
                    String[] parts = m.group().split("-");
                    int currentYear = java.time.LocalDate.now().getYear();
                    return String.format("%04d-%02d-%02d", 
                        currentYear, 
                        Integer.parseInt(parts[0]), 
                        Integer.parseInt(parts[1]));
                }
            } catch (Exception e) { return rawDate; }
        }

        // 4. Handle relative dates ("1d", "2w", "1 ngày trước")
        java.time.LocalDate date = java.time.LocalDate.now();
        try {
            if (rawDate.contains("vừa xong") || rawDate.contains("just now") || rawDate.contains("phút") || rawDate.contains("giờ") || rawDate.contains("h ") || rawDate.contains("m ")) {
                return date.toString(); // Happened today
            }
            
            String numberStr = rawDate.replaceAll("[^0-9]", ""); 
            if (!numberStr.isEmpty()) {
                int amount = Integer.parseInt(numberStr);
                if (rawDate.contains("d") || rawDate.contains("ngày")) date = date.minusDays(amount);
                else if (rawDate.contains("w") || rawDate.contains("tuần")) date = date.minusWeeks(amount);
                else if (rawDate.contains("m") || rawDate.contains("tháng")) date = date.minusMonths(amount);
                else if (rawDate.contains("y") || rawDate.contains("năm")) date = date.minusYears(amount);
            }
        } catch (Exception ignored) {}
        
        return date.toString();
    }
    
    // 4. Reddit Crawler (Using JSON backdoor - Handles both Subreddits and Posts)
    private List<SocialResult> crawlRedditJson(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        
        // 1. Basic Validation to prevent malformed URL crashes
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid Reddit URL provided: {}", url);
            return results;
        }

        // 2. Append .json to the URL
        String jsonUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        if (!jsonUrl.endsWith(".json")) {
            jsonUrl += ".json";
        }

        try {
            logger.info("Fetching Reddit JSON from: {}", jsonUrl);
            
            // 3. Fetch the JSON data using Jsoup
            String jsonResponse = Jsoup.connect(jsonUrl)
                    .userAgent("java:com.oop.logistics.crawler:v1.0 (by /u/student_dev)") 
                    .header("Accept", "application/json")
                    .ignoreContentType(true) 
                    .timeout(15000)
                    .execute()
                    .body();

            // 4. Parse the JSON flexibly
            JsonElement rootElement = JsonParser.parseString(jsonResponse);
            JsonArray itemsArray = new JsonArray();

            if (rootElement.isJsonArray()) {
                // Scenario A: It's a POST URL. Root is an array. [1] contains the comments.
                JsonArray rootArray = rootElement.getAsJsonArray();
                if (rootArray.size() > 1) {
                    itemsArray = rootArray.get(1).getAsJsonObject()
                            .getAsJsonObject("data")
                            .getAsJsonArray("children");
                }
            } else if (rootElement.isJsonObject()) {
                // Scenario B: It's a SUBREDDIT URL. Root is an object containing posts.
                itemsArray = rootElement.getAsJsonObject()
                        .getAsJsonObject("data")
                        .getAsJsonArray("children");
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 5. Extract the data
            for (JsonElement elem : itemsArray) {
                if (results.size() >= maxComments) break;

                JsonObject itemData = elem.getAsJsonObject().getAsJsonObject("data");
                
                // For comments it's "body", for subreddit posts it's "title" and "selftext"
                String content = "";
                if (itemData.has("body") && !itemData.get("body").isJsonNull()) {
                    content = itemData.get("body").getAsString(); // Extract comment
                } else if (itemData.has("title") && !itemData.get("title").isJsonNull()) {
                    content = itemData.get("title").getAsString(); // Extract post title
                    if (itemData.has("selftext") && !itemData.get("selftext").isJsonNull() && !itemData.get("selftext").getAsString().trim().isEmpty()) {
                        content += "\n" + itemData.get("selftext").getAsString(); // Add post body if it exists
                    }
                }

                // If we found valid text content, create the SocialResult
                if (!content.trim().isEmpty()) {
                    String author = itemData.has("author") ? itemData.get("author").getAsString() : "Unknown";
                    int likes = itemData.has("ups") ? itemData.get("ups").getAsInt() : 0;
                    
                    // Convert Unix epoch time
                    String date = "Unknown Date";
                    if (itemData.has("created_utc")) {
                        long epochTime = itemData.get("created_utc").getAsLong();
                        date = Instant.ofEpochSecond(epochTime)
                                .atZone(ZoneId.systemDefault())
                                .format(formatter);
                    }

                    results.add(new SocialResult("REDDIT", author, content.trim(), likes, date));
                }
            }
            
            logger.info("Successfully crawled {} items from Reddit.", results.size());
            
        } catch (Exception e) {
            logger.error("Reddit JSON Crawl Error for URL [{}]: {}", jsonUrl, e.getMessage(), e);
        }
        return results;
    }
    private String parseRelativeDate(String relativeDateStr) {
        if (relativeDateStr == null || relativeDateStr.isEmpty()) {
            return java.time.LocalDate.now().toString();
        }
        
        java.time.LocalDate date = java.time.LocalDate.now();
        // Clean up the string (supports both English and Vietnamese browsers)
        relativeDateStr = relativeDateStr.toLowerCase().replace("trước", "").replace("ago", "").trim(); 
        
        try {
            String[] parts = relativeDateStr.split(" ");
            if (parts.length >= 2) {
                int amount = Integer.parseInt(parts[0]);
                String unit = parts[1];
                
                if (unit.contains("năm") || unit.contains("year")) date = date.minusYears(amount);
                else if (unit.contains("tháng") || unit.contains("month")) date = date.minusMonths(amount);
                else if (unit.contains("tuần") || unit.contains("week")) date = date.minusWeeks(amount);
                else if (unit.contains("ngày") || unit.contains("day")) date = date.minusDays(amount);
            }
        } catch (Exception e) {
            // Keep current date if parsing fails
        }
        return date.toString();
    }
    // 5. Twitter / X Crawler
    private List<SocialResult> crawlTwitterSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>();
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid Twitter URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching Twitter URL: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for the main tweet container to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); 

            int previousSize = 0;
            int retries = 0;

            while (results.size() < maxComments && retries < 4) {
                
                // --- CLICK "SHOW REPLIES" BUTTONS ---
                try {
                    js.executeScript(
                        "var buttons = document.querySelectorAll('div[role=\"button\"]');" +
                        "buttons.forEach(b => {" +
                        "   var text = b.innerText ? b.innerText.toLowerCase() : '';" +
                        "   if(text.includes('show replies') || text.includes('show more replies') || text.includes('hiển thị thêm') || text.includes('show probable spam')) {" +
                        "       b.click();" +
                        "   }" +
                        "});"
                    );
                    Thread.sleep(1500); // Give the replies time to load into the DOM
                } catch (Exception ignored) {}

                // Grab all tweet articles on the screen (this captures the main tweet AND all comments)
                List<WebElement> tweets = driver.findElements(By.cssSelector("article[data-testid='tweet']"));
                
                for (WebElement tweet : tweets) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        // 1. Extract Author (Usually found in a data-testid="User-Name" block)
                        String author = "Unknown";
                        try {
                            // Twitter's User-Name block contains Name \n @username \n Date. We split to get just the Name.
                            author = tweet.findElement(By.cssSelector("[data-testid='User-Name']")).getText().split("\n")[0].trim();
                        } catch (Exception ignored) {}

                        // 2. Extract Text
                        String content = "";
                        try {
                            content = tweet.findElement(By.cssSelector("[data-testid='tweetText']")).getAttribute("innerText").trim();
                        } catch (Exception e) {
                            continue; // Skip if it's a media-only tweet (picture/video with no text)
                        }

                        // 3. Extract Date (Bypassing the hover trick by grabbing the exact ISO string)
                        String dateStr = java.time.LocalDate.now().toString();
                        try {
                            WebElement timeEl = tweet.findElement(By.cssSelector("time"));
                            String datetime = timeEl.getAttribute("datetime"); // e.g., "2026-03-22T14:30:00.000Z"
                            if (datetime != null && datetime.length() >= 10) {
                                dateStr = datetime.substring(0, 10); // Extract just the YYYY-MM-DD portion
                            }
                        } catch (Exception ignored) {}

                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");

                        if (!author.isEmpty() && !content.isEmpty() && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("TWITTER", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Scroll down to trigger the next batch of comments
                js.executeScript("window.scrollBy(0, 1500);");
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} tweets/replies from Twitter.", results.size());
            
        } catch (Exception e) {
            logger.error("Twitter Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        }
        return results;
    }
    // 7. Instagram Crawler
    private List<SocialResult> crawlInstagramSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>();
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid Instagram URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching Instagram Post: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); 

            // --- 1. CLICK COMMENT BUTTON OR "VIEW ALL COMMENTS" ---
            try {
                // Look for the Speech Bubble SVG (from your image) or the "View all X comments" text link
                js.executeScript(
                    "var svgs = document.querySelectorAll('svg[aria-label=\"Comment\"], svg[aria-label=\"Bình luận\"]');" +
                    "if (svgs.length > 0) { " +
                    "   var btn = svgs[0].closest('div[role=\"button\"], button, a'); " +
                    "   if (btn) btn.click(); " +
                    "} else {" +
                    "   var spans = document.querySelectorAll('span, div');" +
                    "   for(var i=0; i<spans.length; i++) {" +
                    "       var t = spans[i].innerText ? spans[i].innerText.toLowerCase() : '';" +
                    "       if ((t.includes('view all') && t.includes('comments')) || (t.includes('xem tất cả') && t.includes('bình luận'))) {" +
                    "           spans[i].click(); return;" +
                    "       }" +
                    "   }" +
                    "}"
                );
                Thread.sleep(3000); 
            } catch (Exception ignored) {}

            int previousSize = 0;
            int retries = 0;

            // --- 2. EXTRACTION LOOP ---
            while (results.size() < maxComments && retries < 4) {
                
                // --- CLICK "VIEW ALL {n} REPLIES" ---
                try {
                    js.executeScript(
                        "var elements = document.querySelectorAll('span, div[role=\"button\"]');" +
                        "elements.forEach(el => {" +
                        "   var t = el.innerText ? el.innerText.toLowerCase() : '';" +
                        // Splitting 'view' and 'repl' allows it to catch "view all 4 replies", "view 1 previous reply", etc.
                        "   if((t.includes('view') && t.includes('repl')) || (t.includes('xem') && t.includes('câu trả lời')) || (t.includes('hiển thị') && t.includes('bình luận'))) { " +
                        "       el.click(); " +
                        "   }" +
                        "});"
                    );
                    Thread.sleep(2000); // Give the replies time to slide down
                } catch (Exception ignored) {}

                // --- EXTRACT COMMENTS ---
                // We find all <time> tags on the screen, and work backwards to the comment container
                List<WebElement> timeElements = driver.findElements(By.tagName("time"));
                
                for (WebElement timeEl : timeElements) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        // Instagram DOM changes constantly. The safest way is to go up 5 levels to the main comment block.
                        WebElement commentBlock = (WebElement) js.executeScript(
                            "return arguments[0].closest('ul, li, div[role=\"listitem\"]') || arguments[0].parentElement.parentElement.parentElement.parentElement.parentElement;", 
                            timeEl
                        );

                        if (commentBlock == null) continue;

                        // Extract all text from the block at once. It usually looks like:
                        // "username\nThis is the comment text!\n2w\nReply"
                        String rawText = commentBlock.getAttribute("innerText");
                        if (rawText == null || rawText.isEmpty()) continue;

                        String[] lines = rawText.split("\\n");
                        if (lines.length < 2) continue;

                        String author = lines[0].trim();
                        String content = "";

                        // If the user is verified, line 1 might be "Verified", so the comment is pushed to line 2
                        if (lines.length > 2 && (lines[1].contains("Verified") || lines[1].trim().isEmpty() || lines[1].equals("•"))) {
                            content = lines[2].trim();
                        } else {
                            content = lines[1].trim();
                        }

                        // Extract Date (Bypassing hover by grabbing the ISO datetime attribute)
                        String dateStr = java.time.LocalDate.now().toString();
                        String datetime = timeEl.getAttribute("datetime"); // e.g., "2026-03-03T12:00:00.000Z"
                        if (datetime != null && datetime.length() >= 10) {
                            dateStr = datetime.substring(0, 10); // Grabs "YYYY-MM-DD"
                        }

                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");

                        if (!author.isEmpty() && !content.isEmpty() && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("INSTAGRAM", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // Instagram places comments inside a specific popup scroll window
                js.executeScript(
                    "var containers = document.querySelectorAll('div[style*=\"overflow-y: scroll\"], div[style*=\"overflow-y: auto\"], div[style*=\"overflow: hidden auto\"], article');" +
                    "if(containers.length > 0) { containers[containers.length-1].scrollBy(0, 1000); }" +
                    "window.scrollBy(0, 1000);"
                );
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} comments from Instagram.", results.size());
            
        } catch (Exception e) {
            logger.error("Instagram Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        }
        return results;
    }
    // 8. Threads Crawler (Safe Version - Prevents Wandering)
    private List<SocialResult> crawlThreadsSelenium(String url, int maxComments) {
        List<SocialResult> results = new ArrayList<>();
        Set<String> seenComments = new HashSet<>();
        
        if (url == null || !url.startsWith("http")) {
            logger.warn("Invalid Threads URL provided: {}", url);
            return results;
        }

        WebDriver driver = setupWebDriver();
        try {
            logger.info("Fetching Threads Post: {}", url);
            driver.get(url);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); 

            // Clean the original URL for our safety monitor (removes tracking tags like ?igshid=...)
            final String cleanOriginalUrl = url.split("\\?")[0].replace("threads.com", "threads.net");

            int previousSize = 0;
            int retries = 0;

            // --- EXTRACTION LOOP ---
            while (results.size() < maxComments && retries < 4) {

                // 1. EXTRACT COMMENTS FIRST
                List<WebElement> timeElements = driver.findElements(By.tagName("time"));
                
                for (WebElement timeEl : timeElements) {
                    if (results.size() >= maxComments) break;
                    
                    try {
                        WebElement commentBlock = (WebElement) js.executeScript(
                            "return arguments[0].closest('div[data-pressable-container=\"true\"]') || arguments[0].parentElement.parentElement.parentElement.parentElement.parentElement;", 
                            timeEl
                        );

                        if (commentBlock == null) continue;

                        String rawText = commentBlock.getAttribute("innerText");
                        if (rawText == null || rawText.isEmpty()) continue;

                        String[] lines = rawText.split("\\n");
                        if (lines.length < 3) continue;

                        String author = lines[0].trim();
                        String content = "";

                        for (int i = 1; i < lines.length; i++) {
                            String line = lines[i].trim();
                            if (!line.isEmpty() && !line.matches("\\d+[hmdw]") && !line.equals("Verified") && !line.equals(author)) {
                                content = line;
                                break;
                            }
                        }

                        // Extract Date
                        String dateStr = java.time.LocalDate.now().toString();
                        String datetime = timeEl.getAttribute("datetime"); 
                        if (datetime != null && datetime.length() >= 10) {
                            dateStr = datetime.substring(0, 10); 
                        }

                        String uniqueId = author + "|" + content.replaceAll("\\s+", " ");

                        if (!author.isEmpty() && !content.isEmpty() && !seenComments.contains(uniqueId)) {
                            seenComments.add(uniqueId);
                            results.add(new SocialResult("THREADS", author, content, 0, dateStr));
                        }
                    } catch (Exception ignored) {}
                }

                if (results.size() == previousSize) {
                    retries++;
                } else {
                    retries = 0;
                }
                previousSize = results.size();

                // 2. SAFETY MONITOR: Did we wander off the main thread?
                String currentUrl = driver.getCurrentUrl();
                if (currentUrl != null) {
                    String cleanCurrentUrl = currentUrl.split("\\?")[0].replace("threads.com", "threads.net");
                    
                    // If the current URL doesn't match the original, we either clicked a sub-thread or wandered away.
                    if (!cleanCurrentUrl.equalsIgnoreCase(cleanOriginalUrl)) {
                        logger.info("Crawler navigated away to {}. Returning to main thread...", cleanCurrentUrl);
                        driver.navigate().back(); // Use actual browser history to go back
                        Thread.sleep(3000);
                        continue; // Skip the rest of the loop and resume extracting on the main page
                    }
                }

                // 3. SAFE EXPANSION: Only click explicit "View replies" buttons (No blind clicking containers!)
                try {
                    js.executeScript(
                        "var elements = document.querySelectorAll('span, div[role=\"button\"]');" +
                        "elements.forEach(el => {" +
                        "   var t = el.innerText ? el.innerText.toLowerCase() : '';" +
                        "   if((t.includes('view') && t.includes('repl')) || (t.includes('xem') && t.includes('câu trả lời')) || (t.includes('hiển thị') && t.includes('bình luận'))) { " +
                        "       el.click(); " +
                        "   }" +
                        "});"
                    );
                    Thread.sleep(2000); 
                } catch (Exception ignored) {}

                // 4. SCROLL
                js.executeScript("window.scrollBy(0, 1500);");
                Thread.sleep(3000); 
            }
            logger.info("Successfully crawled {} comments from Threads.", results.size());
            
        } catch (Exception e) {
            logger.error("Threads Selenium Crawl Error for URL [{}]: {}", url, e.getMessage(), e);
        }
        return results;
    }
    private WebDriver setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        
        // Tell Selenium to connect to the Chrome window we just opened manually
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        
        try {
            return new ChromeDriver(options);
        } catch (Exception e) {
            logger.error("Could not connect to Chrome. Did you open it with the --remote-debugging-port=9222 flag? Error: {}", e.getMessage());
            throw e;
        }
    }
}