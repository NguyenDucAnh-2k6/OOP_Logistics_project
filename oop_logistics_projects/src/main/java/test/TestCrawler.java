package test;

import com.oop.logistics.crawler.FacebookCrawler;
import com.oop.logistics.crawler.FacebookCrawler.Comment;

import java.util.List;

public class TestCrawler {
    
    public static void main(String[] args) {
        FacebookCrawler crawler = null;
        
        try {
            System.out.println("=== Facebook Comment Crawler v11 (Enhanced Date Extraction) ===\n");
            
            // ============ ADJUSTABLE CONSTANTS ============
            // Increase these for more comments
            FacebookCrawler.MAX_SCROLL_ATTEMPTS = 30;      // Default: 30 (try 40-50 for more)
            FacebookCrawler.SCROLL_PAUSE_TIME = 2000;       // Default: 2000ms (try 2500-3000 for stability)
            FacebookCrawler.BUTTON_CLICK_WAIT = 1500;       // Default: 1500ms (try 2000 for slow connections)
            FacebookCrawler.MAX_UNCHANGED_ATTEMPTS = 5;     // Default: 5 (try 7-10 to be more persistent)
            FacebookCrawler.PAGE_LOAD_WAIT = 7000;          // Default: 7000ms (try 10000 for slow connections)
            FacebookCrawler.REEL_COMMENTS_WAIT = 4000;      // Default: 4000ms (try 5000 for Reels)
            // ==============================================
            
            System.out.println("Current Configuration:");
            System.out.println("  MAX_SCROLL_ATTEMPTS: " + FacebookCrawler.MAX_SCROLL_ATTEMPTS);
            System.out.println("  SCROLL_PAUSE_TIME: " + FacebookCrawler.SCROLL_PAUSE_TIME + "ms");
            System.out.println("  BUTTON_CLICK_WAIT: " + FacebookCrawler.BUTTON_CLICK_WAIT + "ms");
            System.out.println("  MAX_UNCHANGED_ATTEMPTS: " + FacebookCrawler.MAX_UNCHANGED_ATTEMPTS);
            System.out.println("  PAGE_LOAD_WAIT: " + FacebookCrawler.PAGE_LOAD_WAIT + "ms\n");
            
            // Facebook cookies - REPLACE WITH YOUR OWN
            String cUser = "100072127640070";
            String xs = "14%3A_3tWopl3qts-2g%3A2%3A1765898701%3A-1%3A-1%3A%3AAcxoaG_DEIT2YU-dAm--C8TVP9BBcqcCWanCHhnpUw";
            String fr = "1hqFmtFI3VKuxGTMG.AWeQkm8Oj4TiKT3IfOH2wcLWI4CWfTkZbG7QwZGbxn3Nokp3orA.BpQq_0..AAA.0.0.BpQq_0.AWczvMVsV2yYbsB7kkvkAGk6ois";
            
            if (cUser.equals("your_c_user_value") || xs.equals("your_xs_value")) {
                System.err.println("ERROR: Please update Facebook cookies in TestCrawler.java");
                System.err.println("\nHow to get your cookies:");
                System.err.println("1. Open Facebook in Chrome and login");
                System.err.println("2. Press F12 to open Developer Tools");
                System.err.println("3. Go to 'Application' tab -> 'Cookies' -> 'https://www.facebook.com'");
                System.err.println("4. Find and copy values for: c_user, xs, fr");
                return;
            }
            
            // Choose URL to crawl
            // String postUrl = "https://www.facebook.com/share/p/17GAV3Q9ZA/";  // Regular post
            String postUrl = "https://www.facebook.com/share/p/1GYUos3v6x/";  // Reel
            
            crawler = new FacebookCrawler();
            
            System.out.println("Step 1: Logging into Facebook with cookies...");
            boolean loginSuccess = crawler.loginWithCookies(cUser, xs, fr);
            
            if (!loginSuccess) {
                System.err.println("Login failed! Cookies may be invalid or expired.");
                return;
            }
            
            System.out.println("\nStep 2: Starting to crawl comments from:");
            System.out.println(postUrl);
            System.out.println("\nPlease wait, this may take a few moments...\n");
            
            List<Comment> comments = crawler.crawlComments(postUrl);
            
            System.out.println("\n=== Crawling Results ===");
            System.out.println("Total comments found: " + comments.size());
            
            if (comments.isEmpty()) {
                System.out.println("\nNo comments were extracted.");
                System.out.println("\nPossible reasons:");
                System.out.println("1. The post may require login to view comments");
                System.out.println("2. Facebook's HTML structure may have changed");
                System.out.println("3. Try increasing MAX_SCROLL_ATTEMPTS and SCROLL_PAUSE_TIME");
                System.out.println("4. Facebook may be blocking automated access");
            } else {
                // Export to CSV
                String csvFilename = "comments.csv";
                FacebookCrawler.exportToCsv(comments, csvFilename);
                
                // Also print first 5 comments as preview
                System.out.println("\n=== Preview (First 5 Comments) ===\n");
                int previewCount = Math.min(5, comments.size());
                for (int i = 0; i < previewCount; i++) {
                    System.out.println("--- Comment #" + (i + 1) + " ---");
                    System.out.println(comments.get(i));
                }
                
                if (comments.size() > 5) {
                    System.out.println("... and " + (comments.size() - 5) + " more comments");
                    System.out.println("Check " + csvFilename + " for all comments");
                }
                
                // Print statistics
                System.out.println("\n=== Statistics ===");
                long commentsWithReplies = comments.stream()
                    .filter(Comment::hasReply)
                    .count();
                long replyComments = comments.stream()
                    .filter(Comment::isReply)
                    .count();
                long topLevelComments = comments.size() - replyComments;
                
                System.out.println("Top-level comments: " + topLevelComments);
                System.out.println("Reply comments: " + replyComments);
                System.out.println("Comments with replies: " + commentsWithReplies);
                System.out.println("Comments without replies: " + (topLevelComments - commentsWithReplies));
                
                System.out.println("\n✓ All comments have been saved to: " + csvFilename);
            }
            
        } catch (Exception e) {
            System.err.println("\nError occurred during crawling:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (crawler != null) {
                System.out.println("\nClosing browser...");
                crawler.close();
                System.out.println("Done!");
            }
        }
    }
    
    // Alternative test method with custom URL and cookies
    public static void testWithCustomUrl(String url, String cUser, String xs, String fr) {
        FacebookCrawler crawler = null;
        
        try {
            crawler = new FacebookCrawler();
            
            System.out.println("Logging in with cookies...");
            if (!crawler.loginWithCookies(cUser, xs, fr)) {
                System.err.println("Login failed!");
                return;
            }
            
            System.out.println("Crawling: " + url);
            
            List<Comment> comments = crawler.crawlComments(url);
            
            System.out.println("\nFound " + comments.size() + " comments:");
            for (Comment comment : comments) {
                System.out.println("- " + comment.getAuthor() + ": " + comment.getContent());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (crawler != null) {
                crawler.close();
            }
        }
    }
    
    // Method to test with environment variables (more secure)
    public static void testWithEnvVariables() {
        String cUser = System.getenv("FB_C_USER");
        String xs = System.getenv("FB_XS");
        String fr = System.getenv("FB_FR");
        String url = System.getenv("FB_POST_URL");
        
        if (cUser == null || xs == null) {
            System.err.println("Please set environment variables:");
            System.err.println("  FB_C_USER=your_c_user_cookie");
            System.err.println("  FB_XS=your_xs_cookie");
            System.err.println("  FB_FR=your_fr_cookie (optional)");
            System.err.println("  FB_POST_URL=https://www.facebook.com/...");
            return;
        }
        
        if (url == null) {
            url = "https://www.facebook.com/share/p/17GAV3Q9ZA/";
        }
        
        testWithCustomUrl(url, cUser, xs, fr);
    }
    
    // Helper method to extract cookies from a logged-in session
    public static void extractCookiesFromBrowser() {
        FacebookCrawler crawler = null;
        
        try {
            System.out.println("=== Cookie Extractor ===");
            System.out.println("This will open Facebook. Please login manually.");
            System.out.println("After logging in, press Enter in this console...\n");
            
            crawler = new FacebookCrawler();
            crawler.getDriver().get("https://www.facebook.com/");
            
            // Wait for user to login manually
            System.out.println("Waiting for manual login...");
            System.out.println("Press Enter after you've logged in:");
            System.in.read();
            
            // Print the cookies
            crawler.printCurrentCookies();
            
            System.out.println("Copy these cookie values to TestCrawler.java");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (crawler != null) {
                System.out.println("\nClosing in 10 seconds...");
                try { Thread.sleep(10000); } catch (Exception e) {}
                crawler.close();
            }
        }
    }
}