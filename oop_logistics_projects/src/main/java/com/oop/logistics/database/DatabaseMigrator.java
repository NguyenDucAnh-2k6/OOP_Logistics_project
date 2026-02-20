package com.oop.logistics.database;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;

public class DatabaseMigrator {

    public static void migrateLegacyData() {
        System.out.println("🚀 Starting data migration to SQLite...");
        DataRepository repo = new DataRepository();
        
        // 1. Get or create the "Typhoon Yagi" disaster ID
        int disasterId = repo.getOrCreateDisaster("Typhoon Yagi");

        // 2. Migrate YagiNews_normalized.csv
        try (Reader in = new FileReader("YagiNews_normalized.csv")) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
            int count = 0;
            for (CSVRecord record : records) {
                if (record.size() >= 2) {
                    String date = record.get(0);
                    String text = record.get(1);
                    // Use a fake URL so the database unique constraint doesn't fail
                    String fakeUrl = "legacy_news_" + (++count); 
                    repo.saveNews(disasterId, fakeUrl, "Legacy Yagi News", text, date);
                }
            }
            System.out.println("✅ Successfully migrated " + count + " articles from YagiNews_normalized.csv");
        } catch (Exception e) {
            System.err.println("⚠️ Error migrating news (File might be missing or locked): " + e.getMessage());
        }

        // 3. Migrate YagiComments.csv
        try (Reader in = new FileReader("YagiComments.csv")) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
            
            // Comments need a parent "News" article to attach to in the database. 
            // We create a dummy Facebook Post for them.
            int dummyNewsId = repo.saveNews(disasterId, "legacy_fb_post", "Yagi Facebook Discussion", "Archived comments from YagiComments.csv", "07/09/2024");
            
            int count = 0;
            if (dummyNewsId != -1) {
                for (CSVRecord record : records) {
                    if (record.size() >= 2) {
                        String date = record.get(0);
                        String text = record.get(1);
                        repo.saveComment(dummyNewsId, text, "Legacy User", date);
                        count++;
                    }
                }
            }
            System.out.println("✅ Successfully migrated " + count + " comments from YagiComments.csv");
        } catch (Exception e) {
            System.err.println("⚠️ Error migrating comments: " + e.getMessage());
        }
        
        System.out.println("🎉 Migration complete! You can now use the UI to analyze Typhoon Yagi.");
    }
}