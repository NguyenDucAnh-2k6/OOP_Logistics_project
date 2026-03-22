package com.oop.logistics.preprocessing;

import com.oop.logistics.config.KeywordManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Extracts location information from text (Static Utility)
 */
public class LocationExtractor {
    
    private static final Set<String> provinces = new HashSet<>();
    private static final Set<String> cities = new HashSet<>();
    
    // Load data when the class is first accessed
    static {
        loadLocations();
    }
    
    private LocationExtractor() {}

    /**
     * Traverses up the directory tree to reliably locate the external config file.
     * This acts like Python's os.path manipulation.
     */
    private static String resolveConfigPath(String fileName) {
        // Start at the current working directory
        Path currentPath = Paths.get("").toAbsolutePath();
        
        while (currentPath != null) {
            // Try matching exactly in the current directory
            Path directPath = currentPath.resolve("external config").resolve(fileName);
            if (Files.exists(directPath)) {
                return directPath.toString();
            }
            
            // Try matching inside the oop_logistics_projects subfolder
            Path subfolderPath = currentPath.resolve("oop_logistics_projects").resolve("external config").resolve(fileName);
            if (Files.exists(subfolderPath)) {
                return subfolderPath.toString();
            }
            
            // Move one directory up (os.path.dirname equivalent)
            currentPath = currentPath.getParent();
        }
        
        // Fallback if not found (will let the system throw a standard IOException later)
        return "external config/" + fileName;
    }
    
    public static void loadLocations() {
        KeywordManager manager = new KeywordManager();
        String resolvedPath = resolveConfigPath("location_keywords.json");
        
        try {
            manager.loadFromJson(resolvedPath);
            provinces.clear();
            cities.clear();
            provinces.addAll(manager.getKeywords("province"));
            cities.addAll(manager.getKeywords("city"));
        } catch (IOException e) {
            System.err.println("Failed to load location keywords from " + resolvedPath + ": " + e.getMessage());
        }
    }
    
    /**
     * Extract all possible locations from text, case-insensitive and deduplicated.
     */
    public static List<String> extractAllLocations(String text) {
        Set<String> uniqueLocations = new HashSet<>();
        
        if (text == null || text.isEmpty()) return new ArrayList<>();
        
        // Convert text to lowercase once for faster, case-insensitive searching
        String lowerText = text.toLowerCase();
        
        for (String province : provinces) {
            if (lowerText.contains(province.toLowerCase())) {
                uniqueLocations.add(capitalizeWords(province));
            }
        }
        for (String city : cities) {
            if (lowerText.contains(city.toLowerCase())) {
                uniqueLocations.add(capitalizeWords(city));
            }
        }
        
        return new ArrayList<>(uniqueLocations);
    }
    
    /**
     * Helper to ensure all locations are formatted beautifully for the UI (e.g. "bắc ninh" -> "Bắc Ninh")
     */
    private static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
            }
        }
        return capitalized.toString().trim();
    }
    
    /**
     * Add custom location to the recognizer in memory
     */
    public static void addProvince(String province) {
        if (province != null && !province.trim().isEmpty()) {
            provinces.add(province.trim());
        }
    }
    
    public static void addCity(String city) {
        if (city != null && !city.trim().isEmpty()) {
            cities.add(city.trim());
        }
    }
}