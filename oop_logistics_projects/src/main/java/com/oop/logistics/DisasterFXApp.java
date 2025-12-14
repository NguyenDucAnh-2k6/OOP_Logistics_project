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

    // --- COLORS ---
    private static final String SIDEBAR_COLOR = "#2c3e50";
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

        // --- Header ---
        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label appTitle = new Label("Disaster Logistics Dashboard");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        appTitle.setTextFill(Color.web(SIDEBAR_COLOR));
        
        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.GRAY);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(appTitle, spacer, statusLabel);
        mainLayout.setTop(header);

        // --- Side Menu ---
        mainLayout.setLeft(createSideMenu());

        // --- Center Content ---
        Label welcomeMsg = new Label("Select a problem from the left menu to analyze data.");
        welcomeMsg.setFont(Font.font("Segoe UI", 16));
        welcomeMsg.setTextFill(Color.DARKGRAY);
        StackPane centerContainer = new StackPane(welcomeMsg);
        mainLayout.setCenter(centerContainer);

        // 3. Load Data
        loadData("D:\\JAVAProjects\\OOP_Logistics_project\\YagiComments.csv");

        // 4. Show Window
        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setTitle("Yagi Storm Analysis - JavaFX Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSideMenu() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20));
        menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: " + SIDEBAR_COLOR + ";");

        Label menuTitle = new Label("ANALYSIS MODULES");
        menuTitle.setTextFill(Color.web("#bdc3c7"));
        menuTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        Button btn1 = createStyledButton("Problem 1:\nSentiment Trends");
        Button btn2 = createStyledButton("Problem 2:\nDamage Types");
        Button btn3 = createStyledButton("Problem 3:\nRelief Satisfaction");
        Button btn4 = createStyledButton("Problem 4:\nRelief Effectiveness");

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
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #34495e; -fx-border-width: 0 0 1 0;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #34495e; -fx-border-width: 0 0 1 0;"));
        return btn;
    }

    // ============================================================================================
    //  DATA LOADING
    // ============================================================================================
    private void loadData(String csvPath) {
        rawTexts.clear();
        rawDates.clear();
        
        File file = new File(csvPath);
        if (!file.exists()) file = new File("oop_logistics_projects/" + csvPath);

        if (!file.exists()) {
            setStatus("Error: " + csvPath + " not found!", true);
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // Header
            if (line == null) return;
            
            String[] headers = line.split(",");
            int textIdx = -1, dateIdx = -1;
            
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().replace("\"", "").trim();
                if (h.contains("text") || h.contains("content")) textIdx = i;
                if (h.contains("date") || h.contains("thời gian")) dateIdx = i;
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

    // ============================================================================================
    //  PROBLEM 1: SENTIMENT TRENDS (Line Chart)
    // ============================================================================================
    private void showProblem1() {
        setStatus("Analyzing P1: Sentiment Trends...", false);
        new Thread(() -> {
            try {
                List<Map<String, Object>> data = client.getSentimentTimeSeries(rawTexts, rawDates);
                
                // Sort by date
                data.sort((p1, p2) -> parseDate((String)p1.get("date")).compareTo(parseDate((String)p2.get("date"))));

                Platform.runLater(() -> {
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
                    styleLines(pos, "green"); styleLines(neg, "red"); styleLines(neu, "gray");
                    displayChart(chart);
                    setStatus("P1 Analysis Complete.", false);
                });
            } catch (Exception e) { handleEx(e); }
        }).start();
    }

    // ============================================================================================
    //  PROBLEM 2: DAMAGE TYPES (Bar Chart)
    // ============================================================================================
    private void showProblem2() {
        setStatus("Analyzing P2: Damage Types...", false);
        new Thread(() -> {
            try {
                List<String> categories = client.getDamageClassification(rawTexts);
                
                // Process: Count frequencies
                Map<String, Long> counts = categories.stream()
                    .filter(c -> !c.equals("Other")) // Optional filter
                    .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Damage Category");
                    NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Reports");
                    BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
                    chart.setTitle("Most Reported Damage Types");
                    chart.setLegendVisible(false);

                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    counts.forEach((cat, count) -> series.getData().add(new XYChart.Data<>(cat, count)));
                    
                    chart.getData().add(series);
                    // Color the bars red
                    for(XYChart.Data<String,Number> data : series.getData()) {
                        data.getNode().setStyle("-fx-bar-fill: #e74c3c;");
                    }
                    
                    displayChart(chart);
                    setStatus("P2 Analysis Complete.", false);
                });
            } catch (Exception e) { handleEx(e); }
        }).start();
    }

    // ============================================================================================
    //  PROBLEM 3: RELIEF SATISFACTION (Stacked Bar Chart)
    // ============================================================================================
    private void showProblem3() {
        setStatus("Analyzing P3: Relief Satisfaction...", false);
        new Thread(() -> {
            try {
                Map<String, Map<String, Double>> data = client.getReliefSentiment(rawTexts);

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Relief Sector");
                    NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Mentions");
                    StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
                    chart.setTitle("Public Satisfaction by Sector");

                    XYChart.Series<String, Number> posSeries = new XYChart.Series<>(); posSeries.setName("Positive");
                    XYChart.Series<String, Number> negSeries = new XYChart.Series<>(); negSeries.setName("Negative");

                    data.forEach((cat, scores) -> {
                        if (!cat.equals("Other")) {
                            posSeries.getData().add(new XYChart.Data<>(cat, scores.get("positive")));
                            negSeries.getData().add(new XYChart.Data<>(cat, scores.get("negative")));
                        }
                    });

                    chart.getData().addAll(negSeries, posSeries); // Neg on bottom
                    // Style
                    for(XYChart.Data<String,Number> d : posSeries.getData()) d.getNode().setStyle("-fx-bar-fill: #2ecc71;");
                    for(XYChart.Data<String,Number> d : negSeries.getData()) d.getNode().setStyle("-fx-bar-fill: #e74c3c;");
                    
                    displayChart(chart);
                    setStatus("P3 Analysis Complete.", false);
                });
            } catch (Exception e) { handleEx(e); }
        }).start();
    }

    // ============================================================================================
    //  PROBLEM 4: RELIEF TRENDS (Multi-Line Chart)
    // ============================================================================================
    private void showProblem4() {
        setStatus("Analyzing P4: Relief Trends...", false);
        new Thread(() -> {
            try {
                List<Map<String, Object>> data = client.getReliefTimeSeries(rawTexts, rawDates);

                // Sort the raw list by date first
                data.sort((p1, p2) -> parseDate((String)p1.get("date")).compareTo(parseDate((String)p2.get("date"))));

                Platform.runLater(() -> {
                    CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Date");
                    NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Positive Mentions");
                    LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                    chart.setTitle("Relief Effectiveness Trends (Positive Sentiment)");

                    // Group by Category to create separate lines
                    Map<String, XYChart.Series<String, Number>> seriesMap = new java.util.HashMap<>();

                    for (Map<String, Object> point : data) {
                        String cat = (String) point.get("category");
                        String date = (String) point.get("date");
                        Number val = (Number) point.get("positive");

                        if (cat.equals("Other")) continue;

                        seriesMap.putIfAbsent(cat, new XYChart.Series<>());
                        seriesMap.get(cat).setName(cat);
                        seriesMap.get(cat).getData().add(new XYChart.Data<>(date, val));
                    }

                    seriesMap.values().forEach(s -> chart.getData().add(s));
                    displayChart(chart);
                    setStatus("P4 Analysis Complete.", false);
                });
            } catch (Exception e) { handleEx(e); }
        }).start();
    }

    // --- UTILITIES ---

    private void displayChart(Chart chart) {
        VBox container = new VBox(chart);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-background-radius: 5;");
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        StackPane wrapper = new StackPane(container);
        wrapper.setPadding(new Insets(20));
        mainLayout.setCenter(wrapper);
    }

    private void styleLines(XYChart.Series<String, Number> series, String color) {
        // Simple delay lookup since nodes aren't created until added to chart
        if (series.getNode() != null) series.getNode().setStyle("-fx-stroke: " + color + ";");
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) data.getNode().setStyle("-fx-background-color: " + color + ", white;");
        }
    }

    private java.time.LocalDate parseDate(String d) {
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(d.length() > 8 ? "d/M/yyyy" : "d/M/yy");
            return java.time.LocalDate.parse(d, f);
        } catch (Exception e) { return java.time.LocalDate.MIN; }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(error ? Color.RED : Color.web("#27ae60"));
        if(error) System.err.println(msg);
    }

    private void handleEx(Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> setStatus("API Error: " + e.getMessage(), true));
    }

    private void safeRun(Runnable r) {
        if (rawTexts.isEmpty()) setStatus("No data loaded.", true);
        else r.run();
    }
}