package com.oop.logistics.ui;
import java.io.IOException;

import com.oop.logistics.config.*;
import com.oop.logistics.ui.components.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
public class DisasterFXApp extends Application {

    private final DisasterContext context = new DisasterContext();
    private BorderPane root;
    private StackPane centerPane;
    @Override
    public void start(Stage stage) {
        //loadConfigurations();
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
        // 1. Create the container layout
        VBox selectionLayout = new VBox(20); // 20px spacing
        selectionLayout.setAlignment(Pos.CENTER);
        selectionLayout.setPadding(new Insets(40));

        // 2. Create UI Elements
        Label instructions = new Label("Select Data Source:");
        instructions.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button btnFacebook = new Button("Facebook Data");
        Button btnNews = new Button("News Data");
        Button btnContributeKeywords = new Button("Add Personal Keywords");

        // Style the buttons for better visibility
        String btnStyle = "-fx-font-size: 14px; -fx-padding: 10 20; -fx-base: #3498db; -fx-text-fill: white;";
        btnFacebook.setStyle(btnStyle);
        btnNews.setStyle(btnStyle);
        btnFacebook.setMinWidth(150);
        btnNews.setMinWidth(150);

        // Style the contribute button differently
        String contributeBtnStyle = "-fx-font-size: 14px; -fx-padding: 10 20; -fx-base: #e67e22; -fx-text-fill: white;";
        btnContributeKeywords.setStyle(contributeBtnStyle);
        btnContributeKeywords.setMinWidth(150);

        // 3. Add Actions
        // When clicked, set the source in 'context' and move to the next screen
        btnFacebook.setOnAction(e -> {
            context.setDataSource("Facebook");
            showMainWorkflow(isReadyMode);
        });

        btnNews.setOnAction(e -> {
            context.setDataSource("News");
            showMainWorkflow(isReadyMode);
        });

        btnContributeKeywords.setOnAction(e -> {
            context.openKeywordContribution();
        });

        // 4. Assemble
        selectionLayout.getChildren().addAll(
            instructions, 
            btnFacebook, 
            btnNews, 
            new Separator(),
            btnContributeKeywords
        );

        // 5. Update the Center Pane (Important!)
        // This replaces the previous view (ModeSelection) with this new buttons view
        centerPane.getChildren().setAll(selectionLayout);
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