package com.oop.logistics.ui.controllers;

import javafx.fxml.FXML;

public class ModeSelectionController {
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleReadyData() {
        mainController.navigateToDataSourceSelection(true);
    }

    @FXML
    private void handleAddData() {
        mainController.navigateToDataSourceSelection(false);
    }
}