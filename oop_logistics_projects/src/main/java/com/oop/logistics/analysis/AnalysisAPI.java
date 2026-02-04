package com.oop.logistics.analysis;

import java.util.List;
import java.util.Map;

public interface AnalysisAPI {
    String getProviderName();
    String getConfiguration();
    boolean isAvailable();

    // Updated methods to include 'modelType'
    List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates, String modelType) throws Exception;
    
    List<String> getDamageClassification(List<String> texts, String modelType) throws Exception;
    
    Map<String, Map<String, Double>> getReliefSentiment(List<String> texts, String modelType) throws Exception;
    
    List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates, String modelType) throws Exception;
}