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
    public AnalysisData getAnalysisData(String disasterName) {
        AnalysisData data = new AnalysisData();
        
        // This query gets all comments linked to a specific disaster name
        String sql = "SELECT c.content, c.published_date FROM comments c " +
                     "JOIN news n ON c.news_id = n.id " +
                     "JOIN disasters d ON n.disaster_id = d.id " +
                     "WHERE d.name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, disasterName);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String content = rs.getString("content");
                String date = rs.getString("published_date");
                
                // Only add if content isn't empty
                if (content != null && !content.trim().isEmpty()) {
                    data.texts.add(content);
                    data.dates.add(date != null ? date : "01/01/1970"); // Fallback for missing dates
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching data for analysis: " + e.getMessage());
        }
        
        return data;
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
    public int saveNews(int disasterId, String url, String title, String content, String date) {
        String sql = "INSERT OR IGNORE INTO news(disaster_id, url, title, content, published_date) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, disasterId);
            pstmt.setString(2, url);
            pstmt.setString(3, title);
            pstmt.setString(4, content);
            pstmt.setString(5, date);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            
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