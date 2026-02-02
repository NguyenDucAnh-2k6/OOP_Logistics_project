package com.oop.logistics.analysis;

import com.oop.logistics.models.AnalysisRequest;
import com.oop.logistics.models.AnalysisResponse;

/**
 * Interface for analysis APIs (can be Python-based or Java-based)
 * This abstraction allows easy swapping of analysis implementations
 */
public interface AnalysisAPI {
    
    /**
     * Perform sentiment analysis on text
     * @param request Analysis request containing text and parameters
     * @return Analysis response with sentiment scores
     */
    AnalysisResponse analyzeSentiment(AnalysisRequest request);
    
    /**
     * Get the name of this analysis provider
     * @return Provider name
     */
    String getProviderName();
    
    /**
     * Check if the API is available
     * @return true if API can be used
     */
    boolean isAvailable();
    
    /**
     * Get API configuration information
     * @return Configuration details
     */
    String getConfiguration();
}