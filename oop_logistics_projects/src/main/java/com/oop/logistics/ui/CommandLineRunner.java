package com.oop.logistics.ui;

import java.util.ArrayList;
import java.util.List;

import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.config.CategoryManager;
import com.oop.logistics.config.KeywordManager;
import com.oop.logistics.core.DisasterLogisticsPipeline;
import com.oop.logistics.core.SourceConfiguration;
import com.oop.logistics.datasources.FacebookDataSourceWithScraping;
import com.oop.logistics.models.DisasterAnalysisReport;
import com.oop.logistics.models.DisasterEvent;
import com.oop.logistics.utils.CsvExporter;

public class CommandLineRunner {    
    public static void main(String[] args) {
        // --- 1. INITIALIZE COMPONENTS ---
        KeywordManager keywordManager = new KeywordManager();
        CategoryManager categoryManager = new CategoryManager();
        PythonAnalysisClient analysisAPI =
            new PythonAnalysisClient("http://localhost:5000"); //

        // Create pipeline
        DisasterLogisticsPipeline pipeline = new DisasterLogisticsPipeline(
            keywordManager,
            categoryManager,
            analysisAPI
        );

        // --- 2. CONFIGURE FACEBOOK SCRAPING (for comments) ---
        // NOTE: This assumes you are using FacebookDataSourceWithScraping
        String disasterKeyword = "Yagi"; // The disaster keyword
        String fbPageUsername = "BBCnewsVietnamese"; // A high-traffic page/source
        List<DisasterEvent> finalEvents = new ArrayList<>();
        FacebookDataSourceWithScraping facebookSource = null;
        
        try {
            facebookSource = new FacebookDataSourceWithScraping();
            SourceConfiguration config = new SourceConfiguration();
            
            // Set the necessary configuration for Selenium comment scraping
            config.setProperty("mode", "SELENIUM"); // Must be SELENIUM mode for comment scraping
            config.setProperty("page_username", fbPageUsername);
            config.setProperty("headless", 1); // Run headless (1=true)
            
            facebookSource.configure(config);

            System.out.println("\n✓ Starting targeted scraping for comments...");
            // Assuming the implementation from the previous step is now available in FacebookDataSourceWithScraping
            finalEvents = facebookSource.fetchDisasterCommentsForAnalysis(
                disasterKeyword, 
                10,   // Max 10 relevant posts
                50    // Max 50 comments per post
            );

        } catch (Exception e) {
            System.err.println("✗ Facebook Scraping failed: " + e.getMessage());
        } finally {
            if (facebookSource != null) {
                facebookSource.cleanup(); // Close the Selenium browser instance
            }
        }
        
        if (finalEvents.isEmpty()) {
            System.err.println("\nNo data collected. Exiting pipeline.");
            pipeline.shutdown();
            return;
        }

        // --- 3. RUN ANALYSIS (Preprocessing, Location, Sentiment) ---
        System.out.println("\n✓ Running analysis pipeline on collected comments (" + finalEvents.size() + " events)...");
        List<DisasterEvent> processedEvents = pipeline.dataPreprocessor.preprocess(finalEvents);
        DisasterAnalysisReport report = pipeline.disasterAnalyzer.analyzeEvents(processedEvents);

        // --- 4. EXPORT TO CSV ---
        try {
            CsvExporter.exportEventsToCsv(processedEvents, "yagi_storm_sentiment_timeline.csv");
        } catch (Exception e) {
            System.err.println("FATAL: Could not export data to CSV: " + e.getMessage());
        }

        // --- 5. PRINT REPORT (for CLI verification) ---
        // (You can copy the original print logic here to see the results immediately)

        pipeline.shutdown();
        System.out.println("\nDone!");
    }
}

