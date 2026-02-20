package com.oop.logistics;

import com.oop.logistics.ui.DisasterFXApp;
import com.oop.logistics.database.DatabaseManager;
import com.oop.logistics.database.DatabaseMigrator; // Import your new migrator

public class Launcher {
    public static void main(String[] args) {
        
        // 1. Initialize the database
        DatabaseManager.initializeDatabase(); 

        // 2. RUN THE MIGRATION (Delete or comment this out after you run the app once!)
        DatabaseMigrator.migrateLegacyData();

        // 3. Start the UI
        DisasterFXApp.main(args);
    }
}