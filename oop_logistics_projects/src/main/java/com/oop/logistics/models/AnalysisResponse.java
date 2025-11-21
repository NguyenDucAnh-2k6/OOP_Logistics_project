
package com.oop.logistics.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
public class AnalysisResponse {
    
    @SerializedName("sentiment")
    private String sentiment; // "positive", "negative", "neutral"
    
    @SerializedName("confidence")
    private double confidence; // 0.0 to 1.0
    
    @SerializedName("scores")
    private Map<String, Double> scores; // detailed sentiment scores
    
    @SerializedName("results")
    private List<SentimentResult> results; // for batch processing
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("processing_time")
    private long processingTime; // milliseconds
    
    public AnalysisResponse() {}
    
    // Getters and Setters
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public Map<String, Double> getScores() { return scores; }
    public void setScores(Map<String, Double> scores) { this.scores = scores; }
    
    public List<SentimentResult> getResults() { return results; }
    public void setResults(List<SentimentResult> results) { this.results = results; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    /**
     * Individual result for batch processing
     */
    public static class SentimentResult {
        @SerializedName("text")
        private String text;
        
        @SerializedName("sentiment")
        private String sentiment;
        
        @SerializedName("confidence")
        private double confidence;
        
        public SentimentResult() {}
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}