package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ProcessCSV {

    // List of common Vietnamese stop words ("naive words") that don't help in sentiment analysis
    private static final Set<String> STOP_WORDS = Set.of(
    // Common Vietnamese particles and conjunctions
    "là", "có", "thì", "mà", "và", "nhưng", "hoặc", "nếu", "vì", "do", "tại", 
    "của", "ở", "với", "cho", "để", "về", "từ", "lên", "xuống", "ra", "vào", 
    "đến", "đi", "lại", "qua",

    // Pronouns and classifiers
    "tôi", "ta", "mình", "bạn", "nó", "họ", "chúng", "ông", "bà", "anh", "chị", "em",
    "cái", "con", "chiếc", "những", "các", "mọi", "người", "nhà", 
    "này", "kia", "đó", "ấy", "đây", "đâu", "nào",

    // Adverbs and auxiliary verbs
    "đã", "đang", "sẽ", "vừa", "mới", "từng", 
    "rất", "quá", "lắm", "hơi", "khá", 
    "không", "chẳng", "chưa", "được", "bị", "phải", "nên", "cần",
    "rồi", "xong", "luôn", "ngay", "thôi", "như", "giống", "bằng", 

    // Colloquial/Slang found in the file
    "ko", "k", "dc", "j", "vs", "ak", "ah", "u", "thế", "vậy", "sao", "gì", "chứ"
    );

    public static void main(String[] args) {
        // Input and Output are the same file as requested (processed in place via temp file)
        String inputFile = "YagiComments_fixed.csv";
        String outputFile = "YagiComments_fixed.csv";
        
        processFile(inputFile, outputFile);
    }

    public static void processFile(String inputFile, String outputFile) {
        File tempFile = new File(outputFile + ".tmp");
        
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
            PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))
        ) {
            System.out.println("Processing " + inputFile + "...");

            String rawLine;
            StringBuilder buffer = new StringBuilder();
            boolean isHeader = true;

            while ((rawLine = br.readLine()) != null) {
                if (buffer.length() > 0) buffer.append('\n');
                buffer.append(rawLine);

                // Check if the CSV record is complete (even number of quotes)
                if (isRecordComplete(buffer.toString())) {
                    String fullRecord = buffer.toString();
                    
                    if (isHeader) {
                        // Write header as is, or normalize it
                        pw.println(fullRecord); 
                        isHeader = false;
                    } else {
                        // 1. Parse columns
                        List<String> columns = parseCsvRow(fullRecord);
                        
                        // 2. Process the 'Text' column (assuming it's the 3rd column: Date, URL, Text)
                        if (columns.size() >= 3) {
                            String originalText = columns.get(2);
                            String cleanedText = cleanText(originalText);
                            columns.set(2, cleanedText);
                        }

                        // 3. Write back safely
                        pw.println(toCsvRow(columns));
                    }
                    
                    // Clear buffer for next record
                    buffer.setLength(0);
                }
            }
            
            // Handle any remaining content (malformed last line)
            if (buffer.length() > 0) {
                pw.println(buffer.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error processing CSV file", e);
        }

        // Replace original file with temp file
        File original = new File(outputFile);
        if (original.exists()) {
            if (!original.delete()) {
                System.err.println("⚠ Could not delete original file. output saved to " + tempFile.getName());
                return;
            }
        }
        if (!tempFile.renameTo(original)) {
            System.err.println("⚠ Could not rename temp file to " + outputFile);
        } else {
            System.out.println("✅ Finished. Output saved to " + outputFile);
        }
    }

    /**
     * Checks if a CSV buffer has an even number of quotes, indicating a closed record.
     */
    private static boolean isRecordComplete(String s) {
        int quotes = 0;
        for (char c : s.toCharArray()) {
            if (c == '"') quotes++;
        }
        return quotes % 2 == 0;
    }

    /**
     * Parses a single CSV row (handling quoted fields with commas/newlines).
     */
    private static List<String> parseCsvRow(String row) {
        List<String> columns = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            
            if (inQuotes) {
                if (c == '"') {
                    // Check for escaped quote ("")
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
                        field.append('"');
                        i++; // Skip next quote
                    } else {
                        inQuotes = false; // End of quoted field
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    columns.add(field.toString());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }
        columns.add(field.toString()); // Add last column
        return columns;
    }

    /**
     * Converts a list of strings back to a valid CSV row.
     * Always wraps fields in quotes to avoid CSVLint errors.
     */
    private static String toCsvRow(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            String field = columns.get(i);
            if (field == null) field = "";
            
            // Escape double quotes inside the string: " -> ""
            field = field.replace("\"", "\"\"");
            
            // Wrap in quotes
            sb.append('"').append(field).append('"');
            
            if (i < columns.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Cleans the text content:
     * 1. Removes emojis and symbols.
     * 2. Removes stop words.
     * 3. Normalizes whitespace.
     */
    private static String cleanText(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Remove Emojis and non-text symbols
        // Keep: Letters (\p{L}), Numbers (\p{N}), Punctuation (\p{P}), Whitespace (\p{Z})
        // This regex removes emojis, dingbats, and other symbols.
        String noEmoji = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", " ");

        // 2. Normalize whitespace (tabs/newlines -> space, multiple spaces -> single space)
        String normalized = noEmoji.replaceAll("\\s+", " ").trim();

        // 3. Remove Naive/Stop Words
        // We split by space, check against dictionary, and rebuild
        String[] words = normalized.split(" ");
        StringBuilder cleanBuilder = new StringBuilder();
        
        for (String word : words) {
            // Remove punctuation from the word to check against stop list (e.g., "và," -> "và")
            String rawWord = word.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
            
            if (!STOP_WORDS.contains(rawWord) && !rawWord.isEmpty()) {
                cleanBuilder.append(word).append(" ");
            }
        }

        return cleanBuilder.toString().trim();
    }
}
