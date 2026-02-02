package com.oop.logistics.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ModeSelectionView extends VBox {

    public ModeSelectionView(Consumer<Boolean> onModeSelected) {
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(40));
        this.setSpacing(20);
        this.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label titleLabel = new Label("Choose Your Workflow");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);

        Button readyDataBtn = createModeButton("📁 Use Ready-Data", 
            "Analyze existing CSV files", () -> onModeSelected.accept(true));
        
        Button addDataBtn = createModeButton("➕ Add Data", 
            "Crawl new data from URLs", () -> onModeSelected.accept(false));

        buttonBox.getChildren().addAll(readyDataBtn, addDataBtn);
        this.getChildren().addAll(titleLabel, buttonBox);
    }

    private Button createModeButton(String title, String desc, Runnable action) {
        VBox btnBox = new VBox(8);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20));
        btnBox.setStyle("-fx-background-color: #3498db; -fx-border-color: #2980b9; -fx-border-radius: 8; -fx-cursor: hand;");
        btnBox.setPrefSize(250, 150);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #ecf0f1; -fx-wrap-text: true;");

        btnBox.getChildren().addAll(titleLbl, descLbl);
        Button btn = new Button();
        btn.setGraphic(btnBox);
        btn.setStyle("-fx-background-color: transparent;");
        btn.setOnAction(e -> action.run());
        return btn;
    }
}