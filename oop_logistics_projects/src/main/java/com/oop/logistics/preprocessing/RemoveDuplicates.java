package com.oop.logistics.preprocessing;

import org.apache.commons.csv.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RemoveDuplicates {

    /**
     * Remove duplicate records based on TEXT column (index 1)
     * Overwrite the same CSV file safely (handles multiline text)
     */
    public static void removeDuplicatesInPlace(String csvPath) throws IOException {

        Set<String> seen = new HashSet<>();
        List<CSVRecord> kept = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("date", "text")
                .setSkipHeaderRecord(true)
                .setQuote('"')
                .setEscape('"')
                .setIgnoreEmptyLines(true)
                .setTrim(false)
                .build();

        try (
            Reader reader = new InputStreamReader(
                    new FileInputStream(csvPath), StandardCharsets.UTF_8);
            CSVParser parser = new CSVParser(reader, format)
        ) {
            for (CSVRecord record : parser) {
                if (record.size() < 2) continue;

                String text = normalizeText(record.get(1));

                if (seen.add(text)) {
                    kept.add(record);
                }
            }
        }

        // 🔁 overwrite same file
        try (
            Writer writer = new OutputStreamWriter(
                    new FileOutputStream(csvPath, false), StandardCharsets.UTF_8);
            CSVPrinter printer = new CSVPrinter(writer, format.withHeader())
        ) {
            for (CSVRecord r : kept) {
                printer.printRecord(r.get(0), r.get(1));
            }
        }

        System.out.println("✓ Removed duplicates safely: " + csvPath);
        System.out.println("✓ Records kept: " + kept.size());
    }

    /**
     * Normalize noisy text for duplicate detection
     * (DO NOT destroy content)
     */
    private static String normalizeText(String s) {
        if (s == null) return "";
        return s
                .replaceAll("\\s+", " ")   // collapse whitespace
                .trim();
    }

    
}
