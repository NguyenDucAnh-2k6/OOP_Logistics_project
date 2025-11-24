package com.oop.logistics;

import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.config.CategoryManager;
import com.oop.logistics.config.KeywordManager;
import com.oop.logistics.core.*;
import com.oop.logistics.datasources.FacebookDataSource;
import com.oop.logistics.datasources.TwitterDataSource;
import com.oop.logistics.ui.DashboardFrame;
import javax.swing.*;
import java.util.Arrays;

/**
 * Main application entry point
 * Initializes and launches the disaster logistics system
 */
public class DisasterLogisticsApp {

    private DisasterLogisticsPipeline pipeline;
    private KeywordManager keywordManager;
    private CategoryManager categoryManager;
    private DashboardFrame dashboardFrame;

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch application
        SwingUtilities.invokeLater(() -> {
            DisasterLogisticsApp app = new DisasterLogisticsApp();
            app.initialize();
            app.launch();
        });
    }

    /**
     * Initialize the application
     */
    private void initialize() {
        System.out.println("=== Disaster Logistics Management System ===");
        System.out.println("Initializing components...\n");

        // Initialize configuration managers
        keywordManager = new KeywordManager();
        categoryManager = new CategoryManager();

        // Try to load custom configurations
        loadConfigurations();

        // Initialize sentiment analysis API
        PythonAnalysisClient analysisAPI = initializeAnalysisAPI();

        // Initialize pipeline
        pipeline = new DisasterLogisticsPipeline(
            keywordManager,
            categoryManager,
            analysisAPI
        );

        // Configure data sources
        configureDataSources();

        System.out.println("\nApplication initialized successfully");
        System.out.println("Collection Stats: " + pipeline.getCollectionStats());
    }

    /**
     * Load configurations from files if available
     */
    private void loadConfigurations() {
        try {
            // Try to load keywords from file
            java.io.File keywordsFile = new java.io.File("config/keywords.json");
            if (keywordsFile.exists()) {
                keywordManager.loadFromJson("config/keywords.json");
                System.out.println("✓ Loaded keywords from config/keywords.json");
            } else {
                System.out.println("✓ Using default keywords");
            }

            // Try to load categories from file
            java.io.File categoriesFile = new java.io.File("config/categories.json");
            if (categoriesFile.exists()) {
                categoryManager.loadFromJson("config/categories.json");
                System.out.println("✓ Loaded categories from config/categories.json");
            } else {
                System.out.println("✓ Using default categories");
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading configurations: " + e.getMessage());
            System.out.println("Using default configurations");
        }
    }

    /**
     * Initialize sentiment analysis API
     */
    private PythonAnalysisClient initializeAnalysisAPI() {
        String apiUrl = System.getenv("SENTIMENT_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "http://localhost:5000";
        }

        PythonAnalysisClient analysisAPI = new PythonAnalysisClient(apiUrl);

        if (analysisAPI.isAvailable()) {
            System.out.println("✓ Sentiment Analysis API connected at " + apiUrl);
        } else {
            System.out.println("⚠ Sentiment Analysis API not available at " + apiUrl);
            System.out.println("  Sentiment analysis will be skipped");
        }

        return analysisAPI;
    }

    /**
     * Configure all data sources
     */
    private void configureDataSources() {
        System.out.println("\nConfiguring data sources...");

        // Configure Facebook
        configureFacebookSource();

        // Configure Twitter
        configureTwitterSource();

        System.out.println("");
    }

    /**
     * Configure Facebook data source
     */
    private void configureFacebookSource() {
        try {
            String accessToken = System.getenv("FACEBOOK_ACCESS_TOKEN");
            String pageId = System.getenv("FACEBOOK_PAGE_ID");

            if (accessToken != null && !accessToken.isEmpty() &&
                pageId != null && !pageId.isEmpty()) {

                FacebookDataSource facebookSource = new FacebookDataSource();

                SourceConfiguration config = new SourceConfiguration();
                config.setProperty("access_token", accessToken);
                config.setProperty("page_id", pageId);

                facebookSource.configure(config);
                pipeline.registerDataSource(facebookSource);

                System.out.println("✓ Facebook data source configured");
            } else {
                System.out.println("⚠ Facebook: Missing credentials");
                System.out.println("  Set FACEBOOK_ACCESS_TOKEN and FACEBOOK_PAGE_ID environment variables");
            }
        } catch (Exception e) {
            System.err.println("✗ Facebook configuration error: " + e.getMessage());
        }
    }

    /**
     * Configure Twitter data source
     */
    private void configureTwitterSource() {
        try {
            String bearerToken = System.getenv("TWITTER_BEARER_TOKEN");

            if (bearerToken != null && !bearerToken.isEmpty()) {

                TwitterDataSource twitterSource = new TwitterDataSource(keywordManager);

                SourceConfiguration config = new SourceConfiguration();
                config.setProperty("bearer_token", bearerToken);
                config.setProperty("max_results", 100);
                config.setProperty("viral_threshold", 100);

                // Add custom search keywords
                config.setProperty("keywords", Arrays.asList(
                    "#lũlụt",
                    "#bão",
                    "#thiêntai",
                    "Vietnam flood",
                    "Vietnam disaster"
                ));

                // Add monitored accounts (disaster relief organizations)
                config.setProperty("monitored_accounts", Arrays.asList(
                    "RedCrossVietnam",
                    "UNDP_Vietnam"
                ));

                twitterSource.configure(config);

                // Test connection
                if (twitterSource.testConnection()) {
                    pipeline.registerDataSource(twitterSource);
                    System.out.println("✓ Twitter data source configured and connected");
                } else {
                    pipeline.registerDataSource(twitterSource);
                    System.out.println("⚠ Twitter: Configured but API connection may be limited");
                }
            } else {
                System.out.println("⚠ Twitter: Missing credentials");
                System.out.println("  Set TWITTER_BEARER_TOKEN environment variable");
            }
        } catch (Exception e) {
            System.err.println("✗ Twitter configuration error: " + e.getMessage());
        }
    }

    /**
     * Launch the GUI
     */
    private void launch() {
        dashboardFrame = new DashboardFrame(pipeline);
        dashboardFrame.setVisible(true);
    }

    /**
     * Shutdown the application
     */
    public void shutdown() {
        if (pipeline != null) {
            pipeline.shutdown();
        }
        System.out.println("Application shutdown complete");
    }

    /**
     * Get the pipeline instance
     */
    public DisasterLogisticsPipeline getPipeline() {
        return pipeline;
    }

    /**
     * Get keyword manager
     */
    public KeywordManager getKeywordManager() {
        return keywordManager;
    }

    /**
     * Get category manager
     */
    public CategoryManager getCategoryManager() {
        return categoryManager;
    }
}

