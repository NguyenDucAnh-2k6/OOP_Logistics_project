package com.oop.logistics.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/**
 * Location-specific disaster analysis results
 */
public class LocationAnalysis {

    private String location;                      // Location name
    private String region;                        // Region/Province
    private String country;                       // Country
    private int eventCount;                       // Total number of events
    private Map<String, Integer> disasterTypes;   // Count by disaster type
    private int totalEngagement;                  // Total engagement across events
    private String mostCommonDisaster;            // Most frequent disaster type
    private String severity;                      // Overall severity assessment
    private double latitude;                      // GPS coordinates (optional)
    private double longitude;
    private List<String> eventIds;                // IDs of events at this location

    // Constructors
    public LocationAnalysis() {
        this.disasterTypes = new HashMap<>();
        this.eventIds = new ArrayList<>();
    }

    public LocationAnalysis(String location) {
        this();
        this.location = location;
    }

    public LocationAnalysis(String location, int eventCount) {
        this(location);
        this.eventCount = eventCount;
    }

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public Map<String, Integer> getDisasterTypes() { return disasterTypes; }
    public void setDisasterTypes(Map<String, Integer> disasterTypes) {
        this.disasterTypes = disasterTypes;
        calculateMostCommon();
    }

    public int getTotalEngagement() { return totalEngagement; }
    public void setTotalEngagement(int totalEngagement) {
        this.totalEngagement = totalEngagement;
    }

    public String getMostCommonDisaster() { return mostCommonDisaster; }
    public void setMostCommonDisaster(String mostCommonDisaster) {
        this.mostCommonDisaster = mostCommonDisaster;
    }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<String> getEventIds() { return eventIds; }
    public void setEventIds(List<String> eventIds) { this.eventIds = eventIds; }

    /**
     * Add an event to this location analysis
     */
    public void addEvent(DisasterEvent event) {
        eventCount++;
        
        if (event.getEngagement() > 0) {
            totalEngagement += event.getEngagement();
        }
        
        if (event.getDisasterType() != null) {
            String type = event.getDisasterType();
            disasterTypes.put(type, disasterTypes.getOrDefault(type, 0) + 1);
            calculateMostCommon();
        }
        
        if (event.getSourceId() != null) {
            eventIds.add(event.getSourceId());
        }
        
        calculateSeverity();
    }

    /**
     * Add a disaster type count
     */
    public void addDisasterType(String type, int count) {
        disasterTypes.put(type, disasterTypes.getOrDefault(type, 0) + count);
        calculateMostCommon();
    }

    /**
     * Calculate most common disaster type
     */
    private void calculateMostCommon() {
        if (disasterTypes.isEmpty()) {
            mostCommonDisaster = "Unknown";
            return;
        }

        mostCommonDisaster = disasterTypes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }

    /**
     * Calculate severity based on event count and engagement
     */
    private void calculateSeverity() {
        int severityScore = 0;
        
        // Score based on event count
        if (eventCount >= 50) severityScore += 3;
        else if (eventCount >= 20) severityScore += 2;
        else if (eventCount >= 5) severityScore += 1;
        
        // Score based on engagement
        if (totalEngagement >= 100000) severityScore += 3;
        else if (totalEngagement >= 10000) severityScore += 2;
        else if (totalEngagement >= 1000) severityScore += 1;
        
        // Set severity level
        if (severityScore >= 5) severity = "Critical";
        else if (severityScore >= 3) severity = "High";
        else if (severityScore >= 1) severity = "Medium";
        else severity = "Low";
    }

    /**
     * Get average engagement per event
     */
    public double getAverageEngagement() {
        return eventCount > 0 ? (double) totalEngagement / eventCount : 0.0;
    }

    /**
     * Get count for specific disaster type
     */
    public int getDisasterTypeCount(String type) {
        return disasterTypes.getOrDefault(type, 0);
    }

    /**
     * Get number of different disaster types
     */
    public int getDisasterTypeVariety() {
        return disasterTypes.size();
    }

    /**
     * Check if location has coordinates
     */
    public boolean hasCoordinates() {
        return latitude != 0.0 && longitude != 0.0;
    }

    /**
     * Get full location string
     */
    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        sb.append(location);
        
        if (region != null && !region.isEmpty() && !region.equals(location)) {
            sb.append(", ").append(region);
        }
        
        if (country != null && !country.isEmpty()) {
            sb.append(", ").append(country);
        }
        
        return sb.toString();
    }

    /**
     * Compare by event count
     */
    public static Comparator<LocationAnalysis> byEventCount() {
        return Comparator.comparingInt(LocationAnalysis::getEventCount).reversed();
    }

    /**
     * Compare by engagement
     */
    public static Comparator<LocationAnalysis> byEngagement() {
        return Comparator.comparingInt(LocationAnalysis::getTotalEngagement).reversed();
    }

    /**
     * Compare by severity
     */
    public static Comparator<LocationAnalysis> bySeverity() {
        Map<String, Integer> severityOrder = Map.of(
            "Critical", 4,
            "High", 3,
            "Medium", 2,
            "Low", 1
        );
        
        return (a, b) -> {
            int aOrder = severityOrder.getOrDefault(a.severity, 0);
            int bOrder = severityOrder.getOrDefault(b.severity, 0);
            return Integer.compare(bOrder, aOrder);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationAnalysis that = (LocationAnalysis) o;
        return location != null ? location.equals(that.location) : that.location == null;
    }

    @Override
    public int hashCode() {
        return location != null ? location.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "LocationAnalysis{" +
                "location='" + location + '\'' +
                ", eventCount=" + eventCount +
                ", totalEngagement=" + totalEngagement +
                ", mostCommonDisaster='" + mostCommonDisaster + '\'' +
                ", severity='" + severity + '\'' +
                ", disasterTypes=" + disasterTypes +
                '}';
    }
}