package com.oop.logistics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class CategoryManager {
    // Internal storage remains as Category objects to support the rest of the app
    private Map<String, Category> categoryMap = new HashMap<>();
    private String currentFilePath;

    /**
     * Loads from a simple dictionary JSON (damage_keywords.json).
     * Format: { "Infrastructure": ["word1", "word2"], "People": ["word3"] }
     */
    public void loadFromJson(String filePath) throws IOException {
        this.currentFilePath = filePath;
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(filePath)) {
            // 1. Read as a Map of Lists (Simple Format)
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> rawMap = gson.fromJson(reader, type);

            if (rawMap == null) rawMap = new HashMap<>();

            // 2. Convert to Category Objects
            categoryMap.clear();
            for (Map.Entry<String, List<String>> entry : rawMap.entrySet()) {
                String name = entry.getKey();
                List<String> keywords = entry.getValue();

                // Create a Category object automatically
                // We default the type to "DAMAGE" since we are loading damage keywords
                Category cat = new Category();
                cat.setName(name);
                cat.setType("DAMAGE"); 
                cat.setKeywords(keywords);
                cat.setDescription("Auto-loaded from " + name);

                categoryMap.put(name, cat);
            }
        }
    }

    /**
     * Saves changes back to the simple JSON format.
     */
    public void saveChanges() throws IOException {
        if (currentFilePath == null) return;

        // 1. Convert back to simple map for saving
        Map<String, List<String>> simpleMap = new LinkedHashMap<>();
        for (Map.Entry<String, Category> entry : categoryMap.entrySet()) {
            simpleMap.put(entry.getKey(), entry.getValue().getKeywords());
        }

        // 2. Write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(currentFilePath)) {
            gson.toJson(simpleMap, writer);
        }
    }

    public void addKeywordToCategory(String categoryName, String newKeyword) {
        Category cat = categoryMap.get(categoryName);
        if (cat != null) {
            List<String> keywords = cat.getKeywords();
            if (keywords == null) keywords = new ArrayList<>();
            
            if (!keywords.contains(newKeyword.toLowerCase())) {
                keywords.add(newKeyword.toLowerCase());
                cat.setKeywords(keywords);
            }
        } else {
            // Optional: Create new category if it doesn't exist
            Category newCat = new Category();
            newCat.setName(categoryName);
            newCat.setType("DAMAGE");
            newCat.setKeywords(new ArrayList<>(Collections.singletonList(newKeyword.toLowerCase())));
            categoryMap.put(categoryName, newCat);
        }
    }

    // --- Getters ---

    public Category getCategory(String name) {
        return categoryMap.get(name);
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categoryMap.values());
    }
    
    public Set<String> getAllCategoryNames() {
        return categoryMap.keySet();
    }
}