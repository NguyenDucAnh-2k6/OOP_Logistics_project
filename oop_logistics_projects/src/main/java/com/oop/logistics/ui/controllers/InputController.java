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
        String currentDisaster = context.getDisasterName();

        if (currentDisaster == null || currentDisaster.isEmpty()) {
            context.setStatus("⚠️ Please enter and 'Set' a disaster name in the top bar first!", true);
            return;
        }

        context.setStatus("Crawling: " + url + " for " + currentDisaster, false);
        
        new Thread(() -> {
            try {
                // 1. Initialize Database Repository
                DataRepository repo = new DataRepository();
                int disasterId = repo.getOrCreateDisaster(currentDisaster);

                if ("Facebook".equals(context.getDataSource())) {
                    FacebookCrawler fb = new FacebookCrawler();
                    if (!cUserField.getText().isEmpty()) {
                        fb.loginWithCookies(cUserField.getText(), xsField.getText(), frField.getText());
                    }
                    
                    String date = dateField.getText().isEmpty() ? DateExtract.getCurrentDateDDMMYYYY() : dateField.getText();
                    fb.setCrawlDate(date);
                    
                    // --- FACEBOOK SQLITE INTEGRATION ---
                    FacebookResult fbData = fb.crawlAndReturn(url); 
                    
                    if (fbData != null) {
                        // Save the main post to get a parent newsId
                        int newsId = repo.saveNews(disasterId, url, "Facebook Post", fbData.content, date, "Facebook");
                        
                        // Save all extracted comments linked to this post
                        if (newsId != -1 && fbData.comments != null) {
                            // UPDATED: Now loops through CommentData objects to get specific dates/authors
                            for (FacebookResult.CommentData comment : fbData.comments) {
                                repo.saveComment(newsId, comment.text, comment.author, comment.date);
                            }
                        }
                    }
                    fb.tearDown();

                } else {
                    // --- NEWS ARTICLE SQLITE INTEGRATION ---
                    NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
                    NewsResult article = crawler.crawl(url); 
                    
                    if (article != null) {
                        // Save the news article directly
                        String cleanDate = DateExtract.formatDateToDDMMYYYY(article.date);
                        repo.saveNews(disasterId, article.url, article.title, article.text, cleanDate, "News");
                    } else {
                        throw new RuntimeException("Crawler returned null. Page format might be unsupported.");
                    }
                }

                Platform.runLater(() -> {
                    context.setStatus("✅ Crawl complete! Data routed to DB for: " + currentDisaster, false);
                    urlField.clear();
                });
            } catch (Exception ex) { 
                Platform.runLater(() -> context.setStatus("❌ Crawl failed: " + ex.getMessage(), true)); 
            }
        }).start();
    }
    @FXML
    private void handlePreprocess() {
        // Preprocessing now just fetches clean data from SQLite
        String sourceType = context.getDataSource(); // <-- GET SOURCE TYPE
        context.setStatus("Fetching fresh " + sourceType + " data from database...", false);
        
        new Thread(() -> {
            try {
                String currentDisaster = context.getDisasterName();
                if (currentDisaster != null && !currentDisaster.isEmpty()) {
                    DataRepository repo = new DataRepository();
                    
                    // PASS THE SOURCE TYPE AS THE SECOND ARGUMENT
                    DataRepository.AnalysisData data = repo.getAnalysisData(currentDisaster, sourceType);
                    
                    Platform.runLater(() -> {
                        context.setTexts(data.texts);
                        context.setDates(data.dates);
                        context.setStatus("✅ " + sourceType + " Data loaded from DB! Ready to analyze.", false);
                    });
                } else {
                    Platform.runLater(() -> context.setStatus("⚠️ No active disaster set.", true));
                }
            } catch (Exception ex) { 
                Platform.runLater(() -> context.setStatus("❌ Error loading DB: " + ex.getMessage(), true)); 
            }
        }).start();
    }
}