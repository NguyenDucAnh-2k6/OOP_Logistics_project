package com.oop.logistics.scraping;

import com.oop.logistics.Facebook.FacebookPost;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Facebook scraper using JSoup
 * Limited functionality - works best for mbasic.facebook.com (mobile version)
 * Note: Facebook's main site requires JavaScript, so this has limitations
 */
public class FacebookScraperJSoup {

    private static final String MOBILE_BASE_URL = "https://mbasic.facebook.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36";
    
    private String cookies = "";

    public FacebookScraperJSoup() {}

    /**
     * Scrape public page posts from mobile Facebook
     * This works without login for public pages
     */
    public List<FacebookPost> scrapePublicPagePosts(String pageUsername, int maxPosts) {
        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            String url = MOBILE_BASE_URL + "/" + pageUsername;
            System.out.println("Scraping: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .cookies(parseCookies(cookies))
                .get();
            
            // Find post elements in mobile version
            Elements postElements = doc.select("article, div[data-ft]");
            
            System.out.println("Found " + postElements.size() + " potential posts");
            
            int count = 0;
            for (Element postElement : postElements) {
                if (count >= maxPosts) break;
                
                try {
                    FacebookPost post = extractPostFromMobileElement(postElement);
                    if (post != null && post.getMessage() != null) {
                        posts.add(post);
                        count++;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error scraping page: " + e.getMessage());
        }
        
        return posts;
    }

    /**
     * Extract post from mobile Facebook element
     */
    private FacebookPost extractPostFromMobileElement(Element element) {
        FacebookPost post = new FacebookPost();
        
        try {
            // Extract post text
            Element contentElement = element.selectFirst("div[data-ft] > div > div");
            if (contentElement != null) {
                String text = contentElement.text();
                if (text.length() > 10) {
                    post.setMessage(text);
                }
            }
            
            // Extract post ID from permalink
            Element linkElement = element.selectFirst("a[href*='/story.php'], a[href*='/posts/']");
            if (linkElement != null) {
                String href = linkElement.attr("href");
                post.setId(extractPostIdFromUrl(href));
            } else {
                post.setId("mobile_" + System.currentTimeMillis());
            }
            
            // Extract timestamp
            Element timeElement = element.selectFirst("abbr");
            if (timeElement != null) {
                post.setCreatedTime(timeElement.text());
            }
            
            // Try to extract engagement (limited on mobile)
            String html = element.html();
            extractEngagementFromHtml(html, post);
            
        } catch (Exception e) {
            System.err.println("Error extracting mobile post: " + e.getMessage());
        }
        
        return post.getMessage() != null ? post : null;
    }

    /**
     * Extract post ID from URL
     */
    private String extractPostIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("story_fbid=(\\d+)|posts/(\\d+)|/permalink/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String id = matcher.group(i);
                if (id != null) return id;
            }
        }
        
        return "unknown_" + url.hashCode();
    }

    /**
     * Extract engagement metrics from HTML text
     */
    private void extractEngagementFromHtml(String html, FacebookPost post) {
        // Look for reaction counts
        Pattern reactionPattern = Pattern.compile("(\\d+)\\s+(?:reaction|like)", Pattern.CASE_INSENSITIVE);
        Matcher reactionMatcher = reactionPattern.matcher(html);
        if (reactionMatcher.find()) {
            post.setLikes(Integer.parseInt(reactionMatcher.group(1)));
        }
        
        // Look for comment counts
        Pattern commentPattern = Pattern.compile("(\\d+)\\s+comment", Pattern.CASE_INSENSITIVE);
        Matcher commentMatcher = commentPattern.matcher(html);
        if (commentMatcher.find()) {
            post.setComments(Integer.parseInt(commentMatcher.group(1)));
        }
        
        // Look for share counts
        Pattern sharePattern = Pattern.compile("(\\d+)\\s+share", Pattern.CASE_INSENSITIVE);
        Matcher shareMatcher = sharePattern.matcher(html);
        if (shareMatcher.find()) {
            post.setShares(Integer.parseInt(shareMatcher.group(1)));
        }
    }

    /**
     * Search for posts (requires login cookies)
     */
    public List<FacebookPost> searchPosts(String keyword, int maxResults) {
        List<FacebookPost> posts = new ArrayList<>();
        
        try {
            String searchUrl = MOBILE_BASE_URL + "/search/posts/?q=" + 
                              java.net.URLEncoder.encode(keyword, "UTF-8");
            
            Document doc = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .cookies(parseCookies(cookies))
                .get();
            
            Elements postElements = doc.select("article");
            
            int count = 0;
            for (Element postElement : postElements) {
                if (count >= maxResults) break;
                
                FacebookPost post = extractPostFromMobileElement(postElement);
                if (post != null) {
                    posts.add(post);
                    count++;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error searching: " + e.getMessage());
        }
        
        return posts;
    }

    /**
     * Set cookies for authenticated requests
     * Get these from your browser after logging in:
     * 1. Login to Facebook in browser
     * 2. Press F12 -> Application -> Cookies
     * 3. Copy c_user and xs values
     */
    public void setCookies(String cookieString) {
        this.cookies = cookieString;
    }

    /**
     * Parse cookie string to map
     */
    private java.util.Map<String, String> parseCookies(String cookieString) {
        java.util.Map<String, String> cookieMap = new java.util.HashMap<>();
        
        if (cookieString == null || cookieString.isEmpty()) {
            return cookieMap;
        }
        
        String[] pairs = cookieString.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                cookieMap.put(keyValue[0], keyValue[1]);
            }
        }
        
        return cookieMap;
    }

    /**
     * Get page HTML for debugging
     */
    public String getPageHtml(String url) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get();
            
            return doc.html();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Check if scraper is accessible
     */
    public boolean isAccessible() {
        try {
            Document doc = Jsoup.connect(MOBILE_BASE_URL)
                .userAgent(USER_AGENT)
                .timeout(5000)
                .get();
            
            return doc != null;
        } catch (Exception e) {
            return false;
        }
    }
}