package com.oop.logistics.ui.components;

import com.oop.logistics.config.KeywordManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class KeywordContributionPanel {

    // Helper manager to handle reading/writing JSON
    private final KeywordManager helperManager = new KeywordManager();
    private final Runnable onBackAction;

    // UI Components - Declared here, Initialized in getView()
    private ComboBox<ConfigTarget> fileSelector;
    private ComboBox<String> categorySelector;
    private TextField txtKeyword;
    private Label statusLabel;

    // Enum to map readable names to physical file paths
    private enum ConfigTarget {
        DISASTERS("Disaster Types", "oop_logistics_projects/external config/disasters.json"),
        DAMAGE("Damage Keywords", "oop_logistics_projects/external config/damage_keywords.json"),
        RELIEF("Relief Keywords", "oop_logistics_projects/external config/relief_keywords.json"),
        SENTIMENT("Sentiment Keywords", "oop_logistics_projects/external config/sentiment_keywords.json");

        final String label;
        final String filePath;

        ConfigTarget(String label, String filePath) {
            this.label = label;
            this.filePath = filePath;
        }

        @Override
        public String toString() { return label; }
    }

    public KeywordContributionPanel(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    public Parent getView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: white;");

        // --- 1. Header ---
        Label title = new Label("Contribute Knowledge");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label subtitle = new Label("Select a configuration file to add new keywords to the system.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        // --- 2. Initialize Components (FIXES NULL POINTER EXCEPTION) ---
        fileSelector = new ComboBox<>();
        categorySelector = new ComboBox<>();
        txtKeyword = new TextField();
        statusLabel = new Label();

        // --- 3. Configure File Selector ---
        fileSelector.getItems().addAll(ConfigTarget.values());
        fileSelector.setPromptText("Select Configuration File");
        fileSelector.setMinWidth(350);
        fileSelector.setOnAction(e -> loadSelectedFile());

        // --- 4. Configure Category Selector ---
        categorySelector.setEditable(true); // Allow user to type new categories
        categorySelector.setPromptText("Select Existing or Type New Category");
        categorySelector.setMinWidth(350);

        // --- 5. Configure Input ---
        txtKeyword.setPromptText("Enter new keyword (e.g., 'ngập lụt', 'sập cầu')");
        txtKeyword.setMinWidth(350);

        // --- 6. Buttons ---
        Button btnSave = new Button("Save Keyword");
        btnSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnSave.setMinWidth(150);
        btnSave.setOnAction(e -> saveAction());

        Button btnBack = new Button("← Back to Menu");
        btnBack.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20;");
        btnBack.setMinWidth(150);
        btnBack.setOnAction(e -> {
            if (onBackAction != null) onBackAction.run();
        });

        // --- 7. Layout Assembly ---
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(400);
        form.getChildren().addAll(
            new Label("1. Target File:"), fileSelector,
            new Label("2. Category:"), categorySelector,
            new Label("3. Keyword:"), txtKeyword
        );

        layout.getChildren().addAll(
            title, subtitle, 
            new Separator(), 
            form, 
            btnSave, 
            statusLabel, 
            new Separator(), 
            btnBack
        );

        return layout;
    }

    private void loadSelectedFile() {
        ConfigTarget target = fileSelector.getValue();
        if (target == null) return;

        statusLabel.setText("Loading...");
        statusLabel.setStyle("-fx-text-fill: black;");

        try {
            // Load data using the manager
            helperManager.loadFromJson(target.filePath);
            
            // Populate category dropdown
            categorySelector.getItems().clear();
            categorySelector.getItems().addAll(helperManager.getCategories());
            
            statusLabel.setText("✅ Loaded " + helperManager.getAllKeywords().size() + " categories from " + target.label);
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (IOException e) {
            statusLabel.setText("❌ Error loading file: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    private void saveAction() {
        ConfigTarget target = fileSelector.getValue();
        String cat = categorySelector.getValue();
        String word = txtKeyword.getText();

        // Validation
        if (target == null) {
            statusLabel.setText("⚠ Please select a file first.");
            statusLabel.setStyle("-fx-text-fill: orange;");
            return;
        }
        if (cat == null || cat.trim().isEmpty()) {
            statusLabel.setText("⚠ Please select or type a category.");
            statusLabel.setStyle("-fx-text-fill: orange;");
            return;
        }
        if (word == null || word.trim().isEmpty()) {
            statusLabel.setText("⚠ Please enter a keyword.");
            statusLabel.setStyle("-fx-text-fill: orange;");
            return;
        }

        try {
            // Add to memory
            helperManager.addKeyword(cat.trim(), word.trim());
            
            // Save to disk
            helperManager.saveChanges();
            
            statusLabel.setText("✅ Saved '" + word + "' to category [" + cat + "]");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            // Update dropdown if it was a new category
            if (!categorySelector.getItems().contains(cat.trim())) {
                categorySelector.getItems().add(cat.trim());
                categorySelector.getSelectionModel().select(cat.trim());
            }
            
            txtKeyword.clear();
            
        } catch (IOException e) {
            statusLabel.setText("❌ Save failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
}