package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;

public class StripLevel {

    /**
     * Process a CSV file: read from input, strip the "Type" column, and append to YagiComments.csv
     */
    public static void processFile(String inputCsv) {
        Path inputPath = Paths.get(inputCsv);
        Path outputPath = Paths.get("YagiComments.csv");

        // Regex to split CSV by comma while ignoring commas inside quotes
        Pattern csvPattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        // Check if output file exists and has content
        boolean outputHasContent = Files.exists(outputPath) && getFileSize(outputPath) > 0;

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, 
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            String line;
            boolean isInputHeader = true;

            System.out.println("📖 Reading from: " + inputPath.toAbsolutePath());
            System.out.println("📝 Appending to: " + outputPath.toAbsolutePath());

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Split the line into columns
                String[] cols = csvPattern.split(line);

                // We expect at least date and text columns (indices 0 and 1)
                if (cols.length >= 2) {
                    String date = cols[0];
                    String text = cols[1];

                    // Logic for Header
                    if (isInputHeader) {
                        // Only write the header if the output file was empty/new
                        if (!outputHasContent) {
                            writer.write("date,text");
                            writer.newLine();
                            outputHasContent = true;
                        }
                        isInputHeader = false;
                        continue; // Skip processing the header line as data
                    }

                    // Write the data rows (Date, Text)
                    writer.write(date + "," + text);
                    writer.newLine();
                }
            }

            System.out.println("✅ Successfully stripped and appended data.");

        } catch (IOException e) {
            System.err.println("❌ Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            processFile(args[0]);
        } else {
            processFile("YagiComments_fixed.csv");
        }
    }

    private static long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
}
