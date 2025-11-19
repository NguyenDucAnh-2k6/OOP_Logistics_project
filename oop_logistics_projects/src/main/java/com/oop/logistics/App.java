package com.oop.logistics;

import com.oop.logistics.Facebook.FacebookService;
import com.oop.logistics.Facebook.FacebookPost;
import com.oop.logistics.models.DisasterEvent;
import java.util.List;
import java.util.Scanner;

public class App {
    
    // IMPORTANT: Replace with your actual Facebook Access Token
    private static final String ACCESS_TOKEN = "EAAdllrHFvAYBP1mZAlNTIYHoUpWGckMwmY07S8fxYBaPYyLL01k4tNJ7j1ox9gvWTu5HTZCMB7t0d7ozWSNZBr3Jnq665L9JZBoovdl73PmMR8vGiZAxUKFyV2wmHTlONsZCMObfAqQlrwFTfeny25dt9xY5AsTbZA0aFpGc9NgquVOQAikgwLo6DM3UvUZCLceAUZBp7EP0P0R8osDdu6lSP2ySSoCYWQt9mmmsZAVzgXOf2TRAwQ6G8WXtCxhbOMo36t0iCb7kZCzwSqicR9nvSCP";
    
    // Example Facebook Page IDs (replace with actual pages you want to monitor)
    private static final String[] TEST_PAGE_IDS = {
        "649030828",           // VnExpress
        "100064688586481",          // BBC Vietnamese
        "100064689941048"             // Thanh Nien
    };

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("   DISASTER LOGISTICS MONITORING SYSTEM");
        System.out.println("   Facebook Data Collection & Analysis Demo");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Check if access token is set
        if (ACCESS_TOKEN.equals("YOUR_ACCESS_TOKEN_HERE")) {
            System.out.println("❌ ERROR: Please set your Facebook Access Token in App.java");
            System.out.println("\nHow to get Access Token:");
            System.out.println("1. Go to https://developers.facebook.com/tools/explorer/");
            System.out.println("2. Select your app or create a new one");
            System.out.println("3. Generate an access token with 'pages_read_engagement' permission");
            System.out.println("4. Copy the token and paste it in App.java\n");
            return;
        }

        // Initialize service
        FacebookService facebookService = new FacebookService(ACCESS_TOKEN);
        
