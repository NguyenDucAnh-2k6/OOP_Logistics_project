package com.oop.logistics.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated sentiment analysis result for a collection of events
 */
public class SentimentAnalysisResult {

    private Map<String, Integer> sentimentDistribution; // positive/negative/neutral counts
    private Map<String, Double> sentimentPercentages;   // percentage breakdown
    private double averageConfidence;
    private int totalAnalyzed;
    private int failedAnalysis;
    private String dominantSentiment;
    private long totalProcessingTime; // milliseconds

    // Constructors
    public SentimentAnalysisResult() {
        this.sentimentDistribution = new HashMap<>();
        this.sentimentPercentages = new HashMap<>();
        initializeDefaults();
    }

    private void initializeDefaults() {
        sentimentDistribution.put("positive", 0);
        sentimentDistribution.put("negative", 0);
        sentimentDistribution.put("neutral", 0);
        
        sentimentPercentages.put("positive", 0.0);
        sentimentPercentages.put("negative", 0.0);
        sentimentPercentages.put("neutral", 0.0);
    }

    // Getters and Setters
    public Map<String, Integer> getSentimentDistribution() { return sentimentDistribution; }
    public void setSentimentDistribution(Map<String, Integer> sentimentDistribution) {
        this.sentimentDistribution = sentimentDistribution;
        calculatePercentages();
        calculateDominantSentiment();
    }

    public Map<String, Double> getSentimentPercentages() { return sentimentPercentages; }
    public void setSentimentPercentages(Map<String, Double> sentimentPercentages) {
        this.sentimentPercentages = sentimentPercentages;
    }

    public double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(double averageConfidence) {
        this.averageConfidence = averageConfidence;
    }

    public int getTotalAnalyzed() { return totalAnalyzed; }
    public void setTotalAnalyzed(int totalAnalyzed) { this.totalAnalyzed = totalAnalyzed; }

    public int getFailedAnalysis() { return failedAnalysis; }
    public void setFailedAnalysis(int failedAnalysis) { this.failedAnalysis = failedAnalysis; }

    public String getDominantSentiment() { return dominantSentiment; }
    public void setDominantSentiment(String dominantSentiment) {
        this.dominantSentiment = dominantSentiment;
    }

    public long getTotalProcessingTime() { return totalProcessingTime; }
    public void setTotalProcessingTime(long totalProcessingTime) {
        this.totalProcessingTime = totalProcessingTime;
    }

    /**
     * Add a sentiment result to the aggregation
     */
    public void addSentiment(String sentiment, double confidence) {
        String normalizedSentiment = sentiment.toLowerCase();
        
        sentimentDistribution.put(
            normalizedSentiment,
            sentimentDistribution.getOrDefault(normalizedSentiment, 0) + 1
        );
        
        totalAnalyzed++;
        
        // Update running average confidence
        averageConfidence = ((averageConfidence * (totalAnalyzed - 1)) + confidence) / totalAnalyzed;
        
        calculatePercentages();
        calculateDominantSentiment();
    }

    /**
     * Record a failed analysis
     */
    public void addFailure() {
        failedAnalysis++;
    }

    /**
     * Add processing time
     */
    public void addProcessingTime(long time) {
        totalProcessingTime += time;
    }

    /**
     * Calculate percentage distribution
     */
    private void calculatePercentages() {
        if (totalAnalyzed == 0) {
            return;
        }

        for (String sentiment : sentimentDistribution.keySet()) {
            double percentage = (double) sentimentDistribution.get(sentiment) / totalAnalyzed * 100;
            sentimentPercentages.put(sentiment, Math.round(percentage * 100.0) / 100.0);
        }
    }

    /**
     * Determine dominant sentiment
     */
    private void calculateDominantSentiment() {
        String dominant = "neutral";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : sentimentDistribution.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominant = entry.getKey();
            }
        }

        this.dominantSentiment = dominant;
    }

    /**
     * Get count for specific sentiment
     */
    public int getCount(String sentiment) {
        return sentimentDistribution.getOrDefault(sentiment.toLowerCase(), 0);
    }

    /**
     * Get percentage for specific sentiment
     */
    public double getPercentage(String sentiment) {
        return sentimentPercentages.getOrDefault(sentiment.toLowerCase(), 0.0);
    }

    /**
     * Get positive count
     */
    public int getPositiveCount() {
        return getCount("positive");
    }

    /**
     * Get negative count
     */
    public int getNegativeCount() {
        return getCount("negative");
    }

    /**
     * Get neutral count
     */
    public int getNeutralCount() {
        return getCount("neutral");
    }

    /**
     * Get success rate
     */
    public double getSuccessRate() {
        int total = totalAnalyzed + failedAnalysis;
        return total > 0 ? (double) totalAnalyzed / total * 100 : 0.0;
    }

    /**
     * Get average processing time per item
     */
    public double getAverageProcessingTime() {
        return totalAnalyzed > 0 ? (double) totalProcessingTime / totalAnalyzed : 0.0;
    }

    /**
     * Check if mostly positive
     */
    public boolean isMostlyPositive() {
        return "positive".equals(dominantSentiment);
    }

    /**
     * Check if mostly negative
     */
    public boolean isMostlyNegative() {
        return "negative".equals(dominantSentiment);
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format(
            "Sentiment Analysis: %d analyzed (%d failed) - Positive: %.1f%%, Negative: %.1f%%, Neutral: %.1f%% - Dominant: %s (Avg Confidence: %.2f)",
            totalAnalyzed,
            failedAnalysis,
            getPercentage("positive"),
            getPercentage("negative"),
            getPercentage("neutral"),
            dominantSentiment,
            averageConfidence
        );
    }

    @Override
    public String toString() {
        return "SentimentAnalysisResult{" +
                "distribution=" + sentimentDistribution +
                ", percentages=" + sentimentPercentages +
                ", dominant='" + dominantSentiment + '\'' +
                ", totalAnalyzed=" + totalAnalyzed +
                ", avgConfidence=" + String.format("%.2f", averageConfidence) +
                '}';
    }
}