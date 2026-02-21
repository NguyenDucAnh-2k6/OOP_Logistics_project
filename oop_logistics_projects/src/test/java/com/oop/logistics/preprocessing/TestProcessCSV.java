package com.oop.logistics.preprocessing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessCSV Tests")
class TestProcessCSV {

    @Test
    @DisplayName("Should clean emojis and normalize extra spaces")
    void testCleanTextEmojisAndSpaces() {
        String input = "Bão Yagi 😱 !!!     Ngập lụt khắp nơi...  ";
        
        String result = ProcessCSV.cleanText(input);
        
        assertFalse(result.contains("😱"), "Emojis should be removed");
        assertFalse(result.contains("     "), "Extra spaces should be normalized");
        assertTrue(result.contains("Bão Yagi"), "Valid non-stopword text should remain intact");
    }

    @Test
    @DisplayName("Should handle null and empty strings safely")
    void testCleanTextNull() {
        assertEquals("", ProcessCSV.cleanText(null));
        assertEquals("", ProcessCSV.cleanText("   "));
    }

    @Test
    @DisplayName("Should correctly load and remove stopwords from stopwords.txt")
    void testCleanTextRemovesStopWords() {
        // Arrange
        String input = "Bão Yagi và lũ lụt là những thảm họa của thiên nhiên";
        
        // Act
        String result = ProcessCSV.cleanText(input);
        
        // Assert
        // (Modify the stop words below if your stopwords.txt has different words)
        assertFalse(result.contains(" và "), "Stop word 'và' should be removed");
        assertFalse(result.contains(" là "), "Stop word 'là' should be removed");
        assertFalse(result.contains(" những "), "Stop word 'những' should be removed");
        assertFalse(result.contains(" của "), "Stop word 'của' should be removed");
        
        assertTrue(result.contains("Bão Yagi"), "Meaningful keywords should be kept");
        assertTrue(result.contains("thiên nhiên"), "Meaningful keywords should be kept");
    }
}