/**
 * Command line runner for testing without GUI
 */
class CommandLineRunner {

    public static void main(String[] args) {
        System.out.println("=== Disaster Logistics CLI ===\n");
        runPipeline();
    }

    public static void runPipeline() {
        // Initialize components
        KeywordManager keywordManager = new KeywordManager();
        CategoryManager categoryManager = new CategoryManager();
        PythonAnalysisClient analysisAPI =
            new PythonAnalysisClient("http://localhost:5000");

        // Create pipeline
        DisasterLogisticsPipeline pipeline = new DisasterLogisticsPipeline(
            keywordManager,
            categoryManager,
            analysisAPI
        );

        // Configure Twitter source
        String bearerToken = System.getenv("TWITTER_BEARER_TOKEN");
        if (bearerToken != null) {
            TwitterDataSource twitterSource = new TwitterDataSource(keywordManager);
            SourceConfiguration config = new SourceConfiguration();
            config.setProperty("bearer_token", bearerToken);
            twitterSource.configure(config);
            pipeline.registerDataSource(twitterSource);
            System.out.println("Twitter source configured");
        } else {
            System.out.println("TWITTER_BEARER_TOKEN not set - skipping Twitter");
        }

        // Configure Facebook source
        String fbToken = System.getenv("FACEBOOK_ACCESS_TOKEN");
        String fbPage = System.getenv("FACEBOOK_PAGE_ID");
        if (fbToken != null && fbPage != null) {
            FacebookDataSource facebookSource = new FacebookDataSource();
            SourceConfiguration config = new SourceConfiguration();
            config.setProperty("access_token", fbToken);
            config.setProperty("page_id", fbPage);
            facebookSource.configure(config);
            pipeline.registerDataSource(facebookSource);
            System.out.println("Facebook source configured");
        } else {
            System.out.println("Facebook credentials not set - skipping Facebook");
        }

        // Check data sources
        var stats = pipeline.getCollectionStats();
        System.out.println("\nCollection Stats: " + stats);

        if (stats.getActiveSources() == 0) {
            System.err.println("\nNo data sources configured!");
            System.err.println("Set environment variables:");
            System.err.println("  - TWITTER_BEARER_TOKEN");
            System.err.println("  - FACEBOOK_ACCESS_TOKEN and FACEBOOK_PAGE_ID");
            pipeline.shutdown();
            return;
        }

        // Execute pipeline
        System.out.println("\nExecuting pipeline...\n");
        DisasterLogisticsPipeline.PipelineResult result = pipeline.execute();

        if (result.isSuccess()) {
            System.out.println("=== Pipeline Results ===");
            System.out.println("Raw events collected: " + result.getRawEventCount());
            System.out.println("Processed events: " + result.getProcessedEventCount());

            var report = result.getAnalysisReport();
            System.out.println("\n=== Analysis Report ===");
            System.out.println("Total events: " + report.getTotalEvents());
            System.out.println("\nEvents by Type:");
            report.getEventsByType().forEach((type, count) ->
                System.out.println("  " + type + ": " + count));

            System.out.println("\nEvents by Location:");
            report.getEventsByLocation().forEach((location, count) ->
                System.out.println("  " + location + ": " + count));

            System.out.println("\nEvents by Severity:");
            report.getEventsBySeverity().forEach((severity, count) ->
                System.out.println("  " + severity + ": " + count));

            System.out.println("\nAverage Engagement: " +
                String.format("%.2f", report.getAverageEngagement()));

            System.out.println("\n=== Top 5 Events ===");
            report.getTopEngagementEvents().stream()
                .limit(5)
                .forEach(event -> System.out.println("  - " + event.getDisasterType() +
                    " at " + event.getLocation() +
                    " (Engagement: " + event.getEngagement() + ")"));

        } else {
            System.err.println("Pipeline failed: " + result.getError());
        }

        // Shutdown
        pipeline.shutdown();
        System.out.println("\nDone!");
    }
}