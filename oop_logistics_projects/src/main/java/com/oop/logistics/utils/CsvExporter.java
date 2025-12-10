package com.oop.logistics.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.oop.logistics.models.DisasterEvent;

/**
 * Utility for exporting DisasterEvent data to CSV format.
 */
public class CsvExporter {

    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_HEADER = "SourceID,Timestamp,DateOnly,DisasterType,Location,Severity,Engagement,Description";

    /**
     * Exports a list of DisasterEvents to a CSV file.
     * @param events The list of events to export (already preprocessed/analyzed).
     * @param filePath The path to the output CSV file.
     * @throws IOException If an I/O error occurs.
     */
    public static void exportEventsToCsv(List<DisasterEvent> events, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
    // Write BOM for Excel compatibility (optional but recommended for Vietnamese CSVs)
            writer.write('\ufeff'); 
            writer.println(CSV_HEADER);
            writer.println(CSV_HEADER);

            for (DisasterEvent event : events) {
                // Ensure nulls are handled and description quotes are escaped for clean CSV
                String description = event.getDescription() != null ? 
                                     "\"" + event.getDescription().replace("\"", "\"\"") + "\"" : "";
                
                String timestamp = event.getTimestamp() != null ? event.getTimestamp() : "";
                String dateOnly = "";
                if (!timestamp.isEmpty()) {
                    // Assuming timestamp is cleaned to ISO format (YYYY-MM-DDTHH:MM:SS) in preprocessing
                    dateOnly = timestamp.substring(0, Math.min(10, timestamp.length()));
                }

                String line = event.getSourceId() + CSV_SEPARATOR +
                              timestamp + CSV_SEPARATOR +
                              dateOnly + CSV_SEPARATOR +
                              (event.getDisasterType() != null ? event.getDisasterType() : "Unknown") + CSV_SEPARATOR +
                              (event.getLocation() != null ? event.getLocation() : "") + CSV_SEPARATOR +
                              (event.getSeverity() != null ? event.getSeverity() : "") + CSV_SEPARATOR +
                              event.getEngagement() + CSV_SEPARATOR +
                              description;

                writer.println(line);
            }
            System.out.println("✓ Successfully exported " + events.size() + " events to " + filePath);
        }
    }
}