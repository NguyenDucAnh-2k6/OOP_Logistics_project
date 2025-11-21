package com.oop.logistics.preprocessing;

import com.oop.logistics.models.DisasterEvent;
import com.oop.logistics.config.KeywordManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles data preprocessing: cleaning, deduplication, enrichment
 */
public class DataPreprocessor {
    
    private final KeywordManager keywordManager;
    private final LocationExtractor locationExtractor;
    
    public DataPreprocessor(KeywordManager keywordManager) {
        this.keywordManager = keywordManager;
        this.locationExtractor = new LocationExtractor();
    }
    
    /**
     * Preprocess a list of disaster events
     */
    public List<DisasterEvent> preprocess(List<DisasterEvent> events) {
        // Step 1: Clean data
        List<DisasterEvent> cleaned = cleanEvents(events);
        
        // Step 2: Remove duplicates
        List<DisasterEvent> deduplicated = deduplicateEvents(cleaned);
        
        // Step 3: Enrich events with additional information
        List<DisasterEvent> enriched = enrichEvents(deduplicated);
        
        // Step 4: Filter invalid events
        return filterValidEvents(enriched);
    }
    
    /**
     * Clean event data (trim whitespace, normalize text)
     */
    private List<DisasterEvent> cleanEvents(List<DisasterEvent> events) {
        return events.stream()
            .map(this::cleanEvent)
            .collect(Collectors.toList());
    }
    
    private DisasterEvent cleanEvent(DisasterEvent event) {
        if (event.getDescription() != null) {
            event.setDescription(cleanText(event.getDescription()));
        }
        
        if (event.getLocation() != null) {
            event.setLocation(cleanText(event.getLocation()));
        }
        
        return event;
    }
    
    private String cleanText(String text) {
        if (text == null) return null;
        
        // Remove extra whitespace
        text = text.trim().replaceAll("\\s+", " ");
        
        // Remove URLs
        text = text.replaceAll("https?://\\S+", "");
        
        // Remove special characters but keep Vietnamese diacritics
        // text = text.replaceAll("[^\\p{L}\\p{N}\\s.,!?-]", "");
        
        return text;
    }
    
    /**
     * Remove duplicate events based on content similarity
     */
    private List<DisasterEvent> deduplicateEvents(List<DisasterEvent> events) {
        Map<String, DisasterEvent> uniqueEvents = new HashMap<>();
        
        for (DisasterEvent event : events) {
            String key = generateEventKey(event);
            
            // Keep event with higher engagement if duplicate
            if (!uniqueEvents.containsKey(key) || 
                event.getEngagement() > uniqueEvents.get(key).getEngagement()) {
                uniqueEvents.put(key, event);
            }
        }
        
        return new ArrayList<>(uniqueEvents.values());
    }
    
    private String generateEventKey(DisasterEvent event) {
        // Create a key based on disaster type, location, and time window
        StringBuilder key = new StringBuilder();
        
        if (event.getDisasterType() != null) {
            key.append(event.getDisasterType()).append("_");
        }
        
        if (event.getLocation() != null) {
            key.append(event.getLocation()).append("_");
        }
        
        // Group by day (rough deduplication)
        if (event.getTimestamp() != null) {
            String dateOnly = event.getTimestamp().substring(0, 
                Math.min(10, event.getTimestamp().length()));
            key.append(dateOnly);
        }
        
        return key.toString();
    }
    
    /**
     * Enrich events with additional extracted information
     */
    private List<DisasterEvent> enrichEvents(List<DisasterEvent> events) {
        return events.stream()
            .map(this::enrichEvent)
            .collect(Collectors.toList());
    }
    
    private DisasterEvent enrichEvent(DisasterEvent event) {
        // Extract and set disaster type if not already set
        if (event.getDisasterType() == null || event.getDisasterType().equals("Unknown")) {
            Set<String> categories = keywordManager.detectCategories(event.getDescription());
            if (!categories.isEmpty()) {
                event.setDisasterType(categories.iterator().next());
            }
        }
        
        // Extract location if not set
        if (event.getLocation() == null && event.getDescription() != null) {
            String location = locationExtractor.extractLocation(event.getDescription());
            event.setLocation(location);
        }
        
        // Determine severity based on keywords and engagement
        event.setSeverity(determineSeverity(event));
        
        return event;
    }
    
    private String determineSeverity(DisasterEvent event) {
        int severityScore = 0;
        
        // Check for severity keywords
        if (event.getDescription() != null) {
            String desc = event.getDescription().toLowerCase();
            if (desc.contains("nghiêm trọng") || desc.contains("critical") || 
                desc.contains("severe") || desc.contains("catastrophic")) {
                severityScore += 3;
            }
            if (desc.contains("casualties") || desc.contains("tử vong") || 
                desc.contains("deaths")) {
                severityScore += 3;
            }
            if (desc.contains("damage") || desc.contains("thiệt hại")) {
                severityScore += 1;
            }
        }
        
        // Factor in engagement
        if (event.getEngagement() > 10000) severityScore += 3;
        else if (event.getEngagement() > 1000) severityScore += 2;
        else if (event.getEngagement() > 100) severityScore += 1;
        
        if (severityScore >= 5) return "Critical";
        if (severityScore >= 3) return "High";
        if (severityScore >= 1) return "Medium";
        return "Low";
    }
    
    /**
     * Filter out invalid or incomplete events
     */
    private List<DisasterEvent> filterValidEvents(List<DisasterEvent> events) {
        return events.stream()
            .filter(this::isValidEvent)
            .collect(Collectors.toList());
    }
    
    private boolean isValidEvent(DisasterEvent event) {
        // Must have at least description or location
        if (event.getDescription() == null && event.getLocation() == null) {
            return false;
        }
        
        // Must have disaster type
        if (event.getDisasterType() == null || event.getDisasterType().equals("Unknown")) {
            return false;
        }
        
        return true;
    }
}