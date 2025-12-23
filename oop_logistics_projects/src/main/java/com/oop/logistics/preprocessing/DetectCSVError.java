package com.oop.logistics.preprocessing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DetectCSVError {

    public static void main(String[] args) {
        String fileName = "YagiComments_fixed.csv"; // Make sure this file is in the same directory

        System.out.println("--- 1. Checking for odd number of quotes (Heuristic) ---");
        checkOddQuotes(fileName);

        System.out.println("\n--- 2. Checking for field count inconsistencies ---");
        checkFieldCounts(fileName);
    }

    private static void checkOddQuotes(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                int quoteCount = 0;
                for (char c : line.toCharArray()) {
                    if (c == '"') {
                        quoteCount++;
                    }
                }
                
                // If the number of quotes in a line is odd, it's likely an error
                if (quoteCount % 2 != 0) {
                    System.out.println("[POTENTIAL ERROR] Line " + lineNumber + " has an odd number of quotes (" + quoteCount + "):");
                    System.out.println("    Content: " + (line.length() > 60 ? line.substring(0, 60) + "..." : line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void checkFieldCounts(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            int expectedFields = -1;
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                // Parse the line into fields respecting quotes
                List<String> fields = parseCsvLine(line);
                
                if (lineNumber == 1) {
                    expectedFields = fields.size();
                    System.out.println("Header detected with " + expectedFields + " columns: " + fields);
                } else {
                    if (fields.size() != expectedFields) {
                        System.out.println("[STRUCTURE ERROR] Row " + lineNumber + " has " + fields.size() + 
                                           " columns (Expected " + expectedFields + ").");
                        System.out.println("    Content: " + fields);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    // A simple CSV parser that respects quotes to count fields
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes; // Toggle quote state
            } else if (c == ',' && !inQuotes) {
                // If we hit a comma and are NOT inside quotes, it's a separator
                fields.add(currentField.toString());
                currentField.setLength(0); // Reset buffer for next field
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString()); // Add the last field
        
        return fields;
    }
}