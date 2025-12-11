package com.oop.logistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oop.logistics.analysis.PythonAnalysisClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DisasterFXApp extends Application {

    private PythonAnalysisClient client;
    private final List<String> rawTexts = new ArrayList<>();
    private final List<String> rawDates = new ArrayList<>();
    
    private BorderPane mainLayout;
    private Label statusLabel;
    private VBox sideMenu;

    // --- COLORS & STYLES ---
    private static final String SIDEBAR_COLOR = "#2c3e50";
    private static final String SIDEBAR_TEXT = "white";
    private static final String ACCENT_COLOR = "#3498db";
    private static final String BG_COLOR = "#ecf0f1";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize API Client
        client = new PythonAnalysisClient("http://localhost:8000");

        // 2. Setup Main Layout
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // --- TOP: Header ---
        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label appTitle = new Label("Disaster Logistics Intelligence");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        appTitle.setTextFill(Color.web(SIDEBAR_COLOR));
        
        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.GRAY);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(appTitle, spacer, statusLabel);
        mainLayout.setTop(header);

        // --- LEFT: Side Menu ---
        sideMenu = createSideMenu();
        mainLayout.setLeft(sideMenu);

        // --- CENTER: Content Area ---
        Label welcomeMsg = new Label("Select an analysis module from the left menu.");
        welcomeMsg.setFont(Font.font("Segoe UI", 16));
        welcomeMsg.setTextFill(Color.DARKGRAY);
        
        StackPane centerContainer = new StackPane(welcomeMsg);
        centerContainer.setPadding(new Insets(20));
        mainLayout.setCenter(centerContainer);

        // 3. Load Data
        loadData("D:\\JAVAProjects\\OOP_Logistics_project\\YagiComments.csv");

        // 4. Final Scene Setup
        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setTitle("Yagi Storm Analysis Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSideMenu() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20));
        menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: " + SIDEBAR_COLOR + ";");

        Label menuTitle = new Label("ANALYSIS MODULES");
        menuTitle.setTextFill(Color.web("#95a5a6"));
        menuTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        menuTitle.setPadding(new Insets(0, 0, 10, 5));

        Button btn1 = createStyledButton("Problem 1:\nSentiment Trends");
        Button btn2 = createStyledButton("Problem 2:\nDamage Types");
        Button btn3 = createStyledButton("Problem 3:\nRelief Satisfaction");
        Button btn4 = createStyledButton("Problem 4:\nRelief By Time");

        btn1.setOnAction(e -> safeRun(this::showProblem1));
        btn2.setOnAction(e -> safeRun(this::showProblem2));
        btn3.setOnAction(e -> safeRun(this::showProblem3));
        btn4.setOnAction(e -> safeRun(this::showProblem4));

        menu.getChildren().addAll(menuTitle, btn1, btn2, btn3, btn4);
        return menu;
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(50);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10));
        btn.setFont(Font.font("Segoe UI", 13));
        btn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + SIDEBAR_TEXT + "; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-cursor: hand;"
        );
        
        // Hover effects
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + ACCENT_COLOR + "; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + SIDEBAR_TEXT + "; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-cursor: hand;"
        ));
        
        return btn;
    }

    // --- DATA LOADING (Robust UTF-8) ---
    private void loadData(String csvPath) {
        // ... (Keep your existing robust loading logic here, just ensuring UTF-8)
        File file = new File(csvPath);
        if (!file.exists()) {
             file = new File("oop_logistics_projects/" + csvPath);
        }
        
        if (!file.exists()) {
            setStatus("Error: CSV not found!", true);
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            // ... (Copy the dynamic column detection logic from your previous successful version)
            String line;
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split(",");
            int textIdx = -1, dateIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().replace("\"", "").trim();
                if (h.equals("text") || h.equals("description") || h.equals("content") || h.equals("comment")) textIdx = i;
                if (h.equals("thời gian") || h.equals("date") || h.equals("timestamp")) dateIdx = i;
            }

            if (textIdx == -1 || dateIdx == -1) {
                setStatus("Error: CSV Columns missing (Need 'Text', 'Date')", true);
                return;
            }

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length > Math.max(textIdx, dateIdx)) {
                    String text = parts[textIdx].replace("\"", "").trim();
                    String date = parts[dateIdx].replace("\"", "").trim();
                    if (!text.isEmpty() && !date.isEmpty()) {
                        rawTexts.add(text);
                        rawDates.add(date);
                    }
                }
            }
            setStatus("Loaded " + rawTexts.size() + " records.", false);
        } catch (Exception e) {
            setStatus("Load Error: " + e.getMessage(), true);
        }
    }

    // --- CHARTS & ANALYSIS ---

    // --- PROBLEM 1: Sentiment Trends (Line Chart) ---
    private void showProblem1() {
        setStatus("Analyzing Problem 1 (Sentiment Trends)...", false);
        
        new Thread(() -> {
            try {
                // 1. Fetch Data (Now throws Exception if API fails)
                List<Map<String, Object>> data = client.getSentimentTimeSeries(rawTexts, rawDates);
                
                // 2. Safety Check
                if (data == null || data.isEmpty()) {
                    Platform.runLater(() -> setStatus("API returned no data.", true));
                    return;
                }

                // 3. Sort Data (Safe now because we handled errors)
                data.sort((p1, p2) -> {
                    String d1 = (String) p1.get("date");
                    String d2 = (String) p2.get("date");
                    if (d1 == null || d2 == null) return 0;
                    return parseDate(d1).compareTo(parseDate(d2));
                });

                Platform.runLater(() -> {
                    // ... (Existing chart creation code) ...
                    // Copy the chart creation logic from my previous answer here
                    // ...
                    
                    // (Simplified for brevity, make sure you keep your chart logic)
                    CategoryAxis xAxis = new CategoryAxis(); 
                    NumberAxis yAxis = new NumberAxis();
                    LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                    chart.setTitle("Sentiment Trends");
                    
                    XYChart.Series<String, Number> pos = new XYChart.Series<>(); pos.setName("Positive");
                    XYChart.Series<String, Number> neg = new XYChart.Series<>(); neg.setName("Negative");
                    XYChart.Series<String, Number> neu = new XYChart.Series<>(); neu.setName("Neutral");

                    for (Map<String, Object> point : data) {
                        String date = (String) point.get("date");
                        pos.getData().add(new XYChart.Data<>(date, ((Number) point.get("positive")).intValue()));
                        neg.getData().add(new XYChart.Data<>(date, ((Number) point.get("negative")).intValue()));
                        neu.getData().add(new XYChart.Data<>(date, ((Number) point.get("neutral")).intValue()));
                    }
                    
                    chart.getData().addAll(pos, neg, neu);
                    applyCustomColors(chart, pos, neg, neu);
                    displayChart(chart);
                    
                    setStatus("Analysis Complete.", false);
                });
            } catch (Exception e) {
                // This will now print the REAL error (e.g. "API Error 500")
                e.printStackTrace(); 
                Platform.runLater(() -> setStatus("API Error: " + e.getMessage(), true));
            }
        }).start();
    }

    // Helper to sort dates (dd/MM/yyyy or dd/MM/yy)
    private java.time.LocalDate parseDate(String dateStr) {
        try {
            java.time.format.DateTimeFormatter formatter;
            if (dateStr.length() > 8) { // Assuming d/M/yyyy or dd/MM/yyyy
                 formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy");
            } else { // d/M/yy
                 formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yy");
            }
            return java.time.LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return java.time.LocalDate.MIN; // Push invalid dates to start
        }
    }

    // Helper to style the lines like Matplotlib
    private void applyCustomColors(LineChart<String, Number> chart, 
                                   XYChart.Series<String, Number> pos, 
                                   XYChart.Series<String, Number> neg,
                                   XYChart.Series<String, Number> neu) {
        // We use style lookup which works after the scene is rendered
        pos.getNode().setStyle("-fx-stroke: green;");
        neg.getNode().setStyle("-fx-stroke: red;");
        neu.getNode().setStyle("-fx-stroke: gray;");
        
        for (XYChart.Data<String, Number> data : pos.getData()) {
            if(data.getNode() != null) data.getNode().setStyle("-fx-background-color: green, white;");
        }
        for (XYChart.Data<String, Number> data : neg.getData()) {
            if(data.getNode() != null) data.getNode().setStyle("-fx-background-color: red, white;");
        }
        for (XYChart.Data<String, Number> data : neu.getData()) {
            if(data.getNode() != null) data.getNode().setStyle("-fx-background-color: gray, white;");
        }
    }

    private void showProblem2() {
        setStatus("Analyzing Problem 2 (Damage Types)...", false);
        new Thread(() -> {
            try {
                List<String> classifications = client.getDamageClassification(rawTexts);
                Map<String, Long> counts = classifications.stream()
                        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis();
                    xAxis.setLabel("Damage Category");
                    NumberAxis yAxis = new NumberAxis();
                    yAxis.setLabel("Mentions");

                    BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
                    chart.setTitle("Most Common Types of Damage");
                    chart.setLegendVisible(false);

                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    counts.forEach((type, count) -> {
                        if(!type.equals("Other")) series.getData().add(new XYChart.Data<>(type, count));
                    });

                    chart.getData().add(series);
                    displayChart(chart);
                    setStatus("Analysis Complete.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("API Error: " + e.getMessage(), true));
            }
        }).start();
    }

    private void showProblem3() {
        setStatus("Analyzing Problem 3 (Relief Satisfaction)...", false);
        new Thread(() -> {
            try {
                Map<String, Map<String, Double>> data = client.getReliefSentiment(rawTexts);

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis();
                    xAxis.setLabel("Relief Item");
                    NumberAxis yAxis = new NumberAxis();
                    yAxis.setLabel("Sentiment Score");

                    StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
                    chart.setTitle("Public Satisfaction by Relief Item");

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

                    chart.getData().addAll(negSeries, posSeries);
                    displayChart(chart);
                    setStatus("Analysis Complete.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("API Error: " + e.getMessage(), true));
            }
        }).start();
    }

    private void showProblem4() {
        setStatus("Analyzing Problem 4 (Relief Trends)...", false);
        new Thread(() -> {
            try {
                List<Map<String, Object>> data = client.getReliefTimeSeries(rawTexts, rawDates);

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis();
                    xAxis.setLabel("Date");
                    NumberAxis yAxis = new NumberAxis();
                    yAxis.setLabel("Positive Mentions");

                    LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                    chart.setTitle("Effectiveness of Relief Efforts Over Time");

                    Map<String, XYChart.Series<String, Number>> seriesMap = new java.util.HashMap<>();

                    for (Map<String, Object> point : data) {
                        String cat = (String) point.get("category");
                        if (cat.equals("Other")) continue;

                        seriesMap.putIfAbsent(cat, new XYChart.Series<>());
                        seriesMap.get(cat).setName(cat);
                        seriesMap.get(cat).getData().add(new XYChart.Data<>(
                            (String) point.get("date"), 
                            (Number) point.get("positive")
                        ));
                    }

                    seriesMap.values().forEach(s -> chart.getData().add(s));
                    displayChart(chart);
                    setStatus("Analysis Complete.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("API Error: " + e.getMessage(), true));
            }
        }).start();
    }

    // --- HELPER METHODS ---

    private void displayChart(Chart chart) {
        // Wrap in VBox for padding and style
        VBox container = new VBox(chart);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-background-radius: 5;");
        
        // Ensure chart grows
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Add padding around the container inside center area
        StackPane wrapper = new StackPane(container);
        wrapper.setPadding(new Insets(20));
        
        mainLayout.setCenter(wrapper);
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(isError ? Color.RED : Color.web("#27ae60"));
    }

    private void safeRun(Runnable action) {
        if (rawTexts.isEmpty()) {
            setStatus("No data loaded. Check YagiComments.csv", true);
            return;
        }
        action.run();
    }
}