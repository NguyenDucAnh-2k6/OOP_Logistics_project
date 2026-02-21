package com.oop.logistics.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // This will create a file named "logistics_data.db" in your project folder
    private static final String URL = "jdbc:sqlite:logistics_data.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        String createDisasterTable = "CREATE TABLE IF NOT EXISTS disasters (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "date_searched DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createNewsTable = "CREATE TABLE IF NOT EXISTS news (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "disaster_id INTEGER," +
                "url TEXT UNIQUE," +
                "title TEXT," +
                "content TEXT," +
                "published_date TEXT," +
                "source_type TEXT," + // <-- NEW COLUMN
                "FOREIGN KEY(disaster_id) REFERENCES disasters(id)" +
                ");";

        String createCommentsTable = "CREATE TABLE IF NOT EXISTS comments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "news_id INTEGER," +
                "content TEXT," +
                "author TEXT," +
                "published_date TEXT," +
                "FOREIGN KEY(news_id) REFERENCES news(id)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Create tables
            stmt.execute(createDisasterTable);
            stmt.execute(createNewsTable);
            stmt.execute(createCommentsTable);
            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
}