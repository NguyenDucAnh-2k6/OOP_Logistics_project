package com.oop.logistics.ui.controllers;

import com.oop.logistics.config.KeywordManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class KeywordContributionController {

    @FXML private ComboBox<ConfigTarget> fileSelector;
    @FXML private ComboBox<String> categorySelector;
    @FXML private TextField txtKeyword;
    @FXML private Label statusLabel;

    private MainController mainController;
    private final KeywordManager helperManager = new KeywordManager();

    // Enum now only holds the file name, not the hardcoded path
    public enum ConfigTarget {
        DAMAGE("Damage Keywords", "damage_keywords.json"),
        RELIEF("Relief Keywords", "relief_keywords.json"),
        SENTIMENT("Sentiment Keywords", "sentiment_keywords.json"),
        INTENT("Intent Keywords", "intent_keywords.json"),
        LOCATION("Location Keywords", "location_keywords.json"); 
        
        final String label;
        final String fileName;

        ConfigTarget(String label, String fileName) {
            this.label = label;
            this.fileName = fileName;
        }
        @Override public String toString() { return label; }
    }

    /**
     * Traverses up the directory tree to reliably locate the external config file.
     */
    private String resolveConfigPath(String fileName) {
        Path currentPath = Paths.get("").toAbsolutePath();
        while (currentPath != null) {
            Path directPath = currentPath.resolve("external config").resolve(fileName);
            if (Files.exists(directPath)) return directPath.toString();
            
            Path subfolderPath = currentPath.resolve("oop_logistics_projects").resolve("external config").resolve(fileName);
            if (Files.exists(subfolderPath)) return subfolderPath.toString();
            
            currentPath = currentPath.getParent();
        }
        return "external config/" + fileName;
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

        // Dynamically resolve the path here
        String actualPath = resolveConfigPath(target.fileName);

        try {
            helperManager.loadFromJson(actualPath);
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
            // Since loadFromJson caches the path inside KeywordManager, saveChanges() just works!
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
        if (mainController != null) {
            mainController.navigateToDataSourceSelection(false); 
        }
    }
}