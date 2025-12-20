import com.oop.logistics.crawler.*;
import com.oop.logistics.crawler.FacebookCrawler.Comment;
import java.util.List;

public class TestCrawler {    
    // Alternative test method with custom URL
    public static void testWithCustomUrl(String url) {
        FacebookCrawler crawler = null;
        
        try {
            crawler = new FacebookCrawler();
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
    public static void main(String[] args) {
        FacebookCrawler crawler = null;
        
        try {
            System.out.println("=== Facebook Comment Crawler Test ===\n");
            
            String postUrl = "https://www.facebook.com/share/p/17GAV3Q9ZA/";
            
            crawler = new FacebookCrawler();
            
            System.out.println("Starting to crawl comments from:");
            System.out.println(postUrl);
            System.out.println("\nPlease wait, this may take a few moments...\n");
            
            List<Comment> comments = crawler.crawlComments(postUrl);
            
            System.out.println("\n=== Crawling Results ===");
            System.out.println("Total comments found: " + comments.size());
            System.out.println("\n=== Comments Details ===\n");
            
            if (comments.isEmpty()) {
                System.out.println("No comments were extracted.");
                System.out.println("\nPossible reasons:");
                System.out.println("1. The post may require login to view comments");
                System.out.println("2. Facebook's HTML structure may have changed");
                System.out.println("3. The page may need more time to load");
                System.out.println("4. Facebook may be blocking automated access");
            } else {
                int count = 1;
                for (Comment comment : comments) {
                    System.out.println("--- Comment #" + count + " ---");
                    System.out.println(comment);
                    count++;
                }
                
                // Print statistics
                System.out.println("\n=== Statistics ===");
                long commentsWithReplies = comments.stream()
                    .filter(Comment::hasReply)
                    .count();
                System.out.println("Comments with replies: " + commentsWithReplies);
                System.out.println("Comments without replies: " + (comments.size() - commentsWithReplies));
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
}