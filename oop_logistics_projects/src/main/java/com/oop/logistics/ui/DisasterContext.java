package com.oop.logistics.ui;

import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.preprocessing.DateExtract;
import com.oop.logistics.ui.components.KeywordContributionPanel;
import javafx.application.Platform;
import javafx.scene.control.Label;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DisasterContext {
    private final PythonAnalysisClient client;
    private final List<String> rawTexts = new ArrayList<>();
    private final List<String> rawDates = new ArrayList<>();
    private String dataSource;
    private Label statusLabel;
    private Consumer<Runnable> uiCallback; // To run actions on Main Thread
    private String currentKeywordConfigPath; // Track which config file to use

    public DisasterContext() {
        this.client = new PythonAnalysisClient("http://127.0.0.1:8000");
    }

    public void setStatusLabel(Label label) { this.statusLabel = label; }
    
    public void setStatus(String msg, boolean error) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(msg);
                statusLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #ecf0f1;");
            }
        });
    }

    public void setDataSource(String source) { this.dataSource = source; }
    public String getDataSource() { return dataSource; }
    public List<String> getTexts() { return rawTexts; }
    public List<String> getDates() { return rawDates; }
    public PythonAnalysisClient getClient() { return client; }

    public void clearData() {
        rawTexts.clear();
        rawDates.clear();
        dataSource = null;
    }

    public void loadCsvData(String csvFile) {
        new Thread(() -> {
            try {
                File f = new File(csvFile);
                if (!f.exists()) { setStatus("CSV file not found: " + csvFile, true); return; }

                synchronized (rawTexts) {
                    rawTexts.clear();
                    rawDates.clear();
                    
                    // Robust CSV Reading Logic
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line;
                        StringBuilder sb = new StringBuilder();
                        br.readLine(); // Skip header

                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            if (countQuotes(sb.toString()) % 2 == 0) {
                                List<String> fields = parseCsvLine(sb.toString());
                                if (fields.size() >= 2) {
                                    rawDates.add(normalizeDateFormat(fields.get(0).trim()));
                                    rawTexts.add(fields.get(1).trim());
                                }
                                sb.setLength(0);
                            } else {
                                sb.append("\n");
                            }
                        }
                    }
                }
                setStatus("✅ Loaded " + rawTexts.size() + " records from " + csvFile, false);
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error loading CSV: " + ex.getMessage(), true);
            }
        }).start();
    }

    // --- Helper Methods (Moved from App) ---
    private int countQuotes(String s) {
        int count = 0;
        for (char c : s.toCharArray()) if (c == '"') count++;
        return count;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private String normalizeDateFormat(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return dateStr;
        if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = dateStr.split("/");
            return String.format("%02d/%02d/%s", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), parts[2]);
        }
        return dateStr;
    }

    // --- Keyword Contribution Feature ---
    public void openKeywordContribution() {
        try {
            KeywordContributionPanel panel = new KeywordContributionPanel();
            panel.show();
        } catch (Exception e) {
            setStatus("Error opening keyword contribution: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
}