package com.oop.logistics.ui.controllers;

import com.oop.logistics.ui.DisasterContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label statusLabel;
    
    // --- NEW: Link to FXML text field ---
    @FXML private TextField disasterField;

    private DisasterContext context;

    public void initializeContext() {
        this.context = new DisasterContext();
        this.context.setStatusLabel(statusLabel);
        loadModeSelection();
    }

    // --- NEW: Save disaster to context ---
    @FXML
    private void handleSetDisaster() {
        if (context != null && disasterField.getText() != null && !disasterField.getText().trim().isEmpty()) {
            context.setDisasterName(disasterField.getText().trim());
            context.setStatus("✅ Active Disaster: " + context.getDisasterName(), false);
        }
    }

    @FXML
    private void handleReset() {
        context.clearData();
        if (disasterField != null) {
            disasterField.clear();
        }
        loadModeSelection();
    }

    // --- Navigation Helpers ---

    private void loadModeSelection() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModeSelection.fxml"));
            Parent view = loader.load();
            
            ModeSelectionController ctrl = loader.getController();
            ctrl.setMainController(this); 
            
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
            FXMLLoader analysisLoader = new FXMLLoader(getClass().getResource("/fxml/AnalysisPanel.fxml"));
            Parent analysisView = analysisLoader.load();
            AnalysisController analysisCtrl = analysisLoader.getController();
            analysisCtrl.setContext(context);

            javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(20);
            
            if (!isReadyMode) {
                FXMLLoader inputLoader = new FXMLLoader(getClass().getResource("/fxml/InputPanel.fxml"));
                Parent inputView = inputLoader.load();
                InputController inputCtrl = inputLoader.getController();
                inputCtrl.setContext(context);
                layout.getChildren().add(inputView);
            } else {
                // Since you are using SQLite now, we no longer auto-load hardcoded CSVs here!
                context.setStatus("In Ready Mode: Please enter a disaster name above and click 'Load Database Data'.", false);
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