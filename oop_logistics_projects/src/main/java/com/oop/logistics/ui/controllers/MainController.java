package com.oop.logistics.ui.controllers;

import com.oop.logistics.ui.DisasterContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label statusLabel;

    private DisasterContext context;

    public void initializeContext() {
        this.context = new DisasterContext();
        this.context.setStatusLabel(statusLabel);
        loadModeSelection();
    }

    @FXML
    private void handleReset() {
        context.clearData();
        loadModeSelection();
    }

    // --- Navigation Helpers ---

    private void loadModeSelection() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModeSelection.fxml"));
            Parent view = loader.load();
            
            ModeSelectionController ctrl = loader.getController();
            ctrl.setMainController(this); // Pass reference back to main for navigation
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void navigateToDataSourceSelection(boolean isReadyMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DataSourceSelection.fxml"));
            Parent view = loader.load();
            
            DataSourceController ctrl = loader.getController();
            ctrl.setMainController(this);
            ctrl.setReadyMode(isReadyMode);
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void navigateToWorkflow(boolean isReadyMode) {
        try {
            // Load Analysis View (which contains Input + Charts)
            // Or create a wrapper. For simplicity, let's load a VBox containing Input (optional) + Analysis
            FXMLLoader analysisLoader = new FXMLLoader(getClass().getResource("/fxml/AnalysisPanel.fxml"));
            Parent analysisView = analysisLoader.load();
            AnalysisController analysisCtrl = analysisLoader.getController();
            analysisCtrl.setContext(context);

            javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(20);
            
            // If NOT ready mode, add Input Panel at the top
            if (!isReadyMode) {
                FXMLLoader inputLoader = new FXMLLoader(getClass().getResource("/fxml/InputPanel.fxml"));
                Parent inputView = inputLoader.load();
                InputController inputCtrl = inputLoader.getController();
                inputCtrl.setContext(context);
                layout.getChildren().add(inputView);
            } else {
                // Auto-load default data for Ready Mode
                String file = context.getDataSource().equals("Facebook") ? "YagiComments.csv" : "YagiNews_normalized.csv";
                context.loadCsvData(file);
            }

            layout.getChildren().add(analysisView);
            contentArea.getChildren().setAll(layout);

        } catch (IOException e) { e.printStackTrace(); }
    }
    public void navigateToKeywordContribution() {
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/KeywordContribution.fxml"));
        Parent view = loader.load();
        
        KeywordContributionController ctrl = loader.getController();
        ctrl.setMainController(this);
        
        contentArea.getChildren().setAll(view);
        } catch (IOException e) { 
        e.printStackTrace(); 
        }   
    }    
    public DisasterContext getContext() { return context; }
}