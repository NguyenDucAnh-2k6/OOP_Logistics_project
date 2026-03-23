package com.oop.logistics.ui.controllers;

import javafx.fxml.FXML;

public class DataSourceController {
    private MainController mainController;
    private boolean isReadyMode;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    public void setReadyMode(boolean isReadyMode) {
        this.isReadyMode = isReadyMode;
    }

    // --- Existing Methods ---
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

    // --- New Methods for Social Platforms ---
    @FXML
    private void selectYoutube() {
        mainController.getContext().setDataSource("YouTube");
        mainController.navigateToWorkflow(isReadyMode);
    }

    @FXML
    private void selectTiktok() {
        mainController.getContext().setDataSource("TikTok");
        mainController.navigateToWorkflow(isReadyMode);
    }

    @FXML
    private void selectVoz() {
        mainController.getContext().setDataSource("Voz");
        mainController.navigateToWorkflow(isReadyMode);
    }

    @FXML
    private void selectReddit() {
        mainController.getContext().setDataSource("Reddit");
        mainController.navigateToWorkflow(isReadyMode);
    }
    @FXML
    private void selectTwitter() {
        mainController.getContext().setDataSource("Twitter");
        mainController.navigateToWorkflow(isReadyMode);
    }
    @FXML
    private void selectFacebookDebugMode() {
        mainController.getContext().setDataSource("Facebook-Debug");
        mainController.navigateToWorkflow(isReadyMode);
    }
    @FXML
    private void openKeywords() {
        mainController.navigateToKeywordContribution();
    }
}