package com.oop.logistics.models;

import java.time.LocalDateTime;

public class DisasterEvent {
    
    private String id;
    private String sourceId;  // ID from Facebook/Twitter etc.
    private String source;    // "Facebook", "Twitter", etc.
    private String disasterType;  // "Flood", "Storm", "Earthquake", etc.
    private String description;
    private String location;
    private String timestamp;
    private int engagement;  // Total likes + comments + shares
    private String severity;  // "Low", "Medium", "High", "Critical"
    
    // Constructors
    public DisasterEvent() {}

    public DisasterEvent(String disasterType, String description, String location) {
        this.disasterType = disasterType;
        this.description = description;
        this.location = location;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDisasterType() { return disasterType; }
    public void setDisasterType(String disasterType) { this.disasterType = disasterType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getEngagement() { return engagement; }
    public void setEngagement(int engagement) { this.engagement = engagement; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    @Override
    public String toString() {
        return "DisasterEvent{" +
                "id='" + id + '\'' +
                ", disasterType='" + disasterType + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", source='" + source + '\'' +
                ", engagement=" + engagement +
                ", severity='" + severity + '\'' +
                '}';
    }
}