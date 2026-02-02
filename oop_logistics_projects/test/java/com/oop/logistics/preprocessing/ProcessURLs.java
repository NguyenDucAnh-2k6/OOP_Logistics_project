package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class ProcessURLs {
    public static void main(String[] args) {
        Path targetFile = Paths.get("URL.csv");
        Path sourceFile2 = Paths.get("URL(1).csv");

        List<String> mergedRows = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        
        // Regex to split CSV by comma, ignoring commas inside quotes
        Pattern csvPattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        System.out.println("Reading files and merging...");

        // 1. Read the first file (URL.csv) - Keep Header
        readAndCollect(targetFile, mergedRows, seenUrls, csvPattern, true);

        // 2. Read the second file (URL(1).csv) - Skip Header
        readAndCollect(sourceFile2, mergedRows, seenUrls, csvPattern, false);

        // 3. Overwrite URL.csv with the merged data
        System.out.println("Writing merged data to URL.csv...");
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String row : mergedRows) {
                writer.write(row);
                writer.newLine();
            }
            System.out.println("Successfully merged files into URL.csv with " + mergedRows.size() + " unique rows.");
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private static void readAndCollect(Path path, List<String> rows, Set<String> seenUrls, Pattern pattern, boolean keepHeader) {
        if (!Files.exists(path)) {
            System.out.println("File not found: " + path.toString());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder rowBuffer = new StringBuilder();
            boolean insideQuotes = false;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                rowBuffer.append(line);

                // Handle multi-line CSV fields
                for (char c : line.toCharArray()) {
                    if (c == '"') insideQuotes = !insideQuotes;
                }

                if (insideQuotes) {
                    rowBuffer.append("\n");
                    continue;
                }

                String fullRow = rowBuffer.toString();
                rowBuffer.setLength(0);

                // Header Logic
                if (isFirstLine) {
                    if (keepHeader && rows.isEmpty()) {
                        rows.add(fullRow); // Add header only if it's the very first one
                    }
                    isFirstLine = false;
                    continue; 
                }

                // Deduplication Logic
                String[] cols = pattern.split(fullRow);
                // Assuming "url" is the second column (index 1) based on "date,url" structure
                if (cols.length >= 2) {
                    String url = cols[1].trim(); 
                    // Remove surrounding quotes from URL if present for cleaner comparison
                    url = url.replaceAll("^\"|\"$", "");
                    
                    if (!seenUrls.contains(url)) {
                        seenUrls.add(url);
                        rows.add(fullRow);
                    }
                } else {
                    // If row format is unexpected, decide whether to keep. 
                    // Usually safer to skip or log, but here we'll add if unique isn't violated or just skip.
                    // For now, let's skip malformed rows to be clean.
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + path.toString() + ": " + e.getMessage());
        }
    }
}
