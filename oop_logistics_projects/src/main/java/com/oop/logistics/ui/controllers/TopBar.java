package com.oop.logistics.ui.controllers;

import com.oop.logistics.ui.DisasterContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class TopBar extends HBox {
    
    private Label statusLabel;
    private final TextField disasterField;
    private DisasterContext context;

    public TopBar(Runnable onReset) {
        this.setPadding(new Insets(8));
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");
        this.setSpacing(10);
        
        Label titleLabel = new Label("📊 Disaster Analysis Pipeline v2");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        // --- NEW: THE DISASTER TEXT FIELD AND SET BUTTON ---
        Label disasterLbl = new Label("Disaster:");
        disasterLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        disasterField = new TextField();
        disasterField.setPromptText("e.g. Typhoon Matmo");
        
        Button setDisasterBtn = new Button("Set");
        setDisasterBtn.setStyle("-fx-font-size: 11; -fx-padding: 5 10; -fx-background-color: #27ae60; -fx-text-fill: white;");
        setDisasterBtn.setOnAction(e -> {
            // When clicked, save the typed name into the context
            if (context != null && !disasterField.getText().trim().isEmpty()) {
                context.setDisasterName(disasterField.getText().trim());
                statusLabel.setText("Active Disaster: " + context.getDisasterName());
            }
        });
        // ---------------------------------------------------

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #ecf0f1;");

        Button resetBtn = new Button("← Reset");
        resetBtn.setStyle("-fx-font-size: 11; -fx-padding: 5 15;");
        resetBtn.setOnAction(e -> onReset.run());

        // We add the new label, field, and button to the UI here
        this.getChildren().addAll(titleLabel, disasterLbl, disasterField, setDisasterBtn, spacer, statusLabel, resetBtn);
    }

    public void setContext(DisasterContext context) {
        this.context = context;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }
}