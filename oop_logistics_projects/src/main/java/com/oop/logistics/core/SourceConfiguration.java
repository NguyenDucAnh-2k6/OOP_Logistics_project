package com.oop.logistics.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration container for data sources
 * Supports flexible key-value configuration
 */
public class SourceConfiguration {
    
    private final Map<String, Object> properties;
    
    public SourceConfiguration() {
        this.properties = new HashMap<>();
    }
    
    public SourceConfiguration(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public String getStringProperty(String key) {
        return (String) properties.get(key);
    }
    
    public String getStringProperty(String key, String defaultValue) {
        return properties.containsKey(key) ? (String) properties.get(key) : defaultValue;
    }
    
    public Integer getIntProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return null;
    }
    
    public Integer getIntProperty(String key, int defaultValue) {
        Integer value = getIntProperty(key);
        return value != null ? value : defaultValue;
    }
    
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }
    
    public Map<String, Object> getAllProperties() {
        return new HashMap<>(properties);
    }
}