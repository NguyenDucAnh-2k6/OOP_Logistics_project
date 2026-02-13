package com.oop.logistics.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DataSourceController {
    private MainController mainController;
    private boolean isReadyMode;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    public void setReadyMode(boolean isReadyMode) {
        this.isReadyMode = isReadyMode;
    }

    @FXML
    private void selectFacebook() {
        mainController.getContext().setDataSource("Facebook");
        mainController.navigateToWorkflow(isReadyMode);
    }

    @FXML
    private void selectNews() {
        mainController.getContext().setDataSource("News");
        mainController.navigateToWorkflow(isReadyMode);
    }

    @FXML
    private void openKeywords() {
    // OLD: mainController.getContext().openKeywordContribution();
    // NEW: Use MainController navigation
    mainController.navigateToKeywordContribution();
    }
}