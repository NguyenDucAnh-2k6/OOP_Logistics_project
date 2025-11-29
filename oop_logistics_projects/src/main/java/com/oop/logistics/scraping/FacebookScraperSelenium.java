package com.oop.logistics.scraping;
import com.oop.logistics.Facebook.FacebookComment;
import com.oop.logistics.Facebook.FacebookPost;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facebook scraper using Selenium WebDriver
 * Scrapes public Facebook pages without API token
 */
public class FacebookScraperSelenium {

    private WebDriver driver;
    private WebDriverWait wait;
    private boolean isLoggedIn = false;

    public FacebookScraperSelenium() {
        this(false); // headless by default
    }

    public FacebookScraperSelenium(boolean headless) {
        initializeDriver(headless);
    }

    /**
     * Initialize Chrome WebDriver
     */
    private void initializeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Common options to avoid detection
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        // Disable automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        System.out.println("✓ Chrome WebDriver initialized");
    }

    /**
     * Login to Facebook (optional, for better access)
     * Only needed if scraping private content
     */
    public void login(String email, String password) throws Exception {
        System.out.println("Logging in to Facebook...");
        
        driver.get("https://www.facebook.com/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        
        // Enter credentials
        WebElement emailField = driver.findElement(By.id("email"));
        WebElement passwordField = driver.findElement(By.id("pass"));
        
        emailField.sendKeys(email);
        passwordField.sendKeys(password);
        
        // Click login
        WebElement loginButton = driver.findElement(By.name("login"));
        loginButton.click();
        
        // Wait for redirect
        Thread.sleep(5000);
        
        // Check if login successful
        if (driver.getCurrentUrl().contains("login")) {
            throw new Exception("Login failed. Check credentials or handle 2FA manually.");
        }
        
        isLoggedIn = true;
        System.out.println("✓ Logged in successfully");
    }

    /**
     * Scrape posts from a public Facebook page
     */
    public List<FacebookPost> scrapePagePosts(String pageUsername, int maxPosts) {
        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            String url = "https://www.facebook.com/" + pageUsername;
            System.out.println("Navigating to: " + url);
            
            driver.get(url);
            
            // Wait for page to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);
            
            // Scroll to load more posts
            int scrolls = Math.min(maxPosts / 3, 10); // Approximate posts per scroll
            for (int i = 0; i < scrolls; i++) {
                scrollDown();
                Thread.sleep(2000);
            }
            
            // Extract posts from page
            posts = extractPostsFromPage();
            
            System.out.println("Scraped " + posts.size() + " posts from " + pageUsername);
            
        } catch (Exception e) {
            System.err.println("Error scraping page: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts.subList(0, Math.min(maxPosts, posts.size()));
    }

    /**
     * Extract posts from current page HTML
     */
    private List<FacebookPost> extractPostsFromPage() {
        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            // Find post containers
            // Facebook's HTML structure changes frequently, so we use multiple selectors
            List<WebElement> postElements = findPostElements();
            
            System.out.println("Found " + postElements.size() + " post elements");
            
            for (WebElement postElement : postElements) {
                try {
                    FacebookPost post = extractPostFromElement(postElement);
                    if (post != null && post.getMessage() != null) {
                        posts.add(post);
                    }
                } catch (Exception e) {
                    // Skip problematic posts
                    continue;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting posts: " + e.getMessage());
        }
        
        return posts;
    }

    /**
     * Find post elements using multiple selectors
     */
    private List<WebElement> findPostElements() {
        List<WebElement> elements = new ArrayList<>();
        
        // Try multiple selectors (Facebook changes these frequently)
        String[] selectors = {
        // Most common selector for a post container in modern FB
        "div[role='feed'] > div",
        // Fallback to the classic ARIA role for an article
        "div[role='article']",
        // Selector for the entire feed story wrapper
        "div[data-testid='fbFeedStory']" 
    };
        
        for (String selector : selectors) {
            try {
                List<WebElement> found = driver.findElements(By.cssSelector(selector));
                if (!found.isEmpty()) {
                    elements.addAll(found);
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        return elements;
    }

    /**
     * Extract post data from a WebElement
     */
    private FacebookPost extractPostFromElement(WebElement element) {
        FacebookPost post = new FacebookPost();
        
        try {
            // Extract post ID from element or URL
            String elementHtml = element.getAttribute("outerHTML");
            post.setId(extractPostId(elementHtml));
            
            // Extract text content
            String text = extractTextContent(element);
            post.setMessage(text);
            
            // Extract timestamp
            String timestamp = extractTimestamp(element);
            post.setCreatedTime(timestamp);
            
            // Extract engagement metrics
            extractEngagementMetrics(element, post);
            
        } catch (Exception e) {
            System.err.println("Error extracting post data: " + e.getMessage());
        }
        
        return post;
    }

    /**
     * Extract text content from post
     */
    private String extractTextContent(WebElement element) {
        try {
            // Try multiple selectors for post text
            String[] textSelectors = {
                "[data-ad-comet-preview='message']",
                "[data-ad-preview='message']",
                "div[dir='auto']",
                ".userContent",
                "[data-testid='post_message']"
            };
            
            for (String selector : textSelectors) {
                try {
                    List<WebElement> textElements = element.findElements(By.cssSelector(selector));
                    if (!textElements.isEmpty()) {
                        StringBuilder text = new StringBuilder();
                        for (WebElement el : textElements) {
                            String content = el.getText().trim();
                            if (!content.isEmpty() && content.length() > 10) {
                                text.append(content).append(" ");
                            }
                        }
                        if (text.length() > 0) {
                            return text.toString().trim();
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            // Fallback: get all text
            String allText = element.getText().trim();
            return allText.length() > 10 ? allText : null;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract timestamp
     */
    private String extractTimestamp(WebElement element) {
        try {
            // Look for timestamp elements
            List<WebElement> timeElements = element.findElements(By.tagName("a"));
            for (WebElement timeEl : timeElements) {
                String href = timeEl.getAttribute("href");
                if (href != null && href.contains("/posts/")) {
                    return timeEl.getAttribute("aria-label");
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return java.time.LocalDateTime.now().toString();
    }

    /**
     * Extract engagement metrics (likes, comments, shares)
     */
    private void extractEngagementMetrics(WebElement element, FacebookPost post) {
        try {
            String html = element.getAttribute("outerHTML");
            
            // Extract numbers from aria-labels and text
            Pattern likePattern = Pattern.compile("(\\d+[KkMm]?)\\s*(like|thích)", Pattern.CASE_INSENSITIVE);
            Pattern commentPattern = Pattern.compile("(\\d+[KkMm]?)\\s*(comment|bình luận)", Pattern.CASE_INSENSITIVE);
            Pattern sharePattern = Pattern.compile("(\\d+[KkMm]?)\\s*(share|chia sẻ)", Pattern.CASE_INSENSITIVE);
            
            Matcher likeMatcher = likePattern.matcher(html);
            if (likeMatcher.find()) {
                post.setLikes(parseEngagementNumber(likeMatcher.group(1)));
            }
            
            Matcher commentMatcher = commentPattern.matcher(html);
            if (commentMatcher.find()) {
                post.setComments(parseEngagementNumber(commentMatcher.group(1)));
            }
            
            Matcher shareMatcher = sharePattern.matcher(html);
            if (shareMatcher.find()) {
                post.setShares(parseEngagementNumber(shareMatcher.group(1)));
            }
            
        } catch (Exception e) {
            // Ignore engagement parsing errors
        }
    }

    /**
     * Parse engagement number (handles K, M suffixes)
     */
    private int parseEngagementNumber(String numStr) {
        if (numStr == null) return 0;
        
        numStr = numStr.toUpperCase().trim();
        
        try {
            if (numStr.endsWith("K")) {
                return (int) (Double.parseDouble(numStr.replace("K", "")) * 1000);
            } else if (numStr.endsWith("M")) {
                return (int) (Double.parseDouble(numStr.replace("M", "")) * 1000000);
            } else {
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extract post ID from HTML
     */
    private String extractPostId(String html) {
        Pattern pattern = Pattern.compile("/(posts|permalink|videos)/(\\d+)");
        Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(2);
        }
        
        // Generate a temporary ID
        return "scraped_" + System.currentTimeMillis();
    }

    /**
     * Scroll down to load more content
     */
    private void scrollDown() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    /**
     * Search for posts by keyword
     */
    public List<FacebookPost> searchPosts(String keyword, int maxResults) {
        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            String searchUrl = "https://www.facebook.com/search/posts/?q=" + 
                              java.net.URLEncoder.encode(keyword, "UTF-8");
            
            driver.get(searchUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            
            // Scroll to load results
            for (int i = 0; i < 5; i++) {
                scrollDown();
                Thread.sleep(2000);
            }
            
            posts = extractPostsFromPage();
            
        } catch (Exception e) {
            System.err.println("Error searching posts: " + e.getMessage());
        }
        
        return posts.subList(0, Math.min(maxResults, posts.size()));
    }
        /**
 * Scrape comments for a specific post URL
 */
public List<FacebookComment> scrapeComments(String postUrl, int maxComments) {
        List<FacebookComment> comments = new ArrayList<>();

        try {
            driver.get(postUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);

            // Extract the Post ID from the URL for later reference
            String postId = extractPostId(postUrl);

            // --- 1. Load All Comments (Click Loop) ---
            // Continuously click "View more comments" or "View more replies" up to 50 times
            for (int i = 0; i < 50; i++) {
                try {
                    // Try to find the button that contains keywords like "comments" or "bình luận"
                    WebElement moreCommentsButton = wait.until(ExpectedConditions.
                            presenceOfElementLocated(By.xpath("//div[@role='button']//span[contains(text(),'comments') or contains(text(),'bình luận') or contains(text(),'trả lời')]")));

                    // Use JavascriptExecutor to click to avoid interception by headers/popups
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", moreCommentsButton);

                    // Wait for new content to load or for the button to disappear
                    wait.until(ExpectedConditions.invisibilityOf(moreCommentsButton));
                    Thread.sleep(1500); 

                } catch (TimeoutException | NoSuchElementException e) {
                    // If the button is not found after a delay, assume all comments are loaded
                    break;
                } catch (StaleElementReferenceException e) {
                    // If element becomes stale, retry the loop
                    continue;
                }
            }
            List<WebElement> commentContainers = 
                driver.findElements(By.cssSelector("div[role='article'][tabindex='-1']")); 
            
            System.out.println("Found " + commentContainers.size() + " comment containers.");

            for (WebElement container : commentContainers) {
                if (comments.size() >= maxComments) break;
                
                try {
                    // Locator 1: Comment Text (most stable)
                    WebElement textElement = container.findElement(
                        By.cssSelector("div[data-testid='comment-body'] div[dir='auto'], span[dir='auto']")
                    ); 
                    String text = textElement.getText().trim();
                    
                    // Locator 2: Timestamp (Look for the time link/element)
                    WebElement timeElement = container.findElement(
                        By.cssSelector("a[role='link'] > div > span")
                    );
                    String timeRaw = timeElement.getAttribute("aria-label"); 

                    if (!text.isEmpty()) {
                        FacebookComment comment = new FacebookComment();
                        comment.setText(text);
                        comment.setCreatedTime(timeRaw != null ? timeRaw : java.time.LocalDateTime.now().toString());
                        comment.setPostId(postId);
                        
                        comments.add(comment);
                    }
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    // Skip if text or time elements are missing from this container
                    continue;
                }
            }

            System.out.println("✓ Scraped " + comments.size() + " comments from " + postUrl);

        } catch (Exception e) {
            System.err.println("Fatal error scraping comments: " + e.getMessage());
        }

        return comments;
    }
    /**
     * Take screenshot (useful for debugging)
     */
    public void takeScreenshot(String filename) {
        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            byte[] bytes = screenshot.getScreenshotAs(OutputType.BYTES);
            java.nio.file.Files.write(
                java.nio.file.Paths.get(filename), 
                bytes
            );
            System.out.println("Screenshot saved: " + filename);
        } catch (Exception e) {
            System.err.println("Error taking screenshot: " + e.getMessage());
        }
    }

    /**
     * Close browser and cleanup
     */
    public void close() {
        if (driver != null) {
            driver.quit();
            System.out.println("✓ Browser closed");
        }
    }

    /**
     * Check if scraper is ready
     */
    public boolean isReady() {
        try {
            return driver != null && driver.getWindowHandle() != null;
        } catch (Exception e) {
            return false;
        }
    }
}