package com.oop.logistics.analysis;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AnalysisAPI {
    String getProviderName();
    String getConfiguration();
    boolean isAvailable();

    // Added 'Consumer<Double> onProgress' to all methods
    
    List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress) throws Exception;
    
    List<String> getDamageClassification(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception;
    
    Map<String, Map<String, Double>> getReliefSentiment(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception;
    
    List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress) throws Exception;
    Map<String, Integer> getIntentClassification(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception;
}