package com.oop.logistics.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/**
 * Manages damage categories and relief item categories
 * Allows easy updates and modifications
 */
public class CategoryManager {
    
    private Map<String, Category> categories;
    private final Gson gson;
    
    public CategoryManager() {
        this.gson = new Gson();
        this.categories = new HashMap<>();
        initializeDefaultCategories();
    }
    
    private void initializeDefaultCategories() {
        // Damage categories
        addCategory(new Category("Infrastructure", "DAMAGE", Arrays.asList(
            "đường", "cầu", "nhà", "điện", "nước", "cột", "kính"
        )));
        
        addCategory(new Category("Housing", "DAMAGE", Arrays.asList(
            "phá", "hỏng", "dột", "tốc", "mái", "sập", "đổ"
        )));
        
        addCategory(new Category("Agriculture", "DAMAGE", Arrays.asList(
            "cây", "gia súc", "gia cầm", "nông", "vụ", "lúa", "ngô", "khoai", "sắn"
        )));
        
        addCategory(new Category("Human", "DAMAGE", Arrays.asList(
            "thương vong", "thiệt", "chết", "mất tích", "thương", "tử"
        )));
        
        // Relief item categories
        addCategory(new Category("Food", "RELIEF", Arrays.asList(
            "cơm", "mì", "đông", "đóng", "cung"
        )));
        
        addCategory(new Category("Medical", "RELIEF", Arrays.asList(
            "thuốc", "sơ cứu", "băng", "dược", "sức khỏe", "phục hồi"
        )));
        
        addCategory(new Category("Shelter", "RELIEF", Arrays.asList(
            "lều", "chăn", "quần", "áo", "tạm thời"
        )));
        
        addCategory(new Category("Hygiene", "RELIEF", Arrays.asList(
            "xà phòng", "vệ sinh", "toa lét", "nước sạch", "sát khuẩn"
        )));
    }
    
    public void addCategory(Category category) {
        categories.put(category.getName(), category);
    }
    
    public Category getCategory(String name) {
        return categories.get(name);
    }
    
    public List<Category> getCategoriesByType(String type) {
        List<Category> result = new ArrayList<>();
        for (Category category : categories.values()) {
            if (category.getType().equalsIgnoreCase(type)) {
                result.add(category);
            }
        }
        return result;
    }
    
    public Set<String> getAllCategoryNames() {
        return categories.keySet();
    }
    
    /**
     * Detect which categories are mentioned in text
     */
    public Map<String, Integer> detectCategoriesInText(String text) {
        Map<String, Integer> categoryMatches = new HashMap<>();
        if (text == null) return categoryMatches;
        
        String lowerText = text.toLowerCase();
        
        for (Category category : categories.values()) {
            int matchCount = 0;
            for (String keyword : category.getKeywords()) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            if (matchCount > 0) {
                categoryMatches.put(category.getName(), matchCount);
            }
        }
        
        return categoryMatches;
    }
    
    public void loadFromJson(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, Category>>(){}.getType();
            this.categories = gson.fromJson(reader, type);
        }
    }
    
    public void saveToJson(String filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(categories, writer);
        }
    }
    
    /**
     * Inner class representing a category
     */
    public static class Category {
        private String name;
        private String type; // "DAMAGE" or "RELIEF"
        private List<String> keywords;
        private String description;
        
        public Category() {}
        
        public Category(String name, String type, List<String> keywords) {
            this.name = name;
            this.type = type;
            this.keywords = new ArrayList<>(keywords);
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public void addKeyword(String keyword) {
            if (keywords == null) keywords = new ArrayList<>();
            keywords.add(keyword);
        }
    }
}