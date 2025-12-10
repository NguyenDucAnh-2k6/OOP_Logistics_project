package com.oop.logistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oop.logistics.analysis.PythonAnalysisClient;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DisasterFXApp extends Application {

    private PythonAnalysisClient client;
    private List<String> rawTexts = new ArrayList<>();
    private List<String> rawDates = new ArrayList<>();
    private BorderPane mainLayout;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize API Client (Port 8000 matches your main.py)
        client = new PythonAnalysisClient("http://localhost:8000"); 

        // 2. Load Data from your CSV file
        // Make sure "YagiComments.csv" is in the project root folder
        loadData("YagiComments.csv"); 

        // 3. Setup Layout
        mainLayout = new BorderPane();
        mainLayout.setLeft(createSideMenu());
        mainLayout.setCenter(new Label("Select a problem on the left to analyze Yagi Storm data."));

        Scene scene = new Scene(mainLayout, 1000, 600);
        primaryStage.setTitle("Disaster Logistics Intelligence Dashboard - Yagi Storm");
        primaryStage.setScene(scene);
        primaryStage.show();
        System.out.println("Current Working Directory: " + System.getProperty("user.dir"));
    }

    private void loadData(String csvPath) {
        rawTexts.clear();
        rawDates.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            String headerLine = br.readLine();
            
            if (headerLine == null) {
                System.err.println("CSV file is empty!");
                return;
            }

            // --- Dynamic Column Detection ---
            String[] headers = headerLine.split(",");
            int textIdx = -1;
            int dateIdx = -1;

            // Find columns by checking likely names (case-insensitive)
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().replace("\"", "").trim();
                // Check for Content/Text columns
                if (h.equals("text") || h.equals("description") || h.equals("content") || h.equals("comment")) textIdx = i;
                // Check for Time/Date columns
                if (h.equals("thời gian") || h.equals("date") || h.equals("timestamp") || h.equals("time")) dateIdx = i;
            }

            if (textIdx == -1 || dateIdx == -1) {
                System.err.println("ERROR: Could not find 'Text' or 'Date' columns in CSV header: " + headerLine);
                System.err.println("Please ensure your CSV has headers like 'Text' and 'Thời gian' or 'Date'.");
                return;
            }

            // --- Read Data Rows ---
            while ((line = br.readLine()) != null) {
                // Regex to split by comma ONLY if not inside quotes (handles CSVs with commas in text)
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (parts.length > Math.max(textIdx, dateIdx)) {
                    String text = parts[textIdx].replace("\"", "").trim();
                    String date = parts[dateIdx].replace("\"", "").trim();

                    // Clean Date: Keep only YYYY-MM-DD if user wants daily grouping
                    // If your Python API handles full timestamps, you can keep it as is.
                    // Here we ensure it's at least not empty.
                    if (!text.isEmpty() && !date.isEmpty()) {
                        rawTexts.add(text);
                        rawDates.add(date);
                    }
                }
            }
            System.out.println("✓ Successfully loaded " + rawTexts.size() + " records from " + csvPath);

        } catch (Exception e) {
            System.err.println("Error loading CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private VBox createSideMenu() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(15));
        menu.setStyle("-fx-background-color: #f0f0f0; -fx-pref-width: 200;");

        Label title = new Label("Analysis Modules");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Button btnProb1 = new Button("1. Sentiment Trends");
        Button btnProb2 = new Button("2. Damage Types");
        Button btnProb3 = new Button("3. Relief Satisfaction");
        Button btnProb4 = new Button("4. Relief By Time");

        // Styling
        String btnStyle = "-fx-pref-width: 180; -fx-alignment: BASELINE_LEFT;";
        btnProb1.setStyle(btnStyle);
        btnProb2.setStyle(btnStyle);
        btnProb3.setStyle(btnStyle);
        btnProb4.setStyle(btnStyle);

        // Actions
        btnProb1.setOnAction(e -> showProblem1());
        btnProb2.setOnAction(e -> showProblem2());
        btnProb3.setOnAction(e -> showProblem3());
        btnProb4.setOnAction(e -> showProblem4());

        menu.getChildren().addAll(title, btnProb1, btnProb2, btnProb3, btnProb4);
        return menu;
    }

    // --- PROBLEM 1: Sentiment Over Time (Line Chart) ---
    private void showProblem1() {
        // Call Python API
        List<Map<String, Object>> data = client.getSentimentTimeSeries(rawTexts, rawDates);

        // Create Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Public Sentiment Evolution (Problem 1)");

        XYChart.Series<String, Number> posSeries = new XYChart.Series<>();
        posSeries.setName("Positive");
        XYChart.Series<String, Number> negSeries = new XYChart.Series<>();
        negSeries.setName("Negative");

        for (Map<String, Object> point : data) {
            String date = (String) point.get("date");
            posSeries.getData().add(new XYChart.Data<>(date, ((Number) point.get("positive")).intValue()));
            negSeries.getData().add(new XYChart.Data<>(date, ((Number) point.get("negative")).intValue()));
        }

        lineChart.getData().addAll(posSeries, negSeries);
        mainLayout.setCenter(lineChart);
    }

    // --- PROBLEM 2: Damage Classification (Bar Chart) ---
    private void showProblem2() {
        List<String> classifications = client.getDamageClassification(rawTexts);

        // Aggregate locally since API returns raw list
        Map<String, Long> counts = classifications.stream()
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Damage Type");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Mentions");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Common Damage Types (Problem 2)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reports");

        counts.forEach((type, count) -> {
            if (!type.equals("Other")) { // Optional: Filter 'Other'
                series.getData().add(new XYChart.Data<>(type, count));
            }
        });

        barChart.getData().add(series);
        mainLayout.setCenter(barChart);
    }

    // --- PROBLEM 3: Relief Satisfaction (Stacked Bar Chart) ---
    private void showProblem3() {
        Map<String, Map<String, Double>> data = client.getReliefSentiment(rawTexts);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Relief Category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Sentiment Count");

        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Satisfaction by Relief Item (Problem 3)");

        XYChart.Series<String, Number> posSeries = new XYChart.Series<>();
        posSeries.setName("Positive");
        XYChart.Series<String, Number> negSeries = new XYChart.Series<>();
        negSeries.setName("Negative");

        data.forEach((category, sentiments) -> {
            if (!category.equals("Other")) {
                posSeries.getData().add(new XYChart.Data<>(category, sentiments.get("positive")));
                negSeries.getData().add(new XYChart.Data<>(category, sentiments.get("negative")));
            }
        });

        chart.getData().addAll(negSeries, posSeries); // Negative usually on bottom/left
        mainLayout.setCenter(chart);
    }

    // --- PROBLEM 4: Relief Trends Over Time (Multi-Line Chart) ---
    private void showProblem4() {
        List<Map<String, Object>> data = client.getReliefTimeSeries(rawTexts, rawDates);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Positive Sentiment");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Effectiveness of Relief Over Time (Positive Only) (Problem 4)");

        // Group data by Category to create Series
        Map<String, XYChart.Series<String, Number>> seriesMap = new java.util.HashMap<>();

        for (Map<String, Object> point : data) {
            String cat = (String) point.get("category");
            String date = (String) point.get("date");
            Number posVal = (Number) point.get("positive");

            if (cat.equals("Other")) continue;

            seriesMap.putIfAbsent(cat, new XYChart.Series<>());
            seriesMap.get(cat).setName(cat);
            seriesMap.get(cat).getData().add(new XYChart.Data<>(date, posVal));
        }

        seriesMap.values().forEach(s -> lineChart.getData().add(s));
        mainLayout.setCenter(lineChart);
    }
    public static void main(String[] args) {
        launch(args);
    }
}