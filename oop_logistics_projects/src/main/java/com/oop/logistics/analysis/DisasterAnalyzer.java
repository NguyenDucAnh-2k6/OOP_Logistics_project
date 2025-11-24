package com.oop.logistics.analysis;
import com.oop.logistics.models.*;
import com.oop.logistics.config.CategoryManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main analyzer for disaster events
 * Provides various analysis capabilities
 */
public class DisasterAnalyzer {
    
    private final CategoryManager categoryManager;
    private final AnalysisAPI sentimentAPI;
    
    public DisasterAnalyzer(CategoryManager categoryManager, AnalysisAPI sentimentAPI) {
        this.categoryManager = categoryManager;
        this.sentimentAPI = sentimentAPI;
    }
    
    /**
     * Analyze disaster events and generate insights
     */
    public DisasterAnalysisReport analyzeEvents(List<DisasterEvent> events) {
        DisasterAnalysisReport report = new DisasterAnalysisReport();
        
        // Basic statistics
        report.setTotalEvents(events.size());
        report.setEventsByType(countByDisasterType(events));
        report.setEventsByLocation(countByLocation(events));
        report.setEventsBySeverity(countBySeverity(events));
        
        // Temporal analysis
        report.setTimelineData(analyzeTimeline(events));
        
        // Engagement analysis
        report.setTopEngagementEvents(getTopEngagement(events, 10));
        report.setAverageEngagement(calculateAverageEngagement(events));
        
        // Category analysis
        report.setDamageCategories(analyzeDamageCategories(events));
        report.setReliefCategories(analyzeReliefCategories(events));
        
        // Sentiment analysis
        if (sentimentAPI != null && sentimentAPI.isAvailable()) {
            report.setSentimentAnalysis(analyzeSentiment(events));
        }
        
        return report;
    }
    
    /**
     * Count events by disaster type
     */
    private Map<String, Integer> countByDisasterType(List<DisasterEvent> events) {
        Map<String, Integer> counts = new HashMap<>();
        for (DisasterEvent event : events) {
            String type = event.getDisasterType();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        return counts;
    }
    
    /**
     * Count events by location
     */
    private Map<String, Integer> countByLocation(List<DisasterEvent> events) {
        Map<String, Integer> counts = new HashMap<>();
        for (DisasterEvent event : events) {
            if (event.getLocation() != null) {
                String location = event.getLocation();
                counts.put(location, counts.getOrDefault(location, 0) + 1);
            }
        }
        return counts;
    }
    
    /**
     * Count events by severity
     */
    private Map<String, Integer> countBySeverity(List<DisasterEvent> events) {
        Map<String, Integer> counts = new HashMap<>();
        for (DisasterEvent event : events) {
            if (event.getSeverity() != null) {
                String severity = event.getSeverity();
                counts.put(severity, counts.getOrDefault(severity, 0) + 1);
            }
        }
        return counts;
    }
    
    /**
     * Analyze events over time
     */
    private List<TimelineDataPoint> analyzeTimeline(List<DisasterEvent> events) {
        Map<String, Integer> dailyCounts = new HashMap<>();
        
        for (DisasterEvent event : events) {
            if (event.getTimestamp() != null) {
                String date = event.getTimestamp().substring(0, 10);
                dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + 1);
            }
        }
        
        return dailyCounts.entrySet().stream()
            .map(e -> new TimelineDataPoint(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(TimelineDataPoint::getDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Get top engagement events
     */
    private List<DisasterEvent> getTopEngagement(List<DisasterEvent> events, int limit) {
        return events.stream()
            .sorted(Comparator.comparingInt(DisasterEvent::getEngagement).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate average engagement
     */
    private double calculateAverageEngagement(List<DisasterEvent> events) {
        if (events.isEmpty()) return 0.0;
        
        int totalEngagement = events.stream()
            .mapToInt(DisasterEvent::getEngagement)
            .sum();
        
        return (double) totalEngagement / events.size();
    }
    
    /**
     * Analyze damage categories mentioned in events
     */
    private Map<String, Integer> analyzeDamageCategories(List<DisasterEvent> events) {
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        List<CategoryManager.Category> damageCategories = 
            categoryManager.getCategoriesByType("DAMAGE");
        
        for (DisasterEvent event : events) {
            if (event.getDescription() != null) {
                Map<String, Integer> matches = 
                    categoryManager.detectCategoriesInText(event.getDescription());
                
                for (Map.Entry<String, Integer> entry : matches.entrySet()) {
                    if (isDamageCategory(entry.getKey(), damageCategories)) {
                        categoryCounts.put(
                            entry.getKey(), 
                            categoryCounts.getOrDefault(entry.getKey(), 0) + entry.getValue()
                        );
                    }
                }
            }
        }
        
        return categoryCounts;
    }
    
    /**
     * Analyze relief categories mentioned in events
     */
    private Map<String, Integer> analyzeReliefCategories(List<DisasterEvent> events) {
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        List<CategoryManager.Category> reliefCategories = 
            categoryManager.getCategoriesByType("RELIEF");
        
        for (DisasterEvent event : events) {
            if (event.getDescription() != null) {
                Map<String, Integer> matches = 
                    categoryManager.detectCategoriesInText(event.getDescription());
                
                for (Map.Entry<String, Integer> entry : matches.entrySet()) {
                    if (isReliefCategory(entry.getKey(), reliefCategories)) {
                        categoryCounts.put(
                            entry.getKey(), 
                            categoryCounts.getOrDefault(entry.getKey(), 0) + entry.getValue()
                        );
                    }
                }
            }
        }
        
        return categoryCounts;
    }
    
    private boolean isDamageCategory(String categoryName, 
                                    List<CategoryManager.Category> categories) {
        return categories.stream()
            .anyMatch(c -> c.getName().equals(categoryName));
    }
    
    private boolean isReliefCategory(String categoryName, 
                                    List<CategoryManager.Category> categories) {
        return categories.stream()
            .anyMatch(c -> c.getName().equals(categoryName));
    }
    
    /**
     * Perform sentiment analysis on events
     */
    private SentimentAnalysisResult analyzeSentiment(List<DisasterEvent> events) {
        SentimentAnalysisResult result = new SentimentAnalysisResult();
        
        Map<String, Integer> sentimentCounts = new HashMap<>();
        sentimentCounts.put("positive", 0);
        sentimentCounts.put("negative", 0);
        sentimentCounts.put("neutral", 0);
        
        for (DisasterEvent event : events) {
            if (event.getDescription() != null) {
                AnalysisRequest request = new AnalysisRequest(event.getDescription());
                AnalysisResponse response = sentimentAPI.analyzeSentiment(request);
                
                if (!response.hasError()) {
                    String sentiment = response.getSentiment();
                    sentimentCounts.put(
                        sentiment, 
                        sentimentCounts.getOrDefault(sentiment, 0) + 1
                    );
                }
            }
        }
        
        result.setSentimentDistribution(sentimentCounts);
        return result;
    }
    
    /**
     * Find events requiring urgent response
     */
    public List<DisasterEvent> findUrgentEvents(List<DisasterEvent> events) {
        return events.stream()
            .filter(e -> "Critical".equals(e.getSeverity()) || "High".equals(e.getSeverity()))
            .sorted(Comparator.comparingInt(DisasterEvent::getEngagement).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get location-based analysis
     */
    public Map<String, LocationAnalysis> analyzeByLocation(List<DisasterEvent> events) {
        Map<String, List<DisasterEvent>> eventsByLocation = events.stream()
            .filter(e -> e.getLocation() != null)
            .collect(Collectors.groupingBy(DisasterEvent::getLocation));
        
        Map<String, LocationAnalysis> locationAnalysis = new HashMap<>();
        
        for (Map.Entry<String, List<DisasterEvent>> entry : eventsByLocation.entrySet()) {
            LocationAnalysis analysis = new LocationAnalysis();
            analysis.setLocation(entry.getKey());
            analysis.setEventCount(entry.getValue().size());
            analysis.setDisasterTypes(countByDisasterType(entry.getValue()));
            analysis.setTotalEngagement(
                entry.getValue().stream()
                    .mapToInt(DisasterEvent::getEngagement)
                    .sum()
            );
            
            locationAnalysis.put(entry.getKey(), analysis);
        }
        
        return locationAnalysis;
    }
}