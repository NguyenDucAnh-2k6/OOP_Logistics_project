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
    public enum Platform { YOUTUBE, TIKTOK, VOZ, REDDIT }
    private final Platform platform;

    public UnifiedSocialCrawler(Platform platform) {
        this.platform = platform;
    }

    @Override
    public List<SocialResult> crawlComments(String url, int maxComments) {
        return switch (platform) {
            case VOZ -> crawlVozSelenium(url, maxComments); // <-- Updated to Selenium
            case YOUTUBE -> crawlYouTubeSelenium(url, maxComments);
            case TIKTOK -> crawlTikTokSelenium(url, maxComments);
            case REDDIT -> crawlRedditJson(url, maxComments);
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