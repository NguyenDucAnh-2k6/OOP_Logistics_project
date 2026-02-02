package com.oop.logistics.preprocessing;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DateExtract {

    /**
     * Update Date column for rows [fromIndex -> toIndex] (1-based, excluding header)
     *
     * @param inputCsv  path to input CSV
     * @param outputCsv path to output CSV
     * @param fromIndex starting row (1-based, after header)
     * @param toIndex   ending row (inclusive)
     * @param date      date to assign (dd-MM-yyyy or dd/MM/yyyy – tùy bạn)
     */
    public static void fillDateRange(
            String inputCsv,
            String outputCsv,
            int fromIndex,
            int toIndex,
            String date
    ) {

        // Read logical CSV records (handle quoted fields that contain newlines)
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputCsv))) {
            String raw;
            StringBuilder buf = new StringBuilder();
            while ((raw = br.readLine()) != null) {
                if (buf.length() > 0) buf.append('\n');
                buf.append(raw);

                // Count quotes to detect record boundary. When quotes count is even, record likely complete.
                int quoteCount = 0;
                for (int i = 0; i < buf.length(); i++) if (buf.charAt(i) == '"') quoteCount++;

                if (quoteCount % 2 == 0) {
                    lines.add(buf.toString());
                    buf.setLength(0);
                }
            }
            if (buf.length() > 0) { // leftover (malformed file) -> add as-is
                lines.add(buf.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ Cannot read CSV file", e);
        }

        if (lines.isEmpty()) {
            System.out.println("⚠ CSV file is empty");
            return;
        }

        // Header stays untouched
        String header = lines.get(0);

        List<String> output = new ArrayList<>();
        output.add(header);

        int dataRowIndex = 0; // counts data rows (excluding header)

        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i);
            dataRowIndex++;

            if (dataRowIndex >= fromIndex && dataRowIndex <= toIndex) {
                row = replaceDateColumn(row, date);
            }

            // Preserve original row exactly (except when replaced above)
            output.add(row);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(outputCsv))) {
            for (String l : output) {
                pw.println(l);
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ Cannot write CSV file", e);
        }

        System.out.printf(
                "✅ Updated date='%s' for rows %d → %d%n",
                date, fromIndex, toIndex
        );
    }

    /**
     * Replace the first CSV column (Date) safely
     */
    private static String replaceDateColumn(String csvLine, String newDate) {
        if (csvLine == null || csvLine.isEmpty()) return csvLine;

        // If first char is a quote, find the closing quote taking into account escaped quotes ""
        if (csvLine.charAt(0) == '"') {
            int i = 1;
            while (i < csvLine.length()) {
                if (csvLine.charAt(i) == '"') {
                    // If next char is also a quote, it's an escaped quote -> skip both
                    if (i + 1 < csvLine.length() && csvLine.charAt(i + 1) == '"') {
                        i += 2;
                        continue;
                    }
                    // Found closing quote
                    int after = i + 1;
                    // Expect comma after closing quote
                    if (after < csvLine.length() && csvLine.charAt(after) == ',') {
                        String rest = csvLine.substring(after + 1);
                        return "\"" + newDate + "\"," + rest;
                    } else {
                        // malformed - fallback
                        return "\"" + newDate + "\"" + csvLine.substring(i + 1);
                    }
                }
                i++;
            }
            // no closing quote found -> fallback
            return csvLine;
        } else {
            // not quoted first field: replace up to first comma
            int c = csvLine.indexOf(',');
            if (c == -1) return newDate;
            String rest = csvLine.substring(c + 1);
            return newDate + "," + rest;
        }
    }

    /**
     * Helper: current date dd-MM-yyyy
     */
    public static String getCurrentDateDDMMYYYY() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%02d/%02d/%04d",
            now.getDayOfMonth(),
            now.getMonthValue(),
            now.getYear());
    }

    /**
     * Helper: current datetime (log purpose)
     */
    public static String getCurrentDateTime() {
        return java.time.LocalDateTime.now().toString();
    }

    /**
     * Convert various date string formats to dd/MM/yyyy.
     * Returns "Unknown" when parsing fails.
     */
    /*public static String formatDateToDDMMYYYY(String dateStr) {
        if (dateStr == null) return "Unknown";
        dateStr = dateStr.trim();
        if (dateStr.isEmpty()) return "Unknown";

        // Try to find dd/MM/yyyy or d/M/yyyy or dd-MM-yyyy
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("(\\d{1,2})[\\/\\-](\\d{1,2})[\\/\\-](\\d{2,4})");
        java.util.regex.Matcher m1 = p1.matcher(dateStr);
        if (m1.find()) {
            try {
                int d = Integer.parseInt(m1.group(1));
                int mo = Integer.parseInt(m1.group(2));
                int y = Integer.parseInt(m1.group(3));
                if (y < 100) y += 2000;
                return String.format("%02d/%02d/%04d", d, mo, y);
            } catch (Exception ignored) {}
        }

        // Try ISO yyyy-MM-dd or yyyy/MM/dd
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("(\\d{4})[\\-\\/](\\d{1,2})[\\-\\/](\\d{1,2})");
        java.util.regex.Matcher m2 = p2.matcher(dateStr);
        if (m2.find()) {
            try {
                int y = Integer.parseInt(m2.group(1));
                int mo = Integer.parseInt(m2.group(2));
                int d = Integer.parseInt(m2.group(3));
                return String.format("%02d/%02d/%04d", d, mo, y);
            } catch (Exception ignored) {}
        }

        // Try Vietnamese-like patterns: e.g. "Thứ bảy, 28/9/2024, 14:00 (GMT+7)"
        java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("(\\d{1,2})\\s*[\\/\\-]\\s*(\\d{1,2})\\s*[\\/\\-]\\s*(\\d{4})");
        java.util.regex.Matcher m3 = p3.matcher(dateStr);
        if (m3.find()) {
            try {
                int d = Integer.parseInt(m3.group(1));
                int mo = Integer.parseInt(m3.group(2));
                int y = Integer.parseInt(m3.group(3));
                return String.format("%02d/%02d/%04d", d, mo, y);
            } catch (Exception ignored) {}
        }

        // As a last resort, try to extract any 4-digit year and surrounding numbers
        java.util.regex.Pattern p4 = java.util.regex.Pattern.compile("(\\d{1,2})[^\\d\\n\\r]{1,4}(\\d{1,2})[^\\d\\n\\r]{1,4}(\\d{4})");
        java.util.regex.Matcher m4 = p4.matcher(dateStr);
        if (m4.find()) {
            try {
                int d = Integer.parseInt(m4.group(1));
                int mo = Integer.parseInt(m4.group(2));
                int y = Integer.parseInt(m4.group(3));
                return String.format("%02d/%02d/%04d", d, mo, y);
            } catch (Exception ignored) {}
        }

        return "Unknown";
    }*/
    public static String formatDateToDDMMYYYY(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return rawDate;
        }

        try {
            // Split by comma to separate the date part
            // "Thứ bảy, 28/9/2024, 14:00 (GMT+7)" -> ["Thứ bảy", " 28/9/2024", " 14:00 (GMT+7)"]
            String[] parts = rawDate.split(",");
            
            if (parts.length >= 2) {
                String datePart = parts[1].trim(); // "28/9/2024"
                
                // Split by slash to pad day and month
                String[] dmy = datePart.split("/");
                if (dmy.length == 3) {
                    int day = Integer.parseInt(dmy[0]);
                    int month = Integer.parseInt(dmy[1]);
                    String year = dmy[2];
                    
                    // Format as dd/mm/yyyy
                    return String.format("%02d/%02d/%s", day, month, year);
                }
            }
        } catch (Exception e) {
            // In case of parsing error, return original string or handle log
            System.err.println("Error parsing date: " + rawDate);
        }
        
        return rawDate;
    }
}
