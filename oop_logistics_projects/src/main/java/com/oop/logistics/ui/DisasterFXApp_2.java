package com.oop.logistics.ui;

import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.crawler.FacebookCrawler;
import com.oop.logistics.crawler.FacebookCrawler.Comment;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DisasterFXApp_2 extends Application {

    private PythonAnalysisClient client;
    private FacebookCrawler crawler;
    private BorderPane mainLayout;
    private Label statusLabel;

    private final List<String> rawTexts = new ArrayList<>();
    private final List<String> rawDates = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        client = new PythonAnalysisClient("http://127.0.0.1:8000");

        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        HBox top = new HBox(8);
        top.setPadding(new Insets(8));
        top.setAlignment(Pos.CENTER_LEFT);

        TextField keywordField = new TextField();
        keywordField.setPromptText("Enter disaster keyword (e.g. yagi, matmo)");
        keywordField.setPrefWidth(300);

        TextField cUserField = new TextField();
        cUserField.setPromptText("c_user cookie");
        cUserField.setPrefWidth(220);

        TextField xsField = new TextField();
        xsField.setPromptText("xs cookie");
        xsField.setPrefWidth(220);

        TextField frField = new TextField();
        frField.setPromptText("fr cookie (optional)");
        frField.setPrefWidth(220);

        Button startBtn = new Button("Start Crawl & Analyze");
        startBtn.setOnAction(e -> {
            String kw = keywordField.getText().trim();
            if (kw.isEmpty()) { setStatus("Please enter a keyword", true); return; }
            startBtn.setDisable(true);
            new Thread(() -> runFullPipeline(kw, cUserField.getText().trim(), xsField.getText().trim(), frField.getText().trim(), startBtn)).start();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Ready");

        top.getChildren().addAll(keywordField, cUserField, xsField, frField, startBtn, spacer, statusLabel);
        mainLayout.setTop(top);

        StackPane center = new StackPane(new Label("Enter keyword and cookies, then click Start."));
        mainLayout.setCenter(center);

        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setTitle("DisasterFXApp v2 - Facebook crawler + Analysis");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void runFullPipeline(String keyword, String c_user, String xs, String fr, Button startBtn) {
        try {
            setStatus("Starting crawler...", false);
            crawler = new FacebookCrawler(true); // headless

            boolean logged = false;
            if (c_user != null && !c_user.isEmpty() && xs != null && !xs.isEmpty()) {
                logged = crawler.loginWithCookies(c_user, xs, fr);
            }

            if (!logged) {
                setStatus("Warning: login failed or skipped. Crawling may be limited.", true);
            } else {
                setStatus("Logged in. Searching posts...", false);
            }

            List<String> postUrls = searchPosts(keyword, 50);
            setStatus("Found " + postUrls.size() + " posts. Crawling comments...", false);

            // Save CSV (allow duplicates)
            File csv = new File("comments.csv");
            boolean needHeader = !csv.exists();
            try (FileWriter fw = new FileWriter(csv, true)) {
                if (needHeader) {
                    fw.append("date,text\n");
                }

                for (int i = 0; i < postUrls.size(); i++) {
                    String post = postUrls.get(i);
                    setStatus(String.format("Crawling post %d/%d", i+1, postUrls.size()), false);
                    List<Comment> comments = crawler.crawlComments(post);
                    for (Comment c : comments) {
                        fw.append(c.toCsvRow()).append('\n');
                        rawDates.add(c.getTimestamp() == null ? "" : c.getTimestamp());
                        rawTexts.add(c.getContent() == null ? "" : c.getContent());
                    }
                    fw.flush();
                }
            }

            setStatus("Crawling complete. Running analysis...", false);

            // Call backend analysis endpoints and display charts
            List<java.util.Map<String, Object>> sSeries = client.getSentimentTimeSeries(rawTexts, rawDates);
            java.util.List<String> damageCats = client.getDamageClassification(rawTexts);
            java.util.Map<String, java.util.Map<String, Double>> relief = client.getReliefSentiment(rawTexts);
            List<java.util.Map<String, Object>> reliefSeries = client.getReliefTimeSeries(rawTexts, rawDates);

            Platform.runLater(() -> {
                try {
                    displaySentimentTimeSeries(sSeries);
                    displayDamageTypes(damageCats);
                    displayReliefSentiment(relief);
                    displayReliefTimeSeries(reliefSeries);
                    setStatus("Analysis complete.", false);
                } catch (Exception ex) { setStatus("UI render error: " + ex.getMessage(), true); }
                startBtn.setDisable(false);
            });

        } catch (Exception ex) {
            setStatus("Error: " + ex.getMessage(), true);
            Platform.runLater(() -> startBtn.setDisable(false));
        } finally {
            if (crawler != null) crawler.close();
        }
    }

    private List<String> searchPosts(String keyword, int max) {
        List<String> results = new ArrayList<>();
        try {
            WebDriver driver = crawler.getDriver();
            String q = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String url = "https://www.facebook.com/search/top?q=" + q;
            driver.get(url);
            Thread.sleep(5000);

            // Scroll a bit to load results
            for (int i = 0; i < 6; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollBy(0,800)");
                Thread.sleep(1000);
            }

            Set<String> seen = new HashSet<>();
            List<WebElement> anchors = driver.findElements(By.xpath("//a[contains(@href,'/posts/') or contains(@href,'/permalink') or contains(@href,'/photo.php')]") );
            for (WebElement a : anchors) {
                try {
                    String href = a.getAttribute("href");
                    if (href == null) continue;
                    // Normalize
                    int qidx = href.indexOf('?');
                    if (qidx > 0) href = href.substring(0, qidx);
                    if (!seen.contains(href)) {
                        seen.add(href);
                        results.add(href);
                        if (results.size() >= max) break;
                    }
                } catch (Exception e) { continue; }
            }

        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
        }
        return results;
    }

    // --- UI chart helpers (minimal, reuse patterns from DisasterFXApp) ---
    private void displaySentimentTimeSeries(List<java.util.Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Count");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Public Sentiment Over Time");

        XYChart.Series<String, Number> pos = new XYChart.Series<>(); pos.setName("Positive");
        XYChart.Series<String, Number> neg = new XYChart.Series<>(); neg.setName("Negative");
        XYChart.Series<String, Number> neu = new XYChart.Series<>(); neu.setName("Neutral");

        for (java.util.Map<String, Object> point : data) {
            String d = (String) point.get("date");
            pos.getData().add(new XYChart.Data<>(d, ((Number) point.get("positive")).intValue()));
            neg.getData().add(new XYChart.Data<>(d, ((Number) point.get("negative")).intValue()));
            neu.getData().add(new XYChart.Data<>(d, ((Number) point.get("neutral")).intValue()));
        }

        chart.getData().addAll(pos, neg, neu);
        displayChart(chart);
    }

    private void displayDamageTypes(List<String> categories) {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        for (String c : categories) counts.put(c, counts.getOrDefault(c, 0L) + 1L);

        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Damage Category");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Reports");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Most Reported Damage Types");
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((cat, cnt) -> series.getData().add(new XYChart.Data<>(cat, cnt)));
        chart.getData().add(series);
        displayChart(chart);
    }

    private void displayReliefSentiment(java.util.Map<String, java.util.Map<String, Double>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Relief Sector");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Mentions");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Public Satisfaction by Sector");

        XYChart.Series<String, Number> posSeries = new XYChart.Series<>(); posSeries.setName("Positive");
        XYChart.Series<String, Number> negSeries = new XYChart.Series<>(); negSeries.setName("Negative");

        data.forEach((cat, scores) -> {
            posSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("positive", 0.0)));
            negSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("negative", 0.0)));
        });

        chart.getData().addAll(negSeries, posSeries);
        displayChart(chart);
    }

    private void displayReliefTimeSeries(List<java.util.Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Positive Mentions");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Relief Effectiveness Trends (Positive Sentiment)");

        java.util.Map<String, XYChart.Series<String, Number>> seriesMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> p : data) {
            String cat = (String) p.get("category");
            String date = (String) p.get("date");
            Number val = (Number) p.get("positive");
            if (cat == null || cat.equals("Other")) continue;
            seriesMap.putIfAbsent(cat, new XYChart.Series<>());
            seriesMap.get(cat).setName(cat);
            seriesMap.get(cat).getData().add(new XYChart.Data<>(date, val));
        }
        seriesMap.values().forEach(s -> chart.getData().add(s));
        displayChart(chart);
    }

    private void displayChart(javafx.scene.chart.Chart chart) {
        VBox container = new VBox(chart);
        container.setPadding(new Insets(10));
        mainLayout.setCenter(new StackPane(container));
    }

    private void setStatus(String msg, boolean error) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            if (error) statusLabel.setStyle("-fx-text-fill: red;"); else statusLabel.setStyle("-fx-text-fill: green;");
        });
    }

    public static void main(String[] args) { launch(args); }
}