        // Show menu
        showMenu(facebookService);
    }

    private static void showMenu(FacebookService facebookService) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n┌─────────────────────────────────────┐");
            System.out.println("│          MAIN MENU                  │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.println("1. Fetch All Posts from a Page");
            System.out.println("2. Detect Disaster-Related Posts");
            System.out.println("3. Extract Disaster Events");
            System.out.println("4. Find Viral Disaster Posts");
            System.out.println("5. Analyze Multiple Pages");
            System.out.println("6. Run Full Demo (All Features)");
            System.out.println("0. Exit");
            System.out.print("\nSelect an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    fetchAllPosts(scanner, facebookService);
                    break;
                case "2":
                    detectDisasterPosts(scanner, facebookService);
                    break;
                case "3":
                    extractDisasterEvents(scanner, facebookService);
                    break;
                case "4":
                    findViralPosts(scanner, facebookService);
                    break;
                case "5":
                    analyzeMultiplePages(facebookService);
                    break;
                case "6":
                    runFullDemo(facebookService);
                    break;
                case "0":
                    System.out.println("\n✓ Exiting... Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("\n❌ Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    // Feature 1: Fetch all posts from a page
    private static void fetchAllPosts(Scanner scanner, FacebookService facebookService) {
        System.out.print("\nEnter Facebook Page ID: ");
        String pageId = scanner.nextLine().trim();

        System.out.println("\n🔄 Fetching posts from page: " + pageId + "...\n");

        List<FacebookPost> posts = facebookService.fetchPagePosts(pageId);

        if (posts.isEmpty()) {
            System.out.println("❌ No posts found or error occurred.");
            return;
        }

        System.out.println("✓ Found " + posts.size() + " posts:\n");
        
        int count = 1;
        for (FacebookPost post : posts) {
            System.out.println("─────────────────────────────────────");
            System.out.println("Post #" + count++);
            System.out.println("ID: " + post.getId());
            System.out.println("Created: " + post.getCreatedTime());
            System.out.println("Message: " + truncateMessage(post.getMessage()));
            System.out.println("Engagement: 👍 " + post.getLikes() + 
                             " | 💬 " + post.getComments() + 
                             " | 🔄 " + post.getShares());
        }
        System.out.println("─────────────────────────────────────");
    }

    // Feature 2: Detect disaster-related posts
    private static void detectDisasterPosts(Scanner scanner, FacebookService facebookService) {
        System.out.print("\nEnter Facebook Page ID: ");
        String pageId = scanner.nextLine().trim();

        System.out.println("\n🔍 Analyzing posts for disaster-related content...\n");

        List<FacebookPost> disasterPosts = facebookService.detectDisasterPosts(pageId);

        if (disasterPosts.isEmpty()) {
            System.out.println("✓ No disaster-related posts detected.");
            return;
        }

        System.out.println("⚠️  Found " + disasterPosts.size() + " disaster-related posts:\n");

        int count = 1;
        for (FacebookPost post : disasterPosts) {
            System.out.println("═════════════════════════════════════");
            System.out.println("🚨 Disaster Post #" + count++);
            System.out.println("ID: " + post.getId());
            System.out.println("Created: " + post.getCreatedTime());
            System.out.println("Message: " + truncateMessage(post.getMessage()));
            System.out.println("Engagement: 👍 " + post.getLikes() + 
                             " | 💬 " + post.getComments() + 
                             " | 🔄 " + post.getShares());
        }
        System.out.println("═════════════════════════════════════");
    }

    // Feature 3: Extract disaster events
    private static void extractDisasterEvents(Scanner scanner, FacebookService facebookService) {
        System.out.print("\nEnter Facebook Page ID: ");
        String pageId = scanner.nextLine().trim();

        System.out.println("\n🔍 Extracting disaster events from posts...\n");

        List<DisasterEvent> events = facebookService.extractDisasterEvents(pageId);

        if (events.isEmpty()) {
            System.out.println("✓ No disaster events extracted.");
            return;
        }

        System.out.println("📊 Extracted " + events.size() + " disaster events:\n");

        int count = 1;
        for (DisasterEvent event : events) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌪️  Event #" + count++);
            System.out.println("Type: " + event.getDisasterType());
            System.out.println("Source: " + event.getSource() + " (ID: " + event.getSourceId() + ")");
            System.out.println("Time: " + event.getTimestamp());
            System.out.println("Description: " + truncateMessage(event.getDescription()));
            System.out.println("Total Engagement: " + event.getEngagement());
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // Feature 4: Find viral disaster posts
    private static void findViralPosts(Scanner scanner, FacebookService facebookService) {
        System.out.print("\nEnter Facebook Page ID: ");
        String pageId = scanner.nextLine().trim();
        
        System.out.print("Enter minimum engagement threshold (e.g., 100): ");
        int minEngagement = 100;
        try {
            minEngagement = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, using default: 100");
        }

        System.out.println("\n🔥 Finding viral disaster posts (engagement ≥ " + minEngagement + ")...\n");

        List<FacebookPost> viralPosts = facebookService.getViralDisasterPosts(pageId, minEngagement);

        if (viralPosts.isEmpty()) {
            System.out.println("✓ No viral disaster posts found with engagement ≥ " + minEngagement);
            return;
        }

        System.out.println("🔥 Found " + viralPosts.size() + " viral disaster posts:\n");

        int count = 1;
        for (FacebookPost post : viralPosts) {
            int totalEngagement = post.getLikes() + post.getComments() + post.getShares();
            System.out.println("╔═══════════════════════════════════════╗");
            System.out.println("║  🔥 VIRAL POST #" + count++ + "                    ║");
            System.out.println("╚═══════════════════════════════════════╝");
            System.out.println("ID: " + post.getId());
            System.out.println("Created: " + post.getCreatedTime());
            System.out.println("Message: " + truncateMessage(post.getMessage()));
            System.out.println("Engagement: 👍 " + post.getLikes() + 
                             " | 💬 " + post.getComments() + 
                             " | 🔄 " + post.getShares() +
                             " | Total: " + totalEngagement);
        }
        System.out.println("╚═══════════════════════════════════════╝");
    }

    // Feature 5: Analyze multiple pages
    private static void analyzeMultiplePages(FacebookService facebookService) {
        System.out.println("\n📊 Analyzing multiple Facebook pages for disaster content...\n");

        int totalPosts = 0;
        int totalDisasterPosts = 0;
        int totalEvents = 0;

        for (String pageId : TEST_PAGE_IDS) {
            System.out.println("─────────────────────────────────────");
            System.out.println("📄 Analyzing page: " + pageId);
            
            List<FacebookPost> posts = facebookService.fetchPagePosts(pageId);
            List<FacebookPost> disasterPosts = facebookService.detectDisasterPosts(pageId);
            List<DisasterEvent> events = facebookService.extractDisasterEvents(pageId);

            totalPosts += posts.size();
            totalDisasterPosts += disasterPosts.size();
            totalEvents += events.size();

            System.out.println("  • Total posts: " + posts.size());
            System.out.println("  • Disaster posts: " + disasterPosts.size());
            System.out.println("  • Events extracted: " + events.size());
        }

        System.out.println("─────────────────────────────────────");
        System.out.println("\n📈 SUMMARY:");
        System.out.println("  Total pages analyzed: " + TEST_PAGE_IDS.length);
        System.out.println("  Total posts: " + totalPosts);
        System.out.println("  Total disaster posts: " + totalDisasterPosts);
        System.out.println("  Total events extracted: " + totalEvents);
        
        if (totalPosts > 0) {
            double percentage = (totalDisasterPosts * 100.0) / totalPosts;
            System.out.printf("  Disaster content rate: %.2f%%\n", percentage);
        }
    }

    // Feature 6: Run full demo
    private static void runFullDemo(FacebookService facebookService) {
        System.out.println("\n🚀 Running Full Demo with all features...\n");

        String demoPageId = TEST_PAGE_IDS[0];
        System.out.println("Using demo page: " + demoPageId + "\n");

        // 1. Fetch posts
        System.out.println("1️⃣  Fetching all posts...");
        List<FacebookPost> allPosts = facebookService.fetchPagePosts(demoPageId);
        System.out.println("   ✓ Found " + allPosts.size() + " posts\n");

        // 2. Detect disaster posts
        System.out.println("2️⃣  Detecting disaster-related posts...");
        List<FacebookPost> disasterPosts = facebookService.detectDisasterPosts(demoPageId);
        System.out.println("   ⚠️  Found " + disasterPosts.size() + " disaster posts\n");

        // 3. Extract events
        System.out.println("3️⃣  Extracting disaster events...");
        List<DisasterEvent> events = facebookService.extractDisasterEvents(demoPageId);
        System.out.println("   📊 Extracted " + events.size() + " events\n");

        // 4. Find viral posts
        System.out.println("4️⃣  Finding viral disaster posts (engagement ≥ 50)...");
        List<FacebookPost> viralPosts = facebookService.getViralDisasterPosts(demoPageId, 50);
        System.out.println("   🔥 Found " + viralPosts.size() + " viral posts\n");

        // Show sample results
        if (!disasterPosts.isEmpty()) {
            System.out.println("═══════════════════════════════════════");
            System.out.println("📋 SAMPLE DISASTER POST:");
            FacebookPost samplePost = disasterPosts.get(0);
            System.out.println("Message: " + truncateMessage(samplePost.getMessage()));
            System.out.println("Created: " + samplePost.getCreatedTime());
            System.out.println("Engagement: 👍 " + samplePost.getLikes() + 
                             " | 💬 " + samplePost.getComments() + 
                             " | 🔄 " + samplePost.getShares());
            System.out.println("═══════════════════════════════════════");
        }

        if (!events.isEmpty()) {
            System.out.println("\n📋 SAMPLE DISASTER EVENT:");
            DisasterEvent sampleEvent = events.get(0);
            System.out.println("Type: " + sampleEvent.getDisasterType());
            System.out.println("Description: " + truncateMessage(sampleEvent.getDescription()));
            System.out.println("Total Engagement: " + sampleEvent.getEngagement());
            System.out.println("═══════════════════════════════════════");
        }

        System.out.println("\n✅ Full demo completed!");
    }

    // Helper method to truncate long messages
    private static String truncateMessage(String message) {
        if (message == null) {
            return "[No message]";
        }
        if (message.length() > 150) {
            return message.substring(0, 147) + "...";
        }
        return message;
    }
}