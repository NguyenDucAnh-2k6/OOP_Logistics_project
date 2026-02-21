package com.oop.logistics.preprocessing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DateExtract Tests")
class TestDateExtract {

    @ParameterizedTest
    @DisplayName("Should correctly format known date patterns to dd/MM/yyyy")
    @CsvSource({
        "'Thứ bảy, 28/9/2024, 14:00 (GMT+7)', '28/09/2024'", // Vietnamese format
        "'2024-09-07T15:23:00Z', '07/09/2024'",              // ISO format
        "'15-08-2025', '15/08/2025'",                        // Dashes
        "'1/2/2023', '01/02/2023'",                          // Single digits
        "'Random text with no date', 'Unknown'"              // Invalid string
    })
    void testFormatDateToDDMMYYYY(String rawDate, String expectedCleanDate) {
        String result = DateExtract.formatDateToDDMMYYYY(rawDate);
        assertEquals(expectedCleanDate, result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should return 'Unknown' for null or empty strings")
    void testFormatDateWithNullOrEmpty(String rawDate) {
        assertEquals("Unknown", DateExtract.formatDateToDDMMYYYY(rawDate));
    }
}