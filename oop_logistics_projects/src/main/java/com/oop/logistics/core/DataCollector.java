package com.oop.logistics.core;

import com.oop.logistics.models.DisasterEvent;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orchestrates data collection from multiple sources
 * Supports parallel collection and aggregation
 */
public class DataCollector {
    
    private final List<DataSource> dataSources;
    private final ExecutorService executorService;
    
    public DataCollector() {
        this.dataSources = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public DataCollector(List<DataSource> dataSources) {
        this.dataSources = new ArrayList<>(dataSources);
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Register a new data source
     */
    public void registerDataSource(DataSource dataSource) {
        if (dataSource.isAvailable()) {
            dataSources.add(dataSource);
        }
    }
    
    /**
     * Remove a data source
     */
    public void removeDataSource(String sourceName) {
        dataSources.removeIf(ds -> ds.getSourceName().equals(sourceName));
    }
    
    /**
     * Get all registered data sources
     */
    public List<DataSource> getDataSources() {
        return new ArrayList<>(dataSources);
    }
    
    /**
     * Collect data from all sources sequentially
     */
    public List<DisasterEvent> collectAll() {
        List<DisasterEvent> allEvents = new ArrayList<>();
        
        for (DataSource source : dataSources) {
            try {
                if (source.isAvailable()) {
                    List<DisasterEvent> events = source.fetchDisasterEvents();
                    allEvents.addAll(events);
                    System.out.println("Collected " + events.size() + 
                                     " events from " + source.getSourceName());
                }
            } catch (Exception e) {
                System.err.println("Error collecting from " + 
                                 source.getSourceName() + ": " + e.getMessage());
            }
        }
        
        return allEvents;
    }
    
    /**
     * Collect data from all sources in parallel
     */
    public List<DisasterEvent> collectAllParallel() {
        List<Future<List<DisasterEvent>>> futures = new ArrayList<>();
        
        for (DataSource source : dataSources) {
            Future<List<DisasterEvent>> future = executorService.submit(() -> {
                try {
                    if (source.isAvailable()) {
                        List<DisasterEvent> events = source.fetchDisasterEvents();
                        System.out.println("Collected " + events.size() + 
                                         " events from " + source.getSourceName());
                        return events;
                    }
                } catch (Exception e) {
                    System.err.println("Error collecting from " + 
                                     source.getSourceName() + ": " + e.getMessage());
                }
                return new ArrayList<>();
            });
            futures.add(future);
        }
        
        // Aggregate results
        List<DisasterEvent> allEvents = new ArrayList<>();
        for (Future<List<DisasterEvent>> future : futures) {
            try {
                allEvents.addAll(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                System.err.println("Error retrieving data: " + e.getMessage());
            }
        }
        
        return allEvents;
    }
    
    /**
     * Collect data from a specific source
     */
    public List<DisasterEvent> collectFrom(String sourceName) {
        for (DataSource source : dataSources) {
            if (source.getSourceName().equals(sourceName) && source.isAvailable()) {
                try {
                    return source.fetchDisasterEvents();
                } catch (Exception e) {
                    System.err.println("Error collecting from " + 
                                     sourceName + ": " + e.getMessage());
                    return new ArrayList<>();
                }
            }
        }
        System.err.println("Source not found or unavailable: " + sourceName);
        return new ArrayList<>();
    }
    
    /**
     * Collect viral events from all sources
     */
    public List<DisasterEvent> collectViralEvents(int minEngagement) {
        List<DisasterEvent> viralEvents = new ArrayList<>();
        
        for (DataSource source : dataSources) {
            try {
                if (source.isAvailable()) {
                    List<DisasterEvent> events = source.fetchViralEvents(minEngagement);
                    viralEvents.addAll(events);
                }
            } catch (Exception e) {
                System.err.println("Error collecting viral events from " + 
                                 source.getSourceName() + ": " + e.getMessage());
            }
        }
        
        return viralEvents;
    }
    
    /**
     * Get collection statistics
     */
    public CollectionStats getStats() {
        CollectionStats stats = new CollectionStats();
        stats.setTotalSources(dataSources.size());
        stats.setActiveSources((int) dataSources.stream()
            .filter(DataSource::isAvailable)
            .count());
        
        Map<String, Boolean> sourceStatus = dataSources.stream()
            .collect(Collectors.toMap(
                DataSource::getSourceName,
                DataSource::isAvailable
            ));
        stats.setSourceStatus(sourceStatus);
        
        return stats;
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    /**
     * Collection statistics
     */
    public static class CollectionStats {
        private int totalSources;
        private int activeSources;
        private Map<String, Boolean> sourceStatus;
        
        public CollectionStats() {
            this.sourceStatus = new HashMap<>();
        }
        
        public int getTotalSources() { return totalSources; }
        public void setTotalSources(int totalSources) { this.totalSources = totalSources; }
        
        public int getActiveSources() { return activeSources; }
        public void setActiveSources(int activeSources) { this.activeSources = activeSources; }
        
        public Map<String, Boolean> getSourceStatus() { return sourceStatus; }
        public void setSourceStatus(Map<String, Boolean> sourceStatus) { 
            this.sourceStatus = sourceStatus; 
        }
        
        @Override
        public String toString() {
            return "CollectionStats{" +
                   "totalSources=" + totalSources +
                   ", activeSources=" + activeSources +
                   ", sourceStatus=" + sourceStatus +
                   '}';
        }
    }
}