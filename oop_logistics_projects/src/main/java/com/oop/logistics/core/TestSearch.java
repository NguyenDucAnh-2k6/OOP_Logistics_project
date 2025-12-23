package com.oop.logistics.core;

import java.time.LocalDate;

public class TestSearch {
    public static void main(String[] args) {

        DisasterSearchService service =
                new DisasterSearchService(
                        LocalDate.of(2024, 9, 1),
                        LocalDate.of(2025, 1, 31)
                );

        service.searchNewsUrls("lũ yagi");
    }
}
