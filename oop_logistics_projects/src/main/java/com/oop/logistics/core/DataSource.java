package com.oop.logistics.core;

import com.oop.logistics.models.DisasterEvent;
import java.util.List;

/**
 * Core interface for all data sources (Facebook, Twitter, News APIs, etc.)
 * This design allows easy addition of new data sources
 */
public interface DataSource {
    
    /**
     * Fetch raw data from the source
     * @return List of disaster events
     */
    List<DisasterEvent> fetchDisasterEvents();
    
    /**
     * Get the name of this data source
     * @return Source name (e.g., "Facebook", "Twitter")
     */
    String getSourceName();
    
    /**
     * Check if the data source is available/configured
     * @return true if source can be used
     */
    boolean isAvailable();
    
    /**
     * Configure the data source with necessary credentials/settings
     * @param config Configuration object
     */
    void configure(SourceConfiguration config);
    
    /**
     * Get viral/high-engagement disaster posts
     * @param minEngagement Minimum engagement threshold
     * @return List of high-engagement events
     */
    List<DisasterEvent> fetchViralEvents(int minEngagement);
}