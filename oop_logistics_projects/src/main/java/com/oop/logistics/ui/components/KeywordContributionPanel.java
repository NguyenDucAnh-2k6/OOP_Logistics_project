package com.oop.logistics.ui.components;

import com.oop.logistics.config.CategoryManager;
import com.oop.logistics.config.KeywordManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordContributionPanel {
    private final Stage stage;
    private final Map<String, String> configFiles = new HashMap<>();
    private final Map<String, KeywordManager> keywordManagers = new HashMap<>();
    private final Map<String, CategoryManager> categoryManagers = new HashMap<>();
    private ComboBox<String> configComboBox;
    private ComboBox<String> categoryComboBox;
    private TextArea keywordsTextArea;
    private Label statusLabel;
    private String currentSelectedConfig;

    public KeywordContributionPanel() {
        this.stage = new Stage();
        setupConfigFiles();
        setupUI();
    }

    private void setupConfigFiles() {
        // Define all available config files
        String baseDir = "external config/";
        configFiles.put("Damage Keywords", baseDir + "damage_keywords.json");
        configFiles.put("Disasters", baseDir + "disasters.json");
        configFiles.put("Relief Keywords", baseDir + "relief_keywords.json");
        configFiles.put("Sentiment Keywords", baseDir + "sentiment_keywords.json");
    }

    private void setupUI() {
        stage.setTitle("Add Personal Keywords");
        stage.setWidth(650);
        stage.setHeight(550);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f4f8;");

        // Title
        Label titleLabel = new Label("Contribute Keywords to Analysis");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Config File Selection
        VBox configSection = createConfigSection();

        // Category Selection
        VBox categorySection = createCategorySection();

        // Keywords Input
        VBox keywordsSection = createKeywordsSection();

        // Buttons
        HBox buttonBox = createButtonBox();

        // Status Label
        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");

        root.getChildren().addAll(
            titleLabel,
            new Separator(),
            configSection,
            categorySection,
            keywordsSection,
            buttonBox,
            statusLabel
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private VBox createConfigSection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Select Configuration File:");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        configComboBox = new ComboBox<>();
        configComboBox.setMinWidth(250);
        configComboBox.getItems().addAll(configFiles.keySet());
        configComboBox.getSelectionModel().selectFirst();
        
        // Load first config on startup
        if (!configComboBox.getItems().isEmpty()) {
            currentSelectedConfig = configComboBox.getValue();
            loadConfig(currentSelectedConfig);
        }
        
        // When user changes config file, reload categories
        configComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                currentSelectedConfig = newVal;
                loadConfig(newVal);
            }
        });
        
        section.getChildren().addAll(label, configComboBox);
        return section;
    }

    private VBox createCategorySection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Select Category:");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        categoryComboBox = new ComboBox<>();
        categoryComboBox.setMinWidth(250);
        
        section.getChildren().addAll(label, categoryComboBox);
        return section;
    }

    private VBox createKeywordsSection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Enter Keywords (one per line):");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        keywordsTextArea = new TextArea();
        keywordsTextArea.setWrapText(true);
        keywordsTextArea.setPrefRowCount(8);
        keywordsTextArea.setStyle("-fx-font-size: 11px; -fx-control-inner-background: #ecf0f1;");
        keywordsTextArea.setPromptText("Example:\nkeyword1\nkeyword2\nkeyword3");
        
        section.getChildren().addAll(label, keywordsTextArea);
        return section;
    }

    private HBox createButtonBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_RIGHT);
        
        Button submitBtn = new Button("Add Keywords");
        submitBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 20; -fx-base: #27ae60; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> addKeywords());
        
        Button cancelBtn = new Button("Close");
        cancelBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 20; -fx-base: #95a5a6; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> stage.close());
        
        box.getChildren().addAll(submitBtn, cancelBtn);
        return box;
    }

    private void loadConfig(String configName) {
        try {
            String configPath = configFiles.get(configName);
            System.out.println("Loading config: " + configName + " from " + configPath);
            
            // Check if file exists
            File f = resolveFile(configPath);
            if (f == null) {
                showError("Config file not found: " + configPath);
                categoryComboBox.getItems().clear();
                return;
            }
            
            configPath = f.getAbsolutePath();
            System.out.println("Resolved absolute path: " + configPath);
            
            // Create managers for this config if not already created
            if (!keywordManagers.containsKey(configName)) {
                KeywordManager km = new KeywordManager();
                CategoryManager cm = new CategoryManager();
                km.loadFromJson(configPath);
                cm.loadFromJson(configPath);
                keywordManagers.put(configName, km);
                categoryManagers.put(configName, cm);
            }
            
            // Update category combo box
            KeywordManager km = keywordManagers.get(configName);
            categoryComboBox.getItems().clear();
            categoryComboBox.getItems().addAll(km.getCategories());
            
            if (!categoryComboBox.getItems().isEmpty()) {
                categoryComboBox.getSelectionModel().selectFirst();
                showSuccess("Loaded " + km.getCategories().size() + " categories from " + configName);
            } else {
                showError("No categories found in " + configName);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load config: " + e.getMessage());
            categoryComboBox.getItems().clear();
        }
    }

    private File resolveFile(String filePath) {
        String[] possiblePaths = {
            filePath,
            "oop_logistics_projects/" + filePath,
            System.getProperty("user.dir") + "/" + filePath,
            System.getProperty("user.dir") + "/oop_logistics_projects/" + filePath
        };
        
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                System.out.println("Found file at: " + f.getAbsolutePath());
                return f;
            }
        }
        
        System.out.println("File not found in any of these locations:");
        for (String path : possiblePaths) {
            System.out.println("  - " + new File(path).getAbsolutePath());
        }
        
        return null;
    }

    private void addKeywords() {
        if (currentSelectedConfig == null || currentSelectedConfig.isEmpty()) {
            showError("Please select a configuration file");
            return;
        }
        
        String selectedCategory = categoryComboBox.getValue();
        String keywordsText = keywordsTextArea.getText().trim();
        
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            showError("Please select a category");
            return;
        }
        
        if (keywordsText.isEmpty()) {
            showError("Please enter at least one keyword");
            return;
        }
        
        try {
            String[] keywords = keywordsText.split("\n");
            List<String> addedKeywords = new ArrayList<>();
            
            KeywordManager km = keywordManagers.get(currentSelectedConfig);
            CategoryManager cm = categoryManagers.get(currentSelectedConfig);
            
            for (String keyword : keywords) {
                String trimmed = keyword.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    km.addKeyword(selectedCategory, trimmed);
                    cm.addKeywordToCategory(selectedCategory, trimmed);
                    addedKeywords.add(trimmed);
                }
            }
            
            if (!addedKeywords.isEmpty()) {
                km.saveChanges();
                cm.saveChanges();
                showSuccess("✅ Added " + addedKeywords.size() + " keyword(s) to " + selectedCategory);
                keywordsTextArea.clear();
            } else {
                showError("No valid keywords to add");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to save keywords: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
    }

    private void showError(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }

    public void show() {
        try {
            System.out.println("Opening KeywordContributionPanel...");
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error showing KeywordContributionPanel: " + e.getMessage());
        }
    }
}
