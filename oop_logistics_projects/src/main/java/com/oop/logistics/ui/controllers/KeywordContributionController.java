package com.oop.logistics.ui.controllers;

import com.oop.logistics.config.KeywordManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.io.IOException;

public class KeywordContributionController {

    @FXML private ComboBox<ConfigTarget> fileSelector;
    @FXML private ComboBox<String> categorySelector;
    @FXML private TextField txtKeyword;
    @FXML private Label statusLabel;

    private MainController mainController;
    private final KeywordManager helperManager = new KeywordManager();

    // Enum to map readable names to physical file paths
    public enum ConfigTarget {
        DISASTERS("Disaster Types", "oop_logistics_projects/external config/disasters.json"),
        DAMAGE("Damage Keywords", "oop_logistics_projects/external config/damage_keywords.json"),
        RELIEF("Relief Keywords", "oop_logistics_projects/external config/relief_keywords.json"),
        SENTIMENT("Sentiment Keywords", "oop_logistics_projects/external config/sentiment_keywords.json"),
        INTENT("Intent Keywords", "oop_logistics_projects/external config/intent_keywords.json"); // <-- NEWLY ADDED
        final String label;
        final String filePath;

        ConfigTarget(String label, String filePath) {
            this.label = label;
            this.filePath = filePath;
        }
        @Override public String toString() { return label; }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        fileSelector.setItems(FXCollections.observableArrayList(ConfigTarget.values()));
    }

    @FXML
    private void loadSelectedFile() {
        ConfigTarget target = fileSelector.getValue();
        if (target == null) return;

        statusLabel.setText("Loading...");
        statusLabel.setStyle("-fx-text-fill: black;");

        try {
            helperManager.loadFromJson(target.filePath);
            categorySelector.getItems().clear();
            categorySelector.getItems().addAll(helperManager.getCategories());
            
            statusLabel.setText("✅ Loaded " + helperManager.getAllKeywords().size() + " categories from " + target.label);
            statusLabel.setStyle("-fx-text-fill: green;");
        } catch (IOException e) {
            statusLabel.setText("❌ Error loading file: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleSave() {
        ConfigTarget target = fileSelector.getValue();
        String cat = categorySelector.getValue();
        String word = txtKeyword.getText();

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
            helperManager.addKeyword(cat.trim(), word.trim());
            helperManager.saveChanges();
            
            statusLabel.setText("✅ Saved '" + word + "' to category [" + cat + "]");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            if (!categorySelector.getItems().contains(cat.trim())) {
                categorySelector.getItems().add(cat.trim());
                categorySelector.getSelectionModel().select(cat.trim());
            }
            txtKeyword.clear();
        } catch (IOException e) {
            statusLabel.setText("❌ Save failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleBack() {
        // Return to Data Source Selection (defaulting to isReadyMode=false or passing it if tracked)
        // Since this screen is usually accessed from "Add Data" flow, we can return there.
        // If you want precise state, you might need to pass `isReadyMode` into this controller too.
        if (mainController != null) {
            mainController.navigateToDataSourceSelection(false); 
        }
    }
}