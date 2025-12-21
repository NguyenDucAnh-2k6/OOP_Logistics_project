package com.oop.logistics.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FacebookCrawler v11 - Improved date extraction with hover
 * Hovers over timestamps to reveal full date and time
 */
public class FacebookCrawler {
    
    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;
    private boolean isLoggedIn = false;
    
    // ==================== ADJUSTABLE CONSTANTS ====================
    // Increase these values to get more comments
    
    /**
     * Maximum number of scroll attempts to load more comments
     * Higher = more comments but slower
     * Recommended: 20-50
     */
    public static int MAX_SCROLL_ATTEMPTS = 30;
    
    /**
     * Time to wait between scrolls (milliseconds)
     * Higher = more stable but slower
     * Recommended: 1500-3000
     */
    public static int SCROLL_PAUSE_TIME = 2000;
    
    /**
     * Time to wait after clicking "View more" buttons (milliseconds)
     * Higher = ensures content loads fully
     * Recommended: 1000-2000
     */
    public static int BUTTON_CLICK_WAIT = 1500;
    
    /**
     * Number of unchanged scroll attempts before stopping
     * Higher = more persistent but may waste time
     * Recommended: 3-5
     */
    public static int MAX_UNCHANGED_ATTEMPTS = 5;
    
    /**
     * Initial page load wait time (milliseconds)
     * Higher = better for slow connections
     * Recommended: 5000-10000
     */
    public static int PAGE_LOAD_WAIT = 7000;
    
    /**
     * Wait time after opening Reel comments (milliseconds)
     * Recommended: 3000-5000
     */
    public static int REEL_COMMENTS_WAIT = 4000;
    
    // ==============================================================
    
    public static class Comment {
        private String author;
        private String content;
        private String timestamp;
        private boolean hasReply;
        private boolean isReply;
        private String parentAuthor;
        
        public Comment(String author, String content, String timestamp, boolean hasReply) {
            this(author, content, timestamp, hasReply, false, null);
        }
        
        public Comment(String author, String content, String timestamp, boolean hasReply, 
                      boolean isReply, String parentAuthor) {
            this.author = author;
            this.content = content;
            this.timestamp = timestamp;
            this.hasReply = hasReply;
            this.isReply = isReply;
            this.parentAuthor = parentAuthor;
        }
        
        @Override
        public String toString() {
            String prefix = isReply ? "  ↳ Reply" : "Comment";
            String parent = isReply && parentAuthor != null ? " (to " + parentAuthor + ")" : "";
            return String.format("%s%s:\nAuthor: %s\nContent: %s\nTime: %s\nHas Replies: %s\n", 
                prefix, parent, author, content, timestamp, hasReply);
        }
        
        /**
         * Convert to CSV row format: date,text
         * Escapes quotes and commas in content
         */
        public String toCsvRow() {
            String escapedContent = content.replace("\"", "\"\""); // Escape quotes
            return String.format("\"%s\",\"%s\"", timestamp, escapedContent);
        }

        public String getAuthor() { return author; }
        public String getContent() { return content; }
        public String getTimestamp() { return timestamp; }
        public boolean hasReply() { return hasReply; }
        public boolean isReply() { return isReply; }
        public String getParentAuthor() { return parentAuthor; }
    }
    
    public FacebookCrawler() {
        this(false);
    }
    
    public FacebookCrawler(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-notifications");
        options.addArguments("--lang=vi-VN");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        actions = new Actions(driver);
    }
    
    /**
     * Login using Facebook cookies (c_user, xs, fr)
     */
    public boolean loginWithCookies(String cUser, String xs, String fr) {
        try {
            System.out.println("Logging in with cookies...");
            
            driver.get("https://www.facebook.com/");
            Thread.sleep(2000);
            
            driver.manage().addCookie(new Cookie("c_user", cUser, ".facebook.com", "/", null));
            driver.manage().addCookie(new Cookie("xs", xs, ".facebook.com", "/", null));
            
            if (fr != null && !fr.isEmpty()) {
                driver.manage().addCookie(new Cookie("fr", fr, ".facebook.com", "/", null));
            }
            
            System.out.println("Cookies added. Refreshing page...");
            driver.navigate().refresh();
            Thread.sleep(3000);
            
            String currentUrl = driver.getCurrentUrl();
            boolean loginFormPresent = !driver.findElements(By.xpath(
                "//input[@name='email' or @id='email']")).isEmpty();
            
            if (currentUrl.contains("facebook.com") && !loginFormPresent) {
                System.out.println("✓ Cookie login successful!");
                isLoggedIn = true;
                dismissPopups();
                return true;
            } else {
                System.err.println("✗ Cookie login failed. Cookies may be invalid or expired.");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Cookie login error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void dismissPopups() {
        try {
            Thread.sleep(1000);
            List<WebElement> notNowButtons = driver.findElements(By.xpath(
                "//div[@role='button' and (contains(text(), 'Not now') or " +
                "contains(text(), 'Không phải bây giờ') or contains(text(), 'Bỏ qua'))]"));
            
            for (WebElement button : notNowButtons) {
                try {
                    if (button.isDisplayed()) {
                        button.click();
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    // Button not clickable
                }
            }
        } catch (Exception e) {
            // No popups to dismiss
        }
    }
    
    public void printCurrentCookies() {
        System.out.println("\n=== Current Facebook Cookies ===");
        Set<Cookie> cookies = driver.manage().getCookies();
        
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("c_user") || 
                cookie.getName().equals("xs") || 
                cookie.getName().equals("fr")) {
                System.out.println(cookie.getName() + " = " + cookie.getValue());
            }
        }
        System.out.println("================================\n");
    }
    
    public List<Comment> crawlComments(String postUrl) {
        List<Comment> allComments = new ArrayList<>();
        Set<String> processedComments = new HashSet<>();
        
        try {
            if (!isLoggedIn) {
                System.err.println("Warning: Not logged in. You may see limited content.");
                System.err.println("Please call loginWithCookies() before crawling.");
            }
            
            driver.get(postUrl);
            System.out.println("Navigated to: " + postUrl);
            System.out.println("Waiting " + PAGE_LOAD_WAIT + "ms for page to load...");
            Thread.sleep(PAGE_LOAD_WAIT);
            
            if (driver.getCurrentUrl().contains("login") || 
                !driver.findElements(By.xpath("//*[contains(text(), 'Đăng nhập') or contains(text(), 'Log in')]")).isEmpty()) {
                System.err.println("Login required! Please login first using loginWithCookies() method.");
                return allComments;
            }
            
            boolean isReel = detectAndHandleReel();
            
            if (isReel) {
                System.out.println("Detected Reel format - opening comments panel...");
            }
            
            expandAllCommentsAndReplies();
            Thread.sleep(3000);
            
            System.out.println("Starting comment extraction...");
            allComments.addAll(extractCommentsByStrategy1(processedComments));
            allComments.addAll(extractCommentsByStrategy2(processedComments));
            
            System.out.println("Successfully extracted " + allComments.size() + " total comments/replies");
            
            // Add debug info about what was filtered
            int totalContainers = driver.findElements(By.xpath("//div[@role='article']")).size();
            if (allComments.size() < totalContainers) {
                System.out.println("Note: Found " + totalContainers + " containers but extracted " + 
                                 allComments.size() + " valid comments");
                System.out.println("Some containers may be empty, duplicates, or UI elements");
            }
            
        } catch (Exception e) {
            System.err.println("Error crawling comments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return allComments;
    }
    
    private boolean detectAndHandleReel() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        try {
            String currentUrl = driver.getCurrentUrl();
            boolean urlIndicatesReel = currentUrl.contains("/reel/") || currentUrl.contains("/share/r/");
            
            List<WebElement> reelIndicators = driver.findElements(By.xpath(
                "//video | //div[contains(@class, 'reel')] | " +
                "//div[@role='dialog' and .//video]"));
            
            boolean hasReelUI = !reelIndicators.isEmpty();
            
            if (urlIndicatesReel || hasReelUI) {
                System.out.println("Reel detected! Looking for comment button...");
                
                List<WebElement> commentButtons = driver.findElements(By.xpath(
                    "//div[@role='button' and (@aria-label='Bình luận' or @aria-label='Comment' or " +
                    "@aria-label='Comments' or contains(@aria-label, 'comment'))]"));
                
                if (commentButtons.isEmpty()) {
                    commentButtons = driver.findElements(By.xpath(
                        "//div[@role='button']//svg[contains(@aria-label, 'Bình luận') or " +
                        "contains(@aria-label, 'Comment')]"));
                }
                
                if (commentButtons.isEmpty()) {
                    commentButtons = driver.findElements(By.xpath(
                        "//div[@role='button' and .//i[contains(@style, 'background-image')]]"));
                }
                
                if (commentButtons.isEmpty()) {
                    List<WebElement> allButtons = driver.findElements(By.xpath("//div[@role='button']"));
                    System.out.println("Found " + allButtons.size() + " buttons, checking for comment button...");
                    
                    for (WebElement button : allButtons) {
                        try {
                            String ariaLabel = button.getAttribute("aria-label");
                            String text = button.getText().toLowerCase();
                            
                            if ((ariaLabel != null && (ariaLabel.toLowerCase().contains("comment") || 
                                                       ariaLabel.toLowerCase().contains("bình luận"))) ||
                                text.contains("bình luận") || text.contains("comment")) {
                                commentButtons.add(button);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                
                if (!commentButtons.isEmpty()) {
                    WebElement commentButton = commentButtons.get(0);
                    System.out.println("Found comment button! Clicking...");
                    
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", commentButton);
                    Thread.sleep(1000);
                    
                    try {
                        commentButton.click();
                    } catch (Exception e) {
                        js.executeScript("arguments[0].click();", commentButton);
                    }
                    
                    System.out.println("Waiting " + REEL_COMMENTS_WAIT + "ms for comments panel...");
                    Thread.sleep(REEL_COMMENTS_WAIT);
                    
                    List<WebElement> commentSections = driver.findElements(By.xpath(
                        "//div[contains(@aria-label, 'Comments') or contains(@aria-label, 'Bình luận')] | " +
                        "//div[@role='dialog']//div[@role='article']"));
                    
                    if (!commentSections.isEmpty()) {
                        System.out.println("✓ Comments panel opened successfully!");
                    } else {
                        System.out.println("⚠ Comments panel may not have opened. Trying alternative method...");
                        js.executeScript("window.scrollBy(0, 500);");
                        Thread.sleep(2000);
                    }
                    
                    return true;
                } else {
                    System.out.println("⚠ Could not find comment button. Will try to extract visible comments.");
                }
                
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Error handling Reel: " + e.getMessage());
        }
        
        return false;
    }
    
    private void expandAllCommentsAndReplies() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int previousCount = 0;
        int unchangedCount = 0;
        
        System.out.println("Expanding all comments and replies...");
        System.out.println("Configuration: MAX_SCROLL_ATTEMPTS=" + MAX_SCROLL_ATTEMPTS + 
                         ", SCROLL_PAUSE_TIME=" + SCROLL_PAUSE_TIME + "ms");
        
        WebElement commentsContainer = findCommentsContainer();
        
        for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
            try {
                if (commentsContainer != null) {
                    try {
                        js.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", commentsContainer);
                        Thread.sleep(SCROLL_PAUSE_TIME);
                    } catch (Exception e) {
                        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                        Thread.sleep(SCROLL_PAUSE_TIME);
                    }
                } else {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(SCROLL_PAUSE_TIME);
                }
                
                clickAllButtons(new String[]{
                    "//span[contains(text(), 'Xem thêm bình luận')]",
                    "//span[contains(text(), 'View more comments')]",
                    "//span[contains(text(), 'Xem bình luận khác')]",
                    "//div[@role='button' and contains(., 'bình luận')]"
                });
                
                clickAllButtons(new String[]{
                    "//span[contains(text(), 'phản hồi')]",
                    "//span[contains(text(), 'Xem thêm phản hồi')]",
                    "//span[contains(text(), 'replies')]",
                    "//span[contains(text(), 'View more replies')]",
                    "//div[@role='button' and contains(., 'phản hồi')]",
                    "//a[contains(text(), 'phản hồi')]"
                });
                
                clickAllButtons(new String[]{
                    "//div[@role='button' and (contains(text(), 'Xem thêm') or contains(text(), 'See more'))]",
                    "//span[contains(text(), 'Xem thêm') and not(contains(text(), 'bình luận'))]"
                });
                
                if (commentsContainer != null) {
                    try {
                        js.executeScript("arguments[0].scrollTop -= 300;", commentsContainer);
                        Thread.sleep(500);
                        js.executeScript("arguments[0].scrollTop += 600;", commentsContainer);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        js.executeScript("window.scrollBy(0, -300);");
                        Thread.sleep(500);
                        js.executeScript("window.scrollBy(0, 600);");
                        Thread.sleep(1000);
                    }
                } else {
                    js.executeScript("window.scrollBy(0, -300);");
                    Thread.sleep(500);
                    js.executeScript("window.scrollBy(0, 600);");
                    Thread.sleep(1000);
                }
                
                int currentCount = driver.findElements(By.xpath("//div[@role='article']")).size();
                System.out.println("Attempt " + (attempt + 1) + "/" + MAX_SCROLL_ATTEMPTS + 
                                 ": Found " + currentCount + " comment containers");
                
                if (currentCount == previousCount) {
                    unchangedCount++;
                    if (unchangedCount >= MAX_UNCHANGED_ATTEMPTS) {
                        System.out.println("No new comments for " + MAX_UNCHANGED_ATTEMPTS + " attempts. Stopping.");
                        break;
                    }
                } else {
                    unchangedCount = 0;
                    previousCount = currentCount;
                }
                
            } catch (Exception e) {
                System.out.println("Minor error during expansion: " + e.getMessage());
            }
        }
        
        System.out.println("Finished expanding comments and replies");
    }
    
    private WebElement findCommentsContainer() {
        try {
            List<WebElement> containers = driver.findElements(By.xpath(
                "//div[@role='dialog']//div[.//div[@role='article']] | " +
                "//div[contains(@aria-label, 'Comments') or contains(@aria-label, 'Bình luận')] | " +
                "//div[@role='complementary' and .//div[@role='article']]"));
            
            if (!containers.isEmpty()) {
                System.out.println("Found comments container (dialog/modal)");
                return containers.get(0);
            }
        } catch (Exception e) {
            // No container found
        }
        
        return null;
    }
    
    private void clickAllButtons(String[] xpaths) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        for (String xpath : xpaths) {
            try {
                List<WebElement> buttons = driver.findElements(By.xpath(xpath));
                if (buttons.size() > 0) {
                    System.out.println("  Clicking " + buttons.size() + " buttons: " + xpath.substring(0, Math.min(50, xpath.length())) + "...");
                }
                
                for (WebElement button : buttons) {
                    try {
                        if (button.isDisplayed() && button.isEnabled()) {
                            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                            Thread.sleep(300);
                            
                            try {
                                button.click();
                            } catch (Exception e) {
                                js.executeScript("arguments[0].click();", button);
                            }
                            
                            Thread.sleep(BUTTON_CLICK_WAIT);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
    }
    
    private List<Comment> extractCommentsByStrategy1(Set<String> processedComments) {
        List<Comment> comments = new ArrayList<>();
        
        try {
            List<WebElement> commentContainers = driver.findElements(By.xpath(
                "//div[@role='article' or contains(@class, 'x1lliihq')]"));
            
            System.out.println("Strategy 1: Processing " + commentContainers.size() + " containers");
            
            for (WebElement container : commentContainers) {
                try {
                    // Extract without strict filtering
                    Comment comment = extractCommentFromElement(container, processedComments, false, null);
                    if (comment != null) {
                        comments.add(comment);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            System.out.println("Strategy 1: Extracted " + comments.size() + " comments");
        } catch (Exception e) {
            System.err.println("Strategy 1 error: " + e.getMessage());
        }
        
        return comments;
    }
    
    private List<Comment> extractCommentsByStrategy2(Set<String> processedComments) {
        List<Comment> comments = new ArrayList<>();
        
        try {
            List<WebElement> allDivs = driver.findElements(By.xpath(
                "//div[contains(@class, 'x1lliihq') or contains(@class, 'x1pi30zi')]"));
            
            System.out.println("Strategy 2: Processing " + allDivs.size() + " potential comment divs");
            
            for (WebElement div : allDivs) {
                try {
                    String text = div.getText();
                    // More lenient filtering - keep more content
                    if (text.length() > 2) {
                        Comment comment = extractCommentFromElement(div, processedComments);
                        if (comment != null) {
                            comments.add(comment);
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            System.out.println("Strategy 2: Extracted " + comments.size() + " comments");
        } catch (Exception e) {
            System.err.println("Strategy 2 error: " + e.getMessage());
        }
        
        return comments;
    }
    
    private List<Comment> extractCommentsFromContainer(WebElement container, Set<String> processedComments) {
        List<Comment> comments = new ArrayList<>();
        
        try {
            boolean isReply = false;
            String parentAuthor = null;
            
            try {
                WebElement parent = container.findElement(By.xpath("./ancestor::div[contains(@style, 'padding-left')]"));
                if (parent != null) {
                    isReply = true;
                }
            } catch (Exception e) {
                // Not a reply
            }
            
            Comment comment = extractCommentFromElement(container, processedComments, isReply, parentAuthor);
            if (comment != null) {
                comments.add(comment);
            }
        } catch (Exception e) {
            // Skip
        }
        
        return comments;
    }
    
    private Comment extractCommentFromElement(WebElement element, Set<String> processedComments) {
        return extractCommentFromElement(element, processedComments, false, null);
    }
    
    private Comment extractCommentFromElement(WebElement element, Set<String> processedComments, 
                                             boolean isReply, String parentAuthor) {
        try {
            String author = "";
            String content = "";
            String timestamp = "";
            boolean hasReply = false;
            
            // Extract author - more lenient
            try {
                List<WebElement> authorLinks = element.findElements(By.xpath(
                    ".//a[@role='link']//span"));
                
                for (WebElement link : authorLinks) {
                    String text = link.getText().trim();
                    if (!text.isEmpty() && text.length() < 100) {
                        author = text;
                        break;
                    }
                }
                
                if (author.isEmpty()) {
                    List<WebElement> strongTags = element.findElements(By.tagName("strong"));
                    for (WebElement strong : strongTags) {
                        String text = strong.getText().trim();
                        if (!text.isEmpty() && text.length() < 100) {
                            author = text;
                            break;
                        }
                    }
                }
                
                if (author.isEmpty()) {
                    author = "Unknown";
                }
            } catch (Exception e) {
                author = "Unknown";
            }
            
            // Extract content - less strict filtering
            try {
                List<WebElement> textSpans = element.findElements(By.xpath(
                    ".//div[@dir='auto']//span"));
                
                StringBuilder contentBuilder = new StringBuilder();
                Set<String> seenTexts = new HashSet<>();
                
                for (WebElement span : textSpans) {
                    String text = span.getText().trim();
                    if (!text.isEmpty() && !seenTexts.contains(text)) {
                        seenTexts.add(text);
                        // Less strict filtering - only remove obvious UI elements
                        if (!text.equals("Thích") && !text.equals("Trả lời") && 
                            !text.equals("Bình luận") && !text.equals("Chia sẻ")) {
                            contentBuilder.append(text).append(" ");
                        }
                    }
                }
                content = contentBuilder.toString().trim();
                
                // If still empty, try getting all text
                if (content.isEmpty()) {
                    String allText = element.getText();
                    String[] lines = allText.split("\n");
                    StringBuilder builder = new StringBuilder();
                    
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && 
                            !line.equals("Thích") && !line.equals("Trả lời") &&
                            !line.equals("Bình luận") && !line.equals("Chia sẻ")) {
                            builder.append(line).append(" ");
                        }
                    }
                    content = builder.toString().trim();
                }
                
                // Remove author name from content if it appears at the start
                if (content.startsWith(author)) {
                    content = content.substring(author.length()).trim();
                }
            } catch (Exception e) {
                content = "";
            }
            
            // Extract timestamp with hover to get full date
            try {
                // Find timestamp elements (relative time like "1 năm", "9 tuần")
                List<WebElement> timeElements = element.findElements(By.xpath(
                    ".//span[contains(text(), 'tuần') or contains(text(), 'ngày') or " +
                    "contains(text(), 'giờ') or contains(text(), 'phút') or contains(text(), 'giây') or " +
                    "contains(text(), 'năm') or " +
                    "contains(text(), 'week') or contains(text(), 'day') or " +
                    "contains(text(), 'hour') or contains(text(), 'minute') or contains(text(), 'year')]"));
                
                if (!timeElements.isEmpty()) {
                    WebElement timeElement = timeElements.get(0);
                    String relativeTime = timeElement.getText().trim();
                    
                    // Try to hover to get full date
                    try {
                        // Scroll element into view
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", timeElement);
                        Thread.sleep(300);
                        
                        // Hover over the element
                        actions.moveToElement(timeElement).perform();
                        Thread.sleep(800); // Wait for tooltip to appear
                        
                        // Try to find tooltip with full date
                        List<WebElement> tooltips = driver.findElements(By.xpath(
                            "//div[@role='tooltip'] | //div[contains(@class, 'tooltip')] | " +
                            "//div[contains(@style, 'position: fixed') or contains(@style, 'position: absolute')]"));
                        
                        for (WebElement tooltip : tooltips) {
                            try {
                                String tooltipText = tooltip.getText().trim();
                                // Check if tooltip contains date-like text
                                if (tooltipText.matches(".*\\d{1,2}.*tháng.*\\d{4}.*") || 
                                    tooltipText.matches(".*\\d{1,2}:\\d{2}.*") ||
                                    tooltipText.length() > relativeTime.length()) {
                                    timestamp = tooltipText;
                                    break;
                                }
                            } catch (Exception e) {
                                continue;
                            }
                        }
                        
                        // If no tooltip found, try aria-label or title attribute
                        if (timestamp.isEmpty()) {
                            String ariaLabel = timeElement.getAttribute("aria-label");
                            String title = timeElement.getAttribute("title");
                            
                            if (ariaLabel != null && !ariaLabel.isEmpty() && ariaLabel.length() > relativeTime.length()) {
                                timestamp = ariaLabel;
                            } else if (title != null && !title.isEmpty() && title.length() > relativeTime.length()) {
                                timestamp = title;
                            }
                        }
                        
                        // If still no full date, check parent <a> tag
                        if (timestamp.isEmpty()) {
                            try {
                                WebElement parentLink = timeElement.findElement(By.xpath("./ancestor::a[1]"));
                                String linkAriaLabel = parentLink.getAttribute("aria-label");
                                String linkTitle = parentLink.getAttribute("title");
                                
                                if (linkAriaLabel != null && linkAriaLabel.length() > relativeTime.length()) {
                                    timestamp = linkAriaLabel;
                                } else if (linkTitle != null && linkTitle.length() > relativeTime.length()) {
                                    timestamp = linkTitle;
                                }
                            } catch (Exception e) {
                                // No parent link
                            }
                        }
                        
                        // Fall back to relative time if no full date found
                        if (timestamp.isEmpty()) {
                            timestamp = relativeTime;
                        }
                        
                    } catch (Exception e) {
                        // Hover failed, use relative time
                        timestamp = relativeTime;
                    }
                } else {
                    timestamp = "";
                }
            } catch (Exception e) {
                timestamp = "";
            }
            
            // Check for replies
            try {
                String elementText = element.getText().toLowerCase();
                hasReply = elementText.contains("phản hồi") || elementText.contains("replied");
            } catch (Exception e) {
                hasReply = false;
            }
            
            // More lenient unique key - only use first 30 chars to allow similar comments
            String uniqueKey = author + "|" + content.substring(0, Math.min(30, content.length()));
            
            // Keep the comment if it has ANY content (much less strict)
            if (!content.isEmpty() && content.length() > 1 && !processedComments.contains(uniqueKey)) {
                processedComments.add(uniqueKey);
                return new Comment(author, content, timestamp, hasReply, isReply, parentAuthor);
            }
            
        } catch (Exception e) {
            // Skip
        }
        
        return null;
    }
    
    /**
     * Export comments to CSV file with format: date,text
     * @param comments List of comments to export
     * @param filename Output filename (e.g., "comments.csv")
     */
    public static void exportToCsv(List<Comment> comments, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write CSV header
            writer.append("date,text\n");
            
            // Write each comment
            for (Comment comment : comments) {
                writer.append(comment.toCsvRow());
                writer.append("\n");
            }
            
            System.out.println("\n✓ Successfully exported " + comments.size() + " comments to " + filename);
            
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}