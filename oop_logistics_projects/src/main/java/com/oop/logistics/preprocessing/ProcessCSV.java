package com.oop.logistics.preprocessing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProcessCSV {

    // Danh sách từ dừng sẽ được nạp từ file stopwords.txt
    private static final Set<String> STOP_WORDS = new HashSet<>();

    // Khối static này sẽ chạy một lần duy nhất khi lớp được nạp
    static {
        loadStopWords();
    }

    private static void loadStopWords() {
        // Tên file nằm trong src/main/resources/
        String fileName = "stopwords.txt";
        
        try (InputStream is = ProcessCSV.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                System.err.println("⚠ Cảnh báo: Không tìm thấy file " + fileName + " trong resources. Sử dụng danh sách rỗng.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        STOP_WORDS.add(word);
                    }
                }
            }
            System.out.println("✅ Đã nạp " + STOP_WORDS.size() + " từ dừng (stopwords).");
        } catch (IOException e) {
            System.err.println("⚠ Lỗi khi đọc file stopwords: " + e.getMessage());
        }
    }

    public static void processFile(String inputFile, String outputFile) {
        File tempFile = new File(outputFile + ".tmp");
        
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
            PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))
        ) {
            System.out.println("Đang xử lý file: " + inputFile);

            String rawLine;
            StringBuilder buffer = new StringBuilder();
            boolean isHeader = true;

            while ((rawLine = br.readLine()) != null) {
                if (buffer.length() > 0) buffer.append('\n');
                buffer.append(rawLine);

                // Kiểm tra xem bản ghi CSV đã đầy đủ chưa (số lượng dấu ngoặc kép chẵn)
                if (isRecordComplete(buffer.toString())) {
                    String fullRecord = buffer.toString();
                    
                    if (isHeader) {
                        pw.println(fullRecord); 
                        isHeader = false;
                    } else {
                        List<String> columns = parseCsvRow(fullRecord);
                        
                        // Giả sử cột nội dung là cột thứ 3 (index 2)
                        if (columns.size() >= 3) {
                            String originalText = columns.get(2);
                            String cleanedText = cleanText(originalText);
                            columns.set(2, cleanedText);
                        }

                        pw.println(toCsvRow(columns));
                    }
                    buffer.setLength(0); // Xóa buffer cho dòng tiếp theo
                }
            }
            
            if (buffer.length() > 0) {
                pw.println(buffer.toString());
            }

        } catch (IOException e) {
            // In lỗi rõ ràng hơn thay vì ném RuntimeException
            System.err.println("❌ Lỗi xử lý file CSV: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Đổi tên file tạm thành file chính thức
        File original = new File(outputFile);
        if (original.exists() && !original.delete()) {
            System.err.println("⚠ Không thể xóa file cũ. Kết quả lưu tại: " + tempFile.getName());
            return;
        }
        if (!tempFile.renameTo(original)) {
            System.err.println("⚠ Không thể đổi tên file tạm thành: " + outputFile);
        } else {
            System.out.println("✅ Hoàn tất. Kết quả lưu tại: " + outputFile);
        }
    }

    private static boolean isRecordComplete(String s) {
        int quotes = 0;
        for (char c : s.toCharArray()) {
            if (c == '"') quotes++;
        }
        return quotes % 2 == 0;
    }

    private static List<String> parseCsvRow(String row) {
        List<String> columns = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
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
        columns.add(field.toString());
        return columns;
    }

    private static String toCsvRow(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            String field = columns.get(i);
            if (field == null) field = "";
            field = field.replace("\"", "\"\"");
            sb.append('"').append(field).append('"');
            if (i < columns.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    public static String cleanText(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Loại bỏ ký tự đặc biệt (giữ lại chữ, số, dấu câu cơ bản)
        String noEmoji = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", " ");

        // 2. Chuẩn hóa khoảng trắng
        String normalized = noEmoji.replaceAll("\\s+", " ").trim();

        // 3. Loại bỏ Stop Words
        String[] words = normalized.split(" ");
        StringBuilder cleanBuilder = new StringBuilder();
        
        for (String word : words) {
            String rawWord = word.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
            if (!STOP_WORDS.contains(rawWord) && !rawWord.isEmpty()) {
                cleanBuilder.append(word).append(" ");
            }
        }

        return cleanBuilder.toString().trim();
    }
}