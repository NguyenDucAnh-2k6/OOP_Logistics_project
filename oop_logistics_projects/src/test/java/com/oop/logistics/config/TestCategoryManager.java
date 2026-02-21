package com.oop.logistics.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryManager Tests")
class TestCategoryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCategoryManager.class);
    private CategoryManager categoryManager;
    private File tempJsonFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        logger.info("Setting up temporary JSON file for testing");
        categoryManager = new CategoryManager();
        
        // Create a fake JSON file in a temporary folder
        tempJsonFile = tempDir.resolve("test_keywords.json").toFile();
        try (FileWriter writer = new FileWriter(tempJsonFile)) {
            writer.write("{ \"Infrastructure\": [\"bridge\", \"road\"], \"Relief\": [\"food\"] }");
        }
    }

    @Test
    @DisplayName("Should load categories from JSON file successfully")
    void testLoadFromJson() {
        assertDoesNotThrow(() -> categoryManager.loadFromJson(tempJsonFile.getAbsolutePath()));
        
        Category infra = categoryManager.getCategory("Infrastructure");
        assertNotNull(infra, "Infrastructure category should be loaded");
        assertTrue(infra.getKeywords().contains("bridge"));
        assertEquals("DAMAGE", infra.getType(), "Default type should be set to DAMAGE");
    }

    @Test
    @DisplayName("Should add new keyword to existing category")
    void testAddKeywordToCategory() throws IOException {
        categoryManager.loadFromJson(tempJsonFile.getAbsolutePath());
        
        categoryManager.addKeywordToCategory("Relief", "water");
        
        Category relief = categoryManager.getCategory("Relief");
        assertTrue(relief.getKeywords().contains("water"), "New keyword 'water' should be added");
        assertEquals(2, relief.getKeywords().size(), "Relief should now have 2 keywords");
    }

    @Test
    @DisplayName("Should create new category if adding keyword to non-existent category")
    void testAddKeywordToNewCategory() {
        categoryManager.addKeywordToCategory("Medical", "doctor");
        
        Category medical = categoryManager.getCategory("Medical");
        assertNotNull(medical, "New category 'Medical' should be created");
        assertTrue(medical.getKeywords().contains("doctor"));
    }
}