package com.oop.logistics.ui.controllers;

import com.oop.logistics.crawler.FacebookCrawler;
import com.oop.logistics.crawler.FacebookResult;
import com.oop.logistics.crawler.NewsCrawler;
import com.oop.logistics.crawler.NewsCrawlerFactory;
import com.oop.logistics.crawler.NewsResult;
import com.oop.logistics.database.DataRepository;
import com.oop.logistics.preprocessing.DateExtract;
import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class InputController {
    // UPDATED: Now uses a TextArea for URLs
    @FXML private TextArea urlArea; 
    @FXML private TextField dateField, cUserField, xsField, frField;
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

    // NEW METHOD: Loads URLs found by the DisasterSearchService
    @FXML
    private void handleLoadSearchedUrls() {
        try {
            File file = new File("URL.csv");
            if (!file.exists()) {
                context.setStatus("⚠️ No URL.csv found. Please run a search first.", true);
                return;
            }
            StringBuilder urls = new StringBuilder();
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                String[] parts = line.split("\",\"");
                if (parts.length >= 2) {
                    // Extract the URL from the "date","url" CSV format
                    String u = parts[1].replace("\"", "");
                    urls.append(u).append("\n");
                }
            }
            urlArea.setText(urls.toString());
            context.setStatus("✅ Loaded URLs from search!", false);
        } catch (Exception e) {
            context.setStatus("❌ Failed to load URLs: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCrawl() {
        String rawUrls = urlArea.getText();
        if (rawUrls == null || rawUrls.trim().isEmpty()) {
            context.setStatus("⚠️ Please enter at least one URL to crawl.", true);
            return;
        }
        
        // Split text area by newlines to get individual URLs
        String[] urls = rawUrls.split("\\r?\\n");
        String currentDisaster = context.getDisasterName();

        if (currentDisaster == null || currentDisaster.isEmpty()) {
            context.setStatus("⚠️ Please enter and 'Set' a disaster name in the top bar first!", true);
            return;
        }

        context.setStatus("Preparing to crawl " + urls.length + " URLs for " + currentDisaster + "...", false);
        
        new Thread(() -> {
            try {
                DataRepository repo = new DataRepository();
                int disasterId = repo.getOrCreateDisaster(currentDisaster);
                int successCount = 0;

                if ("Facebook".equals(context.getDataSource())) {
                    FacebookCrawler fb = new FacebookCrawler();
                    // Login happens only ONCE before the loop
                    if (!cUserField.getText().isEmpty()) {
                        fb.loginWithCookies(cUserField.getText(), xsField.getText(), frField.getText());
                    }
                    
                    String date = dateField.getText().isEmpty() ? DateExtract.getCurrentDateDDMMYYYY() : dateField.getText();
                    fb.setCrawlDate(date);
                    
                    // Loop through all URLs
                    for (String url : urls) {
                        url = url.trim();
                        if (url.isEmpty()) continue;

                        final String currentUrl = url;
                        Platform.runLater(() -> context.setStatus("Crawling FB: " + currentUrl, false));
                        
                        FacebookResult fbData = fb.crawlAndReturn(url); 
                        if (fbData != null) {
                            int newsId = repo.saveNews(disasterId, url, "Facebook Post", fbData.content, date, "Facebook");
                            if (newsId != -1 && fbData.comments != null) {
                                for (Object obj : fbData.comments) {
                                    // Compatibility safety check for string vs structured object
                                    if (obj instanceof String) {
                                        repo.saveComment(newsId, (String) obj, "Facebook User", date);
                                    } else if (obj instanceof FacebookResult.CommentData) {
                                        FacebookResult.CommentData cd = (FacebookResult.CommentData) obj;
                                        repo.saveComment(newsId, cd.text, cd.author, cd.date);
                                    }
                                }
                            }
                            successCount++;
                        }
                    }
                    fb.tearDown();

                } else {
                    // Loop through all News URLs
                    for (String url : urls) {
                        url = url.trim();
                        if (url.isEmpty()) continue;

                        final String currentUrl = url;
                        Platform.runLater(() -> context.setStatus("Crawling News: " + currentUrl, false));

                        try {
                            NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
                            NewsResult article = crawler.crawl(url); 
                            
                            if (article != null) {
                                String cleanDate = DateExtract.formatDateToDDMMYYYY(article.date);
                                repo.saveNews(disasterId, article.url, article.title, article.text, cleanDate, "News");
                                successCount++;
                            }
                        } catch (Exception e) {
                            System.out.println("Skipped unsupported/failed URL: " + url);
                        }
                    }
                }

                final int finalCount = successCount;
                Platform.runLater(() -> {
                    context.setStatus("✅ Crawl complete! " + finalCount + " items saved to DB for: " + currentDisaster, false);
                    urlArea.clear();
                });
            } catch (Exception ex) { 
                Platform.runLater(() -> context.setStatus("❌ Crawl failed: " + ex.getMessage(), true)); 
            }
        }).start();
    }

    @FXML
    private void handlePreprocess() {
        String currentDisaster = context.getDisasterName();
        String sourceType = context.getDataSource();

        if (currentDisaster == null || currentDisaster.isEmpty()) {
            context.setStatus("⚠️ Please select a disaster first.", true);
            return;
        }

        context.setStatus("⚙️ Cleaning & Preprocessing " + sourceType + " data...", false);
        
        new Thread(() -> {
            try {
                // Run the DB cleaner, stop-word remover, and deduplicator
                com.oop.logistics.preprocessing.DatabasePreprocessor.preprocessDisasterData(currentDisaster, sourceType);
                
                Platform.runLater(() -> 
                    context.setStatus("✅ Preprocessing complete! Data is clean. Proceed to 'Load Database' in Analysis.", false)
                );
            } catch (Exception ex) { 
                Platform.runLater(() -> context.setStatus("❌ Preprocessing failed: " + ex.getMessage(), true)); 
            }
        }).start();
    }
}