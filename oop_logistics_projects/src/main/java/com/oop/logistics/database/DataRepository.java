package com.oop.logistics.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    public static class AnalysisData {
        public List<String> texts = new ArrayList<>();
        public List<String> dates = new ArrayList<>();
    }

    // Fetch texts and dates for a specific disaster
    // Fetch texts and dates for a specific disaster AND source
    public AnalysisData getAnalysisData(String disasterName, String sourceType) {
        AnalysisData data = new AnalysisData();
        String sql;
        
        if ("News".equalsIgnoreCase(sourceType)) {
            // Fetch actual article content for News
            sql = "SELECT n.content, n.published_date FROM news n " +
                  "JOIN disasters d ON n.disaster_id = d.id " +
                  "WHERE d.name = ? AND n.source_type = ?";
        } else {
            // Fetch comment content for Facebook
            sql = "SELECT c.content, c.published_date FROM comments c " +
                  "JOIN news n ON c.news_id = n.id " +
                  "JOIN disasters d ON n.disaster_id = d.id " +
                  "WHERE d.name = ? AND n.source_type = ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, disasterName);
            pstmt.setString(2, sourceType);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String content = rs.getString("content");
                String date = rs.getString("published_date");
                
                if (content != null && !content.trim().isEmpty()) {
                    data.texts.add(content);
                    data.dates.add(date != null ? date : "01/01/1970");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching data for analysis: " + e.getMessage());
        }
        
        return data;
    }

    // Save a News Article with sourceType
    public int saveNews(int disasterId, String url, String title, String content, String date, String sourceType) {
        String insertSQL = "INSERT OR IGNORE INTO news(disaster_id, url, title, content, published_date, source_type) VALUES(?,?,?,?,?,?)";
        String selectSQL = "SELECT id FROM news WHERE url = ?";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setInt(1, disasterId);
                pstmt.setString(2, url);
                pstmt.setString(3, title);
                pstmt.setString(4, content);
                pstmt.setString(5, date);
                pstmt.setString(6, sourceType);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, url);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    // 1. Get or Create a Disaster ID
    public int getOrCreateDisaster(String disasterName) {
        String insertSQL = "INSERT OR IGNORE INTO disasters(name) VALUES(?)";
        String selectSQL = "SELECT id FROM disasters WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            // Try to insert
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, disasterName);
                pstmt.executeUpdate();
            }
            // Retrieve the ID
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, disasterName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 2. Save a News Article
    // 2. Save a News Article
    public int saveNews(int disasterId, String url, String title, String content, String date) {
        String insertSQL = "INSERT OR IGNORE INTO news(disaster_id, url, title, content, published_date) VALUES(?,?,?,?,?)";
        String selectSQL = "SELECT id FROM news WHERE url = ?";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            
            // Try to insert
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setInt(1, disasterId);
                pstmt.setString(2, url);
                pstmt.setString(3, title);
                pstmt.setString(4, content);
                pstmt.setString(5, date);
                pstmt.executeUpdate();
            }
            
            // Retrieve the ID (works whether it was just inserted or already existed)
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, url);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 3. Save a Comment
    public void saveComment(int newsId, String content, String author, String date) {
        String sql = "INSERT INTO comments(news_id, content, author, published_date) VALUES(?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newsId);
            pstmt.setString(2, content);
            pstmt.setString(3, author);
            pstmt.setString(4, date);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}