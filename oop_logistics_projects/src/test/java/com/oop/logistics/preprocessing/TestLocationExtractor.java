package com.oop.logistics.preprocessing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocationExtractor Tests")
class TestLocationExtractor {

    @Test
    @DisplayName("Should extract and format known provinces and cities")
    void testExtractAllLocations() {
        String text = "Bão lớn càn quét qua hà nội, sau đó tiến vào tp HỒ CHÍ MINH gây ngập tại thủ đức.";
        
        List<String> locations = LocationExtractor.extractAllLocations(text);
        
        assertEquals(3, locations.size(), "Should find exactly 3 locations");
        assertTrue(locations.contains("Hà Nội"));
        assertTrue(locations.contains("Hồ Chí Minh"));
        assertTrue(locations.contains("Thủ Đức"));
    }

    @Test
    @DisplayName("Should not duplicate locations if mentioned multiple times")
    void testDuplicateLocations() {
        String text = "Đà Nẵng mưa lớn. Chính quyền đà nẵng thông báo khẩn.";
        
        List<String> locations = LocationExtractor.extractAllLocations(text);
        
        assertEquals(1, locations.size(), "Should only contain one entry for Da Nang");
        assertEquals("Đà Nẵng", locations.get(0));
    }
}