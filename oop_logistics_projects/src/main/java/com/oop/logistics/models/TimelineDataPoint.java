package com.oop.logistics.models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * Represents a data point on a timeline for temporal analysis
 */
public class TimelineDataPoint implements Comparable<TimelineDataPoint> {

    private String date;           // ISO format: YYYY-MM-DD
    private int count;             // Number of events
    private int totalEngagement;   // Total engagement for the day
    private String dominantType;   // Most common disaster type for the day

    // Constructors
    public TimelineDataPoint() {}

    public TimelineDataPoint(String date, int count) {
        this.date = date;
        this.count = count;
    }

    public TimelineDataPoint(String date, int count, int totalEngagement) {
        this.date = date;
        this.count = count;
        this.totalEngagement = totalEngagement;
    }

    public TimelineDataPoint(String date, int count, int totalEngagement, String dominantType) {
        this.date = date;
        this.count = count;
        this.totalEngagement = totalEngagement;
        this.dominantType = dominantType;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getTotalEngagement() { return totalEngagement; }
    public void setTotalEngagement(int totalEngagement) { this.totalEngagement = totalEngagement; }

    public String getDominantType() { return dominantType; }
    public void setDominantType(String dominantType) { this.dominantType = dominantType; }

    /**
     * Increment count by one
     */
    public void incrementCount() {
        this.count++;
    }

    /**
     * Add engagement to total
     */
    public void addEngagement(int engagement) {
        this.totalEngagement += engagement;
    }

    /**
     * Calculate average engagement per event
     */
    public double getAverageEngagement() {
        return count > 0 ? (double) totalEngagement / count : 0.0;
    }

    /**
     * Parse date to LocalDate
     */
    public LocalDate toLocalDate() {
        if (date == null) return null;
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get formatted date for display
     */
    public String getFormattedDate(String pattern) {
        LocalDate localDate = toLocalDate();
        if (localDate == null) return date;
        return localDate.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Get day of week
     */
    public String getDayOfWeek() {
        LocalDate localDate = toLocalDate();
        if (localDate == null) return "Unknown";
        return localDate.getDayOfWeek().toString();
    }

    /**
     * Check if this date is after another
     */
    public boolean isAfter(TimelineDataPoint other) {
        if (other == null || other.date == null) return true;
        if (this.date == null) return false;
        
        LocalDate thisDate = toLocalDate();
        LocalDate otherDate = other.toLocalDate();
        
        if (thisDate == null || otherDate == null) {
            return this.date.compareTo(other.date) > 0;
        }
        
        return thisDate.isAfter(otherDate);
    }

    /**
     * Check if this date is before another
     */
    public boolean isBefore(TimelineDataPoint other) {
        if (other == null || other.date == null) return false;
        if (this.date == null) return true;
        
        LocalDate thisDate = toLocalDate();
        LocalDate otherDate = other.toLocalDate();
        
        if (thisDate == null || otherDate == null) {
            return this.date.compareTo(other.date) < 0;
        }
        
        return thisDate.isBefore(otherDate);
    }

    @Override
    public int compareTo(TimelineDataPoint other) {
        if (other == null) return 1;
        if (this.date == null && other.date == null) return 0;
        if (this.date == null) return -1;
        if (other.date == null) return 1;
        return this.date.compareTo(other.date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimelineDataPoint that = (TimelineDataPoint) o;
        return date != null ? date.equals(that.date) : that.date == null;
    }

    @Override
    public int hashCode() {
        return date != null ? date.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TimelineDataPoint{" +
                "date='" + date + '\'' +
                ", count=" + count +
                ", totalEngagement=" + totalEngagement +
                ", dominantType='" + dominantType + '\'' +
                '}';
    }

    /**
     * Comparators for sorting
     */
    public static Comparator<TimelineDataPoint> byDate() {
        return Comparator.comparing(TimelineDataPoint::getDate, Comparator.nullsLast(String::compareTo));
    }

    public static Comparator<TimelineDataPoint> byCount() {
        return Comparator.comparingInt(TimelineDataPoint::getCount).reversed();
    }

    public static Comparator<TimelineDataPoint> byEngagement() {
        return Comparator.comparingInt(TimelineDataPoint::getTotalEngagement).reversed();
    }
}