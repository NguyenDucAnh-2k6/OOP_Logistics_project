package com.oop.logistics.models;

import java.util.*;

/**
 * Main disaster analysis report
 */
public class DisasterAnalysisReport {
    
    private int totalEvents;
    private Map<String, Integer> eventsByType;
    private Map<String, Integer> eventsByLocation;
    private Map<String, Integer> eventsBySeverity;
    private List<TimelineDataPoint> timelineData;
    private List<DisasterEvent> topEngagementEvents;
    private double averageEngagement;
    private Map<String, Integer> damageCategories;
    private Map<String, Integer> reliefCategories;
    private SentimentAnalysisResult sentimentAnalysis;
    
    public DisasterAnalysisReport() {
        this.eventsByType = new HashMap<>();
        this.eventsByLocation = new HashMap<>();
        this.eventsBySeverity = new HashMap<>();
        this.timelineData = new ArrayList<>();
        this.topEngagementEvents = new ArrayList<>();
        this.damageCategories = new HashMap<>();
        this.reliefCategories = new HashMap<>();
    }
    
    // Getters and Setters
    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
    
    public Map<String, Integer> getEventsByType() { return eventsByType; }
    public void setEventsByType(Map<String, Integer> eventsByType) { 
        this.eventsByType = eventsByType; 
    }
    
    public Map<String, Integer> getEventsByLocation() { return eventsByLocation; }
    public void setEventsByLocation(Map<String, Integer> eventsByLocation) { 
        this.eventsByLocation = eventsByLocation; 
    }
    
    public Map<String, Integer> getEventsBySeverity() { return eventsBySeverity; }
    public void setEventsBySeverity(Map<String, Integer> eventsBySeverity) { 
        this.eventsBySeverity = eventsBySeverity; 
    }
    
    public List<TimelineDataPoint> getTimelineData() { return timelineData; }
    public void setTimelineData(List<TimelineDataPoint> timelineData) { 
        this.timelineData = timelineData; 
    }
    
    public List<DisasterEvent> getTopEngagementEvents() { return topEngagementEvents; }
    public void setTopEngagementEvents(List<DisasterEvent> topEngagementEvents) { 
        this.topEngagementEvents = topEngagementEvents; 
    }
    
    public double getAverageEngagement() { return averageEngagement; }
    public void setAverageEngagement(double averageEngagement) { 
        this.averageEngagement = averageEngagement; 
    }
    
    public Map<String, Integer> getDamageCategories() { return damageCategories; }
    public void setDamageCategories(Map<String, Integer> damageCategories) { 
        this.damageCategories = damageCategories; 
    }
    
    public Map<String, Integer> getReliefCategories() { return reliefCategories; }
    public void setReliefCategories(Map<String, Integer> reliefCategories) { 
        this.reliefCategories = reliefCategories; 
    }
    
    public SentimentAnalysisResult getSentimentAnalysis() { return sentimentAnalysis; }
    public void setSentimentAnalysis(SentimentAnalysisResult sentimentAnalysis) { 
        this.sentimentAnalysis = sentimentAnalysis; 
    }
}

/**
 * Timeline data point for temporal analysis
 */
class TimelineDataPoint {
    private String date;
    private int count;
    
    public TimelineDataPoint(String date, int count) {
        this.date = date;
        this.count = count;
    }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}

/**
 * Sentiment analysis result
 */
class SentimentAnalysisResult {
    private Map<String, Integer> sentimentDistribution;
    private double averageConfidence;
    
    public SentimentAnalysisResult() {
        this.sentimentDistribution = new HashMap<>();
    }
    
    public Map<String, Integer> getSentimentDistribution() { 
        return sentimentDistribution; 
    }
    public void setSentimentDistribution(Map<String, Integer> sentimentDistribution) { 
        this.sentimentDistribution = sentimentDistribution; 
    }
    
    public double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(double averageConfidence) { 
        this.averageConfidence = averageConfidence; 
    }
}

/**
 * Location-specific analysis
 */
class LocationAnalysis {
    private String location;
    private int eventCount;
    private Map<String, Integer> disasterTypes;
    private int totalEngagement;
    
    public LocationAnalysis() {
        this.disasterTypes = new HashMap<>();
    }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    
    public Map<String, Integer> getDisasterTypes() { return disasterTypes; }
    public void setDisasterTypes(Map<String, Integer> disasterTypes) { 
        this.disasterTypes = disasterTypes; 
    }
    
    public int getTotalEngagement() { return totalEngagement; }
    public void setTotalEngagement(int totalEngagement) { 
        this.totalEngagement = totalEngagement; 
    }
}