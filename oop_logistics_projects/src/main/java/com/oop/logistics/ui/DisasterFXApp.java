package com.oop.logistics.ui;

import com.oop.logistics.ui.components.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DisasterFXApp extends Application {

    private final DisasterContext context = new DisasterContext();
    private BorderPane root;
    private StackPane centerPane;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f4f8;");
        centerPane = new StackPane();

        // 1. Top Bar
        TopBar topBar = new TopBar(this::resetApp);
        context.setStatusLabel(topBar.getStatusLabel());
        root.setTop(topBar);

        // 2. Initial View
        showModeSelection();
        root.setCenter(centerPane);

        stage.setScene(new Scene(root, 1200, 800));
        stage.setTitle("Disaster Analysis Modular");
        stage.show();
    }

    private void resetApp() {
        context.clearData();
        showModeSelection();
    }

    private void showModeSelection() {
        centerPane.getChildren().setAll(new ModeSelectionView(isReadyMode -> {
            showDataSourceSelection(isReadyMode);
        }));
    }

    private void showDataSourceSelection(boolean isReadyMode) {
        // Simple VBox with buttons to choose "Facebook" or "News"
        // On click -> context.setDataSource("Facebook"); showMainWorkflow(isReadyMode);
    }
    
    private void showMainWorkflow(boolean isReadyMode) {
        VBox layout = new VBox(20);
        // Add InputPanel if !isReadyMode
        if (!isReadyMode) {
            layout.getChildren().add(new InputPanel(context));
        } else {
             // Load default file immediately if in Ready Mode
             String file = context.getDataSource().equals("Facebook") ? "YagiComments.csv" : "YagiNews_normalized.csv";
             context.loadCsvData(file);
        }
        
        // Always add Analysis Panel
        layout.getChildren().add(new AnalysisPanel(context));
        centerPane.getChildren().setAll(layout);
    }

    public static void main(String[] args) { launch(args); }
}