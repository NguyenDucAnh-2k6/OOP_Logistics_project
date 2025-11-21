package com.oop.logistics.config;

import java.util.*;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Manages disaster detection keywords with easy updates
 * Keywords can be loaded from JSON files or configured programmatically
 */
public class KeywordManager {
    
    private Map<String, List<String>> keywordsByCategory;
    private final Gson gson;
    
    public KeywordManager() {
        this.gson = new Gson();
        this.keywordsByCategory = new HashMap<>();
        initializeDefaultKeywords();
    }
    
    private void initializeDefaultKeywords() {
        // Flood keywords
        addKeywords("Flood", Arrays.asList(
            "lũ lụt", "flood", "flooding", "ngập lụt", "nước lũ"
        ));
        
        // Storm keywords
        addKeywords("Storm", Arrays.asList(
            "bão", "storm", "hurricane", "typhoon", "cyclone"
        ));
        
        // Earthquake keywords
        addKeywords("Earthquake", Arrays.asList(
            "động đất", "earthquake", "seismic", "chấn động"
        ));
        
        // Fire keywords
        addKeywords("Fire", Arrays.asList(
            "cháy", "fire", "hỏa hoạn", "cháy rừng", "wildfire"
        ));
        
        // Landslide keywords
        addKeywords("Landslide", Arrays.asList(
            "sạt lở", "landslide", "sạt lở đất", "lở đất"
        ));
        
        // Emergency/Relief keywords
        addKeywords("Emergency", Arrays.asList(
            "thiên tai", "disaster", "cứu trợ", "relief", 
            "khẩn cấp", "emergency", "cứu hộ", "rescue"
        ));
    }
    
    public void addKeywords(String category, List<String> keywords) {
        keywordsByCategory.computeIfAbsent(category, k -> new ArrayList<>())
            .addAll(keywords);
    }
    
    public void setKeywords(String category, List<String> keywords) {
        keywordsByCategory.put(category, new ArrayList<>(keywords));
    }
    
    public List<String> getKeywords(String category) {
        return new ArrayList<>(keywordsByCategory.getOrDefault(category, new ArrayList<>()));
    }
    
    public List<String> getAllKeywords() {
        List<String> allKeywords = new ArrayList<>();
        for (List<String> keywords : keywordsByCategory.values()) {
            allKeywords.addAll(keywords);
        }
        return allKeywords;
    }
    
    public Set<String> getCategories() {
        return keywordsByCategory.keySet();
    }
    
    /**
     * Check if text contains any disaster keywords
     */
    public boolean containsDisasterKeyword(String text) {
        if (text == null) return false;
        String lowerText = text.toLowerCase();
        
        for (String keyword : getAllKeywords()) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Detect which disaster categories match the text
     */
    public Set<String> detectCategories(String text) {
        Set<String> matchedCategories = new HashSet<>();
        if (text == null) return matchedCategories;
        
        String lowerText = text.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : keywordsByCategory.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    matchedCategories.add(entry.getKey());
                    break;
                }
            }
        }
        
        return matchedCategories;
    }
    
    /**
     * Load keywords from JSON file
     */
    public void loadFromJson(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            this.keywordsByCategory = gson.fromJson(reader, type);
        }
    }
    
    /**
     * Save keywords to JSON file
     */
    public void saveToJson(String filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(keywordsByCategory, writer);
        }
    }
    
    /**
     * Get JSON representation of keywords
     */
    public String toJson() {
        return gson.toJson(keywordsByCategory);
    }
}