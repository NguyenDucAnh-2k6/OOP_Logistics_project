package com.oop.logistics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class KeywordManager {
    private Map<String, List<String>> keywordsMap = new HashMap<>();
    private String currentFilePath; // Store path to save later

    public void loadFromJson(String filePath) throws IOException {
        this.currentFilePath = filePath;
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            keywordsMap = gson.fromJson(reader, type);
            if (keywordsMap == null) keywordsMap = new HashMap<>();
        }
    }

    // --- NEW: Feature to Add and Save ---

    public void addKeyword(String category, String newKeyword) {
        if (!keywordsMap.containsKey(category)) {
            keywordsMap.put(category, new ArrayList<>());
        }
        
        List<String> list = keywordsMap.get(category);
        // Avoid duplicates
        if (!list.contains(newKeyword.toLowerCase())) {
            list.add(newKeyword.toLowerCase());
        }
    }

    public void saveChanges() throws IOException {
        if (currentFilePath == null) return;
        
        // Pretty print for readability
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(currentFilePath)) {
            gson.toJson(keywordsMap, writer);
        }
    }

    // --- Existing Getters ---

    public List<String> getKeywords(String key) {
        return keywordsMap.getOrDefault(key, Collections.emptyList());
    }

    public Map<String, List<String>> getAllKeywords() {
        return keywordsMap;
    }
    
    public Set<String> getCategories() {
        return keywordsMap.keySet();
    }
}