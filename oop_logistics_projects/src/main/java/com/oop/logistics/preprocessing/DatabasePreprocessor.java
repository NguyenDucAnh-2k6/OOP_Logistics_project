package com.oop.logistics.preprocessing;

import com.oop.logistics.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class DatabasePreprocessor {

    public static void preprocessDisasterData(String disasterName, String sourceType) throws Exception {
        String selectSql, updateSql, deleteSql;

        if ("News".equalsIgnoreCase(sourceType)) {
            selectSql = "SELECT n.id, n.content, n.published_date FROM news n JOIN disasters d ON n.disaster_id = d.id WHERE d.name = ? AND n.source_type = ?";
            updateSql = "UPDATE news SET content = ?, published_date = ? WHERE id = ?";
            deleteSql = "DELETE FROM news WHERE id = ?";
        } else {
            selectSql = "SELECT c.id, c.content, c.published_date FROM comments c JOIN news n ON c.news_id = n.id JOIN disasters d ON n.disaster_id = d.id WHERE d.name = ? AND n.source_type = ?";
            updateSql = "UPDATE comments SET content = ?, published_date = ? WHERE id = ?";
            deleteSql = "DELETE FROM comments WHERE id = ?";
        }

        Set<String> seenTexts = new HashSet<>();
        int processed = 0, duplicates = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            
            selectStmt.setString(1, disasterName);
            selectStmt.setString(2, sourceType);
            ResultSet rs = selectStmt.executeQuery();

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String rawText = rs.getString("content");
                    String rawDate = rs.getString("published_date");

                    // 1. Clean Text (Stopwords, punctuation, emojis) via ProcessCSV
                    String cleanText = ProcessCSV.cleanText(rawText);
                    
                    // 2. Normalize Date via DateExtract
                    String cleanDate = DateExtract.formatDateToDDMMYYYY(rawDate);

                    // 3. Detect and Delete Duplicates
                    if (cleanText.isEmpty() || seenTexts.contains(cleanText)) {
                        deleteStmt.setInt(1, id);
                        deleteStmt.addBatch();
                        duplicates++;
                    } else {
                        seenTexts.add(cleanText);
                        updateStmt.setString(1, cleanText);
                        updateStmt.setString(2, cleanDate);
                        updateStmt.setInt(3, id);
                        updateStmt.addBatch();
                        processed++;
                    }
                }
                
                updateStmt.executeBatch();
                deleteStmt.executeBatch();
            }
        }
        System.out.println("✅ Preprocessing complete: Cleaned " + processed + " records, Removed " + duplicates + " duplicates.");
    }
}