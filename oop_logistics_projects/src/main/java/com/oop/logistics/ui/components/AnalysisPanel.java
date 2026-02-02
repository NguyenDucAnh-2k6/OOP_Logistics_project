package com.oop.logistics.ui.components;

import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisPanel extends VBox {
    private final DisasterContext context;
    private final VBox chartContainer; // Dedicated area for charts

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
        Label label = new Label("📊 Analysis");
        label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(10); 
        grid.setVgap(10);

        grid.add(createBtn("1️⃣ Sentiment Time Series", this::runProblem1), 0, 0);
        grid.add(createBtn("2️⃣ Damage Classification", this::runProblem2), 1, 0);
        grid.add(createBtn("3️⃣ Relief Sentiment", this::runProblem3), 0, 1);
        grid.add(createBtn("4️⃣ Relief Time Series", this::runProblem4), 1, 1);

        this.getChildren().addAll(label, grid, chartContainer);
    }

    private Button createBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefSize(180, 50);
        btn.setWrapText(true);
        btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // =================================================================================
    // PROBLEM 1: Sentiment Time Series
    // =================================================================================
    private void runProblem1() {
        if (checkData(true)) return; // Requires dates
        new Thread(() -> {
            try {
                context.setStatus("Running Sentiment Time Series...", false);
                var data = context.getClient().getSentimentTimeSeries(context.getTexts(), context.getDates());
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
    private void runProblem2() {
        if (checkData(false)) return; // Text only
        new Thread(() -> {
            try {
                context.setStatus("Running Damage Classification...", false);
                // Expecting List<String> of categories returned by Python
                List<String> data = context.getClient().getDamageClassification(context.getTexts());
                Platform.runLater(() -> displayDamageTypes(data));
            } catch (Exception ex) { 
                context.setStatus("Error: " + ex.getMessage(), true); 
            }
        }).start();
    }

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
    private void runProblem3() {
        if (checkData(false)) return; // Text only
        new Thread(() -> {
            try {
                context.setStatus("Running Relief Sentiment...", false);
                // Expecting Map<Category, Map<Sentiment, Score>>
                var data = context.getClient().getReliefSentiment(context.getTexts());
                Platform.runLater(() -> displayReliefSentiment(data));
            } catch (Exception ex) { 
                context.setStatus("Error: " + ex.getMessage(), true); 
            }
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
        if (checkData(true)) return; // Requires dates
        new Thread(() -> {
            try {
                context.setStatus("Running Relief Time Series...", false);
                // Expecting List of points with {category, date, positive_score}
                var data = context.getClient().getReliefTimeSeries(context.getTexts(), context.getDates());
                Platform.runLater(() -> displayReliefTimeSeries(data));
            } catch (Exception ex) { 
                context.setStatus("Error: " + ex.getMessage(), true); 
            }
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

    private void displayChart(Chart chart) {
        chartContainer.getChildren().clear();
        // Allow chart to grow
        chart.setMinHeight(400); 
        chartContainer.getChildren().add(chart);
        context.setStatus("✅ Analysis complete.", false);
    }
}