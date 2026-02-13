package com.oop.logistics.ui.controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class TopBar extends HBox {
    
    private final Label statusLabel;

    public TopBar(Runnable onReset) {
        this.setPadding(new Insets(8));
        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");
        this.setSpacing(10);

        Label titleLabel = new Label("📊 Disaster Analysis Pipeline v2");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #ecf0f1;");

        Button resetBtn = new Button("← Reset");
        resetBtn.setStyle("-fx-font-size: 11; -fx-padding: 5 15;");
        resetBtn.setOnAction(e -> onReset.run());

        this.getChildren().addAll(titleLabel, spacer, statusLabel, resetBtn);
    }

    public Label getStatusLabel() {
        return statusLabel;
    }
}