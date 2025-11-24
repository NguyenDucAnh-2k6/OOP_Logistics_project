package com.oop.logistics.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Response model from analysis APIs
 */
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

    // Constructors
    public AnalysisResponse() {}

    public AnalysisResponse(String sentiment, double confidence) {
        this.sentiment = sentiment;
        this.confidence = confidence;
    }

    // Factory methods
    public static AnalysisResponse success(String sentiment, double confidence) {
        AnalysisResponse response = new AnalysisResponse();
        response.setSentiment(sentiment);
        response.setConfidence(confidence);
        return response;
    }

    public static AnalysisResponse error(String errorMessage) {
        AnalysisResponse response = new AnalysisResponse();
        response.setError(errorMessage);
        return response;
    }

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

    /**
     * Check if response has error
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Check if sentiment is positive
     */
    public boolean isPositive() {
        return "positive".equalsIgnoreCase(sentiment);
    }

    /**
     * Check if sentiment is negative
     */
    public boolean isNegative() {
        return "negative".equalsIgnoreCase(sentiment);
    }

    /**
     * Check if sentiment is neutral
     */
    public boolean isNeutral() {
        return "neutral".equalsIgnoreCase(sentiment);
    }

    /**
     * Get score for specific sentiment
     */
    public double getScoreFor(String sentimentType) {
        if (scores == null) {
            return 0.0;
        }
        return scores.getOrDefault(sentimentType.toLowerCase(), 0.0);
    }

    @Override
    public String toString() {
        if (hasError()) {
            return "AnalysisResponse{error='" + error + "'}";
        }
        return "AnalysisResponse{" +
                "sentiment='" + sentiment + '\'' +
                ", confidence=" + confidence +
                ", processingTime=" + processingTime + "ms" +
                '}';
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

        @SerializedName("index")
        private int index;

        // Constructors
        public SentimentResult() {}

        public SentimentResult(String text, String sentiment, double confidence) {
            this.text = text;
            this.sentiment = sentiment;
            this.confidence = confidence;
        }

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        @Override
        public String toString() {
            return "SentimentResult{" +
                    "text='" + (text != null ? text.substring(0, Math.min(30, text.length())) + "..." : null) + '\'' +
                    ", sentiment='" + sentiment + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}