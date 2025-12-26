package com.oop.logistics.ui.components;

import com.oop.logistics.crawler.FacebookCrawler;
import com.oop.logistics.crawler.NewsCrawler;
import com.oop.logistics.crawler.NewsCrawlerFactory;
import com.oop.logistics.preprocessing.DateExtract;
import com.oop.logistics.preprocessing.NewsPreprocess;
import com.oop.logistics.preprocessing.StripLevel;
import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class InputPanel extends VBox {
    private final DisasterContext context;
    private final TextField urlField = new TextField();
    private final TextField dateField = new TextField();
    private final TextField cUserField = new TextField();
    private final TextField xsField = new TextField();
    private final TextField frField = new TextField();

    public InputPanel(DisasterContext context) {
        this.context = context;
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        
        setupUI();
    }

    private void setupUI() {
        Label inputLabel = new Label("📝 Input");
        inputLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        urlField.setPromptText("Paste URL here...");
        dateField.setPromptText("Date (dd/mm/yyyy)");
        
        this.getChildren().addAll(inputLabel, new Label("URL:"), urlField);

        if ("Facebook".equals(context.getDataSource())) {
            this.getChildren().addAll(new Label("Date:"), dateField, new Label("Cookies (c_user, xs, fr):"), cUserField, xsField, frField);
        }

        Button crawlBtn = new Button("🔗 Crawl");
        crawlBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        crawlBtn.setOnAction(e -> performCrawl());

        Button processBtn = new Button("⚙️ Preprocess");
        processBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        processBtn.setOnAction(e -> performPreprocess());

        HBox actions = new HBox(10, crawlBtn, processBtn);
        this.getChildren().add(actions);
    }

    private void performCrawl() {
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

    private void performPreprocess() {
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