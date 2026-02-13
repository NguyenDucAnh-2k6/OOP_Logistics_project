package com.oop.logistics.ui.controllers;

import com.oop.logistics.crawler.FacebookCrawler;
import com.oop.logistics.crawler.NewsCrawler;
import com.oop.logistics.crawler.NewsCrawlerFactory;
import com.oop.logistics.preprocessing.DateExtract;
import com.oop.logistics.preprocessing.NewsPreprocess;
import com.oop.logistics.preprocessing.StripLevel;
import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class InputController {
    @FXML private TextField urlField, dateField, cUserField, xsField, frField;
    @FXML private Label lblDate, lblCookies;
    @FXML private HBox cookieBox;

    private DisasterContext context;

    public void setContext(DisasterContext context) {
        this.context = context;
        updateVisibility();
    }

    private void updateVisibility() {
        boolean isFb = "Facebook".equals(context.getDataSource());
        lblDate.setVisible(isFb); lblDate.setManaged(isFb);
        dateField.setVisible(isFb); dateField.setManaged(isFb);
        lblCookies.setVisible(isFb); lblCookies.setManaged(isFb);
        cookieBox.setVisible(isFb); cookieBox.setManaged(isFb);
    }

    @FXML
    private void handleCrawl() {
        String url = urlField.getText();
        context.setStatus("Crawling: " + url, false);
        
        new Thread(() -> {
            try {
                if ("Facebook".equals(context.getDataSource())) {
                    FacebookCrawler fb = new FacebookCrawler();
                    if (!cUserField.getText().isEmpty()) 
                        fb.loginWithCookies(cUserField.getText(), xsField.getText(), frField.getText());
                    fb.setCrawlDate(dateField.getText().isEmpty() ? DateExtract.getCurrentDateDDMMYYYY() : dateField.getText());
                    fb.crawl(url);
                    fb.tearDown();
                } else {
                    NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
                    crawler.crawl(url);
                }
                context.setStatus("✅ Crawl complete!", false);
            } catch (Exception ex) { context.setStatus("❌ Crawl failed: " + ex.getMessage(), true); }
        }).start();
    }

    @FXML
    private void handlePreprocess() {
        context.setStatus("Preprocessing...", false);
        new Thread(() -> {
            try {
                if ("Facebook".equals(context.getDataSource())) {
                    StripLevel.processFile("YagiComments_fixed.csv");
                    Platform.runLater(() -> context.loadCsvData("YagiComments.csv"));
                } else {
                    NewsPreprocess.normalizeNewsDateColumn();
                    Platform.runLater(() -> context.loadCsvData("YagiNews_normalized.csv"));
                }
                context.setStatus("✅ Preprocessing complete! Ready to analyze.", false);
            } catch (Exception ex) { context.setStatus("❌ Error: " + ex.getMessage(), true); }
        }).start();
    }
}