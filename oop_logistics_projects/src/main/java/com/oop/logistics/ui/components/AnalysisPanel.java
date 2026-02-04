package com.oop.logistics.ui.components;

import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.scene.control.ProgressBar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisPanel extends VBox {
    private final DisasterContext context;
    private final VBox chartContainer; // Dedicated area for charts
    private ComboBox<String> modelSelector;
    private ProgressBar progressBar;       // NEW
    private Label progressLabel;
    public AnalysisPanel(DisasterContext context) {
        this.context = context;
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        
        // Container for the chart to appear below buttons
        this.chartContainer = new VBox();
        this.chartContainer.setPadding(new Insets(10));
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        setupUI();
    }

    private void setupUI() {
        Label lblTitle = new Label("Analytics Dashboard");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        modelSelector = new ComboBox<>(FXCollections.observableArrayList(
            "AI Model (Accurate - Slower)", 
            "Keyword Search (Instant)"
        ));
        modelSelector.getSelectionModel().select(0);
        
        // --- PROGRESS BAR SETUP ---
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressLabel = new Label("0%");
        progressLabel.setStyle("-fx-font-weight: bold;");
        
        VBox progressBox = new VBox(5, new Label("Analysis Progress:"), new HBox(10, progressBar, progressLabel));
        progressBox.setPadding(new Insets(0, 0, 0, 20)); // Add some spacing
        // --------------------------

        Button btn1 = createBtn("1. Sentiment Trend", this::runProblem1);
        Button btn2 = createBtn("2. Damage Class", this::runProblem2);
        Button btn3 = createBtn("3. Relief Sentiment", this::runProblem3);
        Button btn4 = createBtn("4. Relief Needs Trend", this::runProblem4);

        HBox controls = new HBox(10, modelSelector, btn1, btn2, btn3, btn4);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Add progressBox to the main layout
        HBox topBar = new HBox(20, controls, progressBox);
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        this.getChildren().addAll(lblTitle, topBar, chartContainer);
    }

    // Helper to update progress from background thread
    private Button createBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefSize(180, 50);
        btn.setWrapText(true);
        btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }
    private void updateProgress(double p) {
        Platform.runLater(() -> {
            progressBar.setProgress(p);
            progressLabel.setText(String.format("%.0f%%", p * 100));
        });
    }
    // =================================================================================
    // PROBLEM 1: Sentiment Time Series
    // =================================================================================
    private void runProblem1() {
        if (checkData(true)) return;
        updateProgress(0); // Reset
        new Thread(() -> {
            try {
                String type = getModelType();
                context.setStatus("Processing Problem 1 (" + type + ")...", false);
                
                // Pass 'this::updateProgress'
                var data = context.getClient().getSentimentTimeSeries(
                    context.getTexts(), context.getDates(), type, this::updateProgress
                );
                
                Platform.runLater(() -> displaySentimentTimeSeries(data));
            } catch (Exception e) { context.setStatus("Error: " + e.getMessage(), true); }
        }).start();
    }
    
    private void displaySentimentTimeSeries(List<Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Count");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Public Sentiment Over Time");

        XYChart.Series<String, Number> pos = new XYChart.Series<>(); pos.setName("Positive");
        XYChart.Series<String, Number> neg = new XYChart.Series<>(); neg.setName("Negative");
        XYChart.Series<String, Number> neu = new XYChart.Series<>(); neu.setName("Neutral");

        for (Map<String, Object> point : data) {
            String d = (String) point.get("date");
            pos.getData().add(new XYChart.Data<>(d, ((Number) point.get("positive")).intValue()));
            neg.getData().add(new XYChart.Data<>(d, ((Number) point.get("negative")).intValue()));
            neu.getData().add(new XYChart.Data<>(d, ((Number) point.get("neutral")).intValue()));
        }
        chart.getData().addAll(pos, neg, neu);
        displayChart(chart);
    }

    // =================================================================================
    // PROBLEM 2: Damage Classification
    // =================================================================================
    private void displayDamageTypes(List<String> categories) {
        Map<String, Long> counts = new HashMap<>();
        for (String c : categories) counts.put(c, counts.getOrDefault(c, 0L) + 1);

        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Damage Category");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Count");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Damage Classification");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Incidents");
        counts.forEach((cat, cnt) -> series.getData().add(new XYChart.Data<>(cat, cnt)));
        
        chart.getData().add(series);
        displayChart(chart);
    }

    // =================================================================================
    // PROBLEM 3: Relief Sentiment
    // =================================================================================
    private void runProblem2() {
        if (checkData(false)) return;
        updateProgress(0);
        new Thread(() -> {
            try {
                String type = getModelType();
                context.setStatus("Processing Problem 2 (" + type + ")...", false);
                
                var data = context.getClient().getDamageClassification(
                    context.getTexts(), type, this::updateProgress
                );
                
                Platform.runLater(() -> displayDamageTypes(data));
            } catch (Exception ex) { context.setStatus("Error: " + ex.getMessage(), true); }
        }).start();
    }
    private void runProblem3() {
        if (checkData(false)) return;
        updateProgress(0);
        new Thread(() -> {
            try {
                String type = getModelType();
                context.setStatus("Processing Problem 3...", false);
                var data = context.getClient().getReliefSentiment(
                    context.getTexts(), type, this::updateProgress
                );
                Platform.runLater(() -> displayReliefSentiment(data));
            } catch (Exception ex) { context.setStatus("Error: " + ex.getMessage(), true); }
        }).start();
    }
    private void displayReliefSentiment(Map<String, Map<String, Double>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Relief Sector");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Mentions");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Relief Sentiment by Sector");

        XYChart.Series<String, Number> posSeries = new XYChart.Series<>(); posSeries.setName("Positive");
        XYChart.Series<String, Number> negSeries = new XYChart.Series<>(); negSeries.setName("Negative");

        data.forEach((cat, scores) -> {
            posSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("positive", 0.0)));
            negSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("negative", 0.0)));
        });

        chart.getData().addAll(negSeries, posSeries);
        displayChart(chart);
    }

    // =================================================================================
    // PROBLEM 4: Relief Time Series
    // =================================================================================
    private void runProblem4() {
        if (checkData(true)) return;
        updateProgress(0);
        new Thread(() -> {
            try {
                String type = getModelType();
                context.setStatus("Processing Problem 4...", false);
                var data = context.getClient().getReliefTimeSeries(
                    context.getTexts(), context.getDates(), type, this::updateProgress
                );
                Platform.runLater(() -> displayReliefTimeSeries(data));
            } catch (Exception ex) { context.setStatus("Error: " + ex.getMessage(), true); }
        }).start();
    }

    private void displayReliefTimeSeries(List<Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Positive Mentions");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Relief Effectiveness Trends");

        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();

        for (Map<String, Object> p : data) {
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

    // =================================================================================
    // Helpers
    // =================================================================================
    private boolean checkData(boolean requireDates) {
        if (context.getTexts().isEmpty()) {
            context.setStatus("⚠️ No data loaded! Please load or crawl data first.", true);
            return true;
        }
        if (requireDates && context.getDates().isEmpty()) {
            context.setStatus("⚠️ No dates available for time-series analysis.", true);
            return true;
        }
        return false;
    }
    private String getModelType() {
        String selection = modelSelector.getSelectionModel().getSelectedItem();
        return (selection != null && selection.contains("Keyword")) ? "keyword" : "ai";
    }
    private void displayChart(Chart chart) {
        chartContainer.getChildren().clear();
        // Allow chart to grow
        chart.setMinHeight(400); 
        chartContainer.getChildren().add(chart);
        context.setStatus("✅ Analysis complete.", false);
    }
}