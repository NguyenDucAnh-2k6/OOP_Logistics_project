package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NewsPreprocess: Utility to normalize dates in YagiNews.csv.
 * - Enforces a "date,text" header.
 * - Normalizes the Date column to dd/mm/yyyy.
 * - robustly handles multi-line text fields to prevent CSV corruption.
 */
public class NewsPreprocess {

    private static final String INPUT_FILE = "YagiNews.csv";
    private static final String OUTPUT_FILE = "YagiNews_normalized.csv";

    public static void main(String[] args) {
        normalizeNewsDateColumn();
    }

    public static void normalizeNewsDateColumn() {
        System.out.println("📋 Starting news date normalization...");
        int processed = 0;
        
        java.nio.file.Path inputPath = Paths.get(INPUT_FILE);
        java.nio.file.Path outputPath = Paths.get(OUTPUT_FILE);

        if (!Files.exists(inputPath)) {
            System.err.println("❌ Input file not found: " + INPUT_FILE);
            return;
        }

        try {
            // Check if we need to write a header (if file doesn't exist or is empty)
            boolean needHeader = !Files.exists(outputPath) || Files.size(outputPath) == 0;

            // Use CREATE and APPEND options to preserve existing data
            try (BufferedReader br = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
                 BufferedWriter bw = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, 
                         java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {

                CsvReader reader = new CsvReader(br);

                // 1. Write Header "date,text" ONLY if the file is new/empty
                if (needHeader) {
                    writeCsvRow(bw, Arrays.asList("date", "text"));
                }

                // 2. Read first row to check for existing header in the INPUT file
                List<String> firstRow = reader.readNextRow();
                if (firstRow != null && !firstRow.isEmpty()) {
                    // Check if the first row looks like a header (contains "date")
                    String firstCol = firstRow.get(0).toLowerCase();
                    
                    if (firstCol.contains("date")) {
                        System.out.println("ℹ️  Skipping existing header row in input.");
                        // It is a header, so we skip processing it
                    } else {
                        // It is NOT a header (it's data), so we process and write it
                        processAndWriteRow(bw, firstRow);
                        processed++;
                    }
                }

                // 3. Process remaining Data Rows
                List<String> row;
                while ((row = reader.readNextRow()) != null) {
                    processAndWriteRow(bw, row);
                    processed++;
                }

                System.out.println("\n✅ Normalization complete.");
                System.out.println("   Processed rows: " + processed);
                System.out.println("   Output appended to: " + OUTPUT_FILE);
            }

        } catch (IOException e) {
            System.err.println("❌ Error processing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processAndWriteRow(BufferedWriter bw, List<String> row) throws IOException {
        if (row.isEmpty()) return;

        List<String> outputRow = new ArrayList<>();
        
        // 1. Normalize Date (Column 0)
        // Extracts "28/9/2024" from string and converts to "28/09/2024"
        String originalDate = row.get(0);
        String normalizedDate = normalizeDate(originalDate);
        outputRow.add(normalizedDate);

        // 2. Add remaining columns (Text, etc.) as is
        // We start from index 1 to skip the original date column
        for (int i = 1; i < row.size(); i++) {
            outputRow.add(row.get(i));
        }

        writeCsvRow(bw, outputRow);
    }

    /**
     * Extracts date pattern (d/m/yyyy) and formats to dd/mm/yyyy.
     * Returns just the date part.
     */
    private static String normalizeDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "";
        }

        // Regex to find patterns like d/m/yyyy or dd/mm/yyyy
        // Matches: (1-2 digits) / (1-2 digits) / (4 digits)
        Pattern pattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher matcher = pattern.matcher(rawDate);

        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                String year = matcher.group(3);
                
                // Return in dd/mm/yyyy format (padding with zeros)
                return String.format("%02d/%02d/%s", day, month, year);
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        // If no date pattern found, return original (or empty if you prefer)
        return rawDate;
    }

    /**
     * Helper to write a CSV row with proper quoting.
     * Escapes double quotes inside content (" -> "") and wraps fields in quotes.
     */
    private static void writeCsvRow(BufferedWriter bw, List<String> fields) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            if (field == null) field = "";
            
            // Escape existing quotes by doubling them
            String escaped = field.replace("\"", "\"\"");
            
            // Wrap in quotes
            sb.append("\"").append(escaped).append("\"");
            
            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("\n");
        bw.write(sb.toString());
    }

    /**
     * Robust CSV Reader that handles multi-line text fields correctly.
     * It reads character-by-character to respect quotes and newlines.
     */
    static class CsvReader {
        private final Reader reader;
        private boolean endOfFile = false;

        public CsvReader(Reader reader) {
            this.reader = reader;
        }

        public List<String> readNextRow() throws IOException {
            if (endOfFile) return null;

            List<String> columns = new ArrayList<>();
            StringBuilder currentField = new StringBuilder();
            boolean inQuotes = false;
            boolean recordStarted = false;

            int cInt;
            while (true) {
                cInt = reader.read();
                
                // Handle EOF
                if (cInt == -1) {
                    endOfFile = true;
                    if (recordStarted) {
                        columns.add(currentField.toString());
                        return columns;
                    }
                    return null;
                }

                char c = (char) cInt;
                recordStarted = true;

                if (inQuotes) {
                    if (c == '"') {
                        // Check next char for escaped quote ("")
                        reader.mark(1);
                        int nextC = reader.read();
                        if (nextC == '"') {
                            // It was an escaped quote (""), add a single " to data
                            currentField.append('"');
                        } else {
                            // It was a closing quote
                            inQuotes = false;
                            reader.reset(); // Go back to process the delimiter
                        }
                    } else {
                        // Inside quotes, just add the character (including newlines)
                        currentField.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        // Field separator
                        columns.add(currentField.toString());
                        currentField.setLength(0); 
                    } else if (c == '\r') {
                        // Handle \r\n or just \r
                        reader.mark(1);
                        int nextC = reader.read();
                        if (nextC != '\n') {
                            reader.reset();
                        }
                        // End of line
                        columns.add(currentField.toString());
                        return columns;
                    } else if (c == '\n') {
                        // End of line
                        columns.add(currentField.toString());
                        return columns;
                    } else {
                        currentField.append(c);
                    }
                }
            }
        }
    }
}