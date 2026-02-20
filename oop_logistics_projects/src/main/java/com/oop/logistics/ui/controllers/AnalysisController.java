package com.oop.logistics.ui.controllers;
import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.database.DataRepository;
import com.oop.logistics.ui.DisasterContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisController {

    @FXML private ComboBox<String> modelSelector;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private VBox chartContainer;

    private DisasterContext context;

    public void setContext(DisasterContext context) {
        this.context = context;
    }

    private void updateProgress(double p) {
        Platform.runLater(() -> {
            progressBar.setProgress(p);
            progressLabel.setText(String.format("%.0f%%", p * 100));
        });
    }

    private String getModelType() {
        String selection = modelSelector.getSelectionModel().getSelectedItem();
        return (selection != null && selection.contains("Keyword")) ? "keyword" : "ai";
    }

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
        chart.setMinHeight(400);
        chartContainer.getChildren().add(chart);
        context.setStatus("✅ Analysis complete.", false);
    }
    
    @FXML
    private void handleLoadDatabaseClick() {
        // We get the currently selected disaster from your context
        String currentDisaster = context.getDisasterName(); 
        
        if (currentDisaster != null && !currentDisaster.isEmpty()) {
            loadDataFromDatabase(currentDisaster);
        } else {
            // If they haven't searched/selected a disaster in the main UI yet
            context.setStatus("⚠️ Please search or select a disaster first.", true);
        }
    }

    private void loadDataFromDatabase(String disasterName) {
        context.setStatus("Fetching data from SQLite for: " + disasterName + "...", false);
        updateProgress(0.0); // Reset progress bar
        
        new Thread(() -> {
            DataRepository repo = new DataRepository();
            DataRepository.AnalysisData data = repo.getAnalysisData(disasterName);
            
            Platform.runLater(() -> {
                if (data.texts.isEmpty()) {
                    context.setStatus("⚠️ No data found in database for: " + disasterName, true);
                    context.getTexts().clear(); 
                    context.getDates().clear();
                } else {
                    context.setTexts(data.texts); 
                    context.setDates(data.dates);
                    context.setStatus("✅ Successfully loaded " + data.texts.size() + " items from Database.", false);
                    updateProgress(1.0); // Fill progress bar when done
                }
            });
        }).start();
    }
    // =================================================================================
    // PROBLEM 1: Sentiment Time Series
    // =================================================================================
    @FXML
    private void runProblem1() {
        if (checkData(true)) return;
        updateProgress(0);
        new Thread(() -> {
            try {
                String type = getModelType();
                context.setStatus("Processing Problem 1 (" + type + ")...", false);
                
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
    @FXML
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
    @FXML
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
    @FXML
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
}