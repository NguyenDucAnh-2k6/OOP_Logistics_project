package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class StripLevel {

    /**
     * Process a CSV file: read from input, strip the "Type" column (index 2), and append to YagiComments.csv
     */
    public static void processFile(String inputCsv) {
        Path inputPath = Paths.get(inputCsv);
        Path outputPath = Paths.get("YagiComments.csv");

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, 
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            System.out.println("📖 Reading from: " + inputPath.toAbsolutePath());
            System.out.println("📝 Appending to: " + outputPath.toAbsolutePath());

            // Check if output is empty to write header if needed (optional)
            if (Files.exists(outputPath) && Files.size(outputPath) == 0) {
                writer.write("date,text");
                writer.newLine();
            }

            String line;
            StringBuilder sb = new StringBuilder();
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                sb.append(line);

                // Check for balanced quotes
                if (isRowComplete(sb.toString())) {
                    String fullRow = sb.toString();
                    sb.setLength(0); // Clear buffer

                    if (isHeader) {
                        isHeader = false;
                        continue; // Skip header of input file
                    }

                    List<String> cols = parseCsvRow(fullRow);
                    
                    // We expect date (0), text (1), type (2). We want 0 and 1.
                    if (cols.size() >= 2) {
                        String date = cols.get(0);
                        String text = cols.get(1);
                        
                        // Handle quotes for output: ensure text is properly quoted if it contains commas or newlines
                        // Since we parsed it, 'text' is the raw content. We need to re-quote it for CSV output.
                        String csvDate = escapeCsv(date);
                        String csvText = escapeCsv(text);
                        
                        writer.write(csvDate + "," + csvText);
                        writer.newLine();
                    }
                } else {
                    sb.append("\n"); // Append newline that readLine() consumed
                }
            }

            System.out.println("✅ Successfully processed and appended to " + outputPath.getFileName());

        } catch (IOException e) {
            System.err.println("❌ Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isRowComplete(String row) {
        int quotes = 0;
        for (char c : row.toCharArray()) {
            if (c == '"') quotes++;
        }
        return quotes % 2 == 0;
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        // If value contains quote, comma or newline, wrap in quotes and escape existing quotes
        if (val.contains("\"") || val.contains(",") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private static List<String> parseCsvRow(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    current.append('"'); // Escaped quote
                    i++; 
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}