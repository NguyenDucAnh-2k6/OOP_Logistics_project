package com.oop.logistics.core;

import com.oop.logistics.analysis.AnalysisAPI;
import com.oop.logistics.analysis.DisasterAnalyzer;
import com.oop.logistics.config.CategoryManager;
import com.oop.logistics.config.KeywordManager;
import com.oop.logistics.models.DisasterAnalysisReport;
import com.oop.logistics.models.DisasterEvent;
import com.oop.logistics.preprocessing.DataPreprocessor;
import java.util.List;

/**
 * Complete disaster logistics data pipeline
 * Integrates data collection, preprocessing, and analysis
 */
public class DisasterLogisticsPipeline {
    
    private final DataCollector dataCollector;
    private final DataPreprocessor dataPreprocessor;
    private final DisasterAnalyzer disasterAnalyzer;
    private final KeywordManager keywordManager;
    private final CategoryManager categoryManager;
    
    /**
     * Constructor with all dependencies
     */
    public DisasterLogisticsPipeline(
            KeywordManager keywordManager,
            CategoryManager categoryManager,
            AnalysisAPI analysisAPI) {
        
        this.keywordManager = keywordManager;
        this.categoryManager = categoryManager;
        this.dataCollector = new DataCollector();
        this.dataPreprocessor = new DataPreprocessor(keywordManager);
        this.disasterAnalyzer = new DisasterAnalyzer(categoryManager, analysisAPI);
    }
    
    /**
     * Register a data source to the pipeline
     */
    public void registerDataSource(DataSource dataSource) {
        dataCollector.registerDataSource(dataSource);
    }
    
    /**
     * Execute the complete pipeline
     */
    public PipelineResult execute() {
        PipelineResult result = new PipelineResult();
        
        try {
            // Step 1: Data Collection
            System.out.println("Step 1: Collecting data from sources...");
            List<DisasterEvent> rawEvents = dataCollector.collectAllParallel();
            result.setRawEventCount(rawEvents.size());
            System.out.println("Collected " + rawEvents.size() + " raw events");
            
            // Step 2: Data Preprocessing
            System.out.println("Step 2: Preprocessing data...");
            List<DisasterEvent> processedEvents = dataPreprocessor.preprocess(rawEvents);
            result.setProcessedEventCount(processedEvents.size());
            result.setProcessedEvents(processedEvents);
            System.out.println("Processed " + processedEvents.size() + " valid events");
            
            // Step 3: Data Analysis
            System.out.println("Step 3: Analyzing data...");
            DisasterAnalysisReport report = disasterAnalyzer.analyzeEvents(processedEvents);
            result.setAnalysisReport(report);
            System.out.println("Analysis complete");
            
            result.setSuccess(true);
            
        } catch (Exception e) {
            System.err.println("Pipeline error: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Execute pipeline and find urgent events
     */
    public List<DisasterEvent> findUrgentEvents() {
        List<DisasterEvent> rawEvents = dataCollector.collectAllParallel();
        List<DisasterEvent> processedEvents = dataPreprocessor.preprocess(rawEvents);
        return disasterAnalyzer.findUrgentEvents(processedEvents);
    }
    
    /**
     * Get collection statistics
     */
    public DataCollector.CollectionStats getCollectionStats() {
        return dataCollector.getStats();
    }
    
    /**
     * Update keyword configuration
     */
    public void updateKeywords(String category, List<String> keywords) {
        keywordManager.setKeywords(category, keywords);
    }
    
    /**
     * Update category configuration
     */
    public void updateCategory(CategoryManager.Category category) {
        categoryManager.addCategory(category);
    }
    
    /**
     * Get current configuration
     */
    public PipelineConfiguration getConfiguration() {
        PipelineConfiguration config = new PipelineConfiguration();
        config.setKeywordCategories(keywordManager.getCategories());
        config.setCategoryNames(categoryManager.getAllCategoryNames());
        config.setDataSources(dataCollector.getDataSources().stream()
            .map(DataSource::getSourceName)
            .toList());
        return config;
    }
    
    /**
     * Shutdown pipeline
     */
    public void shutdown() {
        dataCollector.shutdown();
    }
    
    /**
     * Pipeline execution result
     */
    public static class PipelineResult {
        private boolean success;
        private String error;
        private int rawEventCount;
        private int processedEventCount;
        private List<DisasterEvent> processedEvents;
        private DisasterAnalysisReport analysisReport;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public int getRawEventCount() { return rawEventCount; }
        public void setRawEventCount(int rawEventCount) { 
            this.rawEventCount = rawEventCount; 
        }
        
        public int getProcessedEventCount() { return processedEventCount; }
        public void setProcessedEventCount(int processedEventCount) { 
            this.processedEventCount = processedEventCount; 
        }
        
        public List<DisasterEvent> getProcessedEvents() { return processedEvents; }
        public void setProcessedEvents(List<DisasterEvent> processedEvents) { 
            this.processedEvents = processedEvents; 
        }
        
        public DisasterAnalysisReport getAnalysisReport() { return analysisReport; }
        public void setAnalysisReport(DisasterAnalysisReport analysisReport) { 
            this.analysisReport = analysisReport; 
        }
    }
    
    /**
     * Pipeline configuration
     */
    public static class PipelineConfiguration {
        private java.util.Set<String> keywordCategories;
        private java.util.Set<String> categoryNames;
        private List<String> dataSources;
        
        public java.util.Set<String> getKeywordCategories() { return keywordCategories; }
        public void setKeywordCategories(java.util.Set<String> keywordCategories) { 
            this.keywordCategories = keywordCategories; 
        }
        
        public java.util.Set<String> getCategoryNames() { return categoryNames; }
        public void setCategoryNames(java.util.Set<String> categoryNames) { 
            this.categoryNames = categoryNames; 
        }
        
        public List<String> getDataSources() { return dataSources; }
        public void setDataSources(List<String> dataSources) { 
            this.dataSources = dataSources; 
        }
    }
}