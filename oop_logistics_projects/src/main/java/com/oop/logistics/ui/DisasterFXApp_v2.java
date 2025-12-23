package com.oop.logistics.ui;

import com.oop.logistics.analysis.PythonAnalysisClient;
import com.oop.logistics.gem_crawler.FacebookCrawler;
import com.oop.logistics.gem_crawler.NewsCrawlerFactory;
import com.oop.logistics.gem_crawler.NewsCrawler;
import com.oop.logistics.preprocessing.DateExtract;
import com.oop.logistics.preprocessing.StripLevel;
import com.oop.logistics.preprocessing.NewsPreprocess;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DisasterFXApp_v2 extends Application {

    private PythonAnalysisClient client;
    private BorderPane mainLayout;
    private Label statusLabel;
    private StackPane centerPane;

    // State
    private String currentMode = null; // "ready-data" or "add-data"
    private String dataSource = null;  // "Facebook" or "News"
    private final List<String> rawTexts = new ArrayList<>();
    private final List<String> rawDates = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        client = new PythonAnalysisClient("http://127.0.0.1:8000");
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: #f0f4f8;");

        // Top bar
        HBox topBar = createTopBar();
        mainLayout.setTop(topBar);

        // Center panel (will show mode selection or workflow)
        centerPane = new StackPane();
        showModeSelection();
        mainLayout.setCenter(centerPane);

        Scene scene = new Scene(mainLayout, 1400, 900);
        primaryStage.setTitle("Disaster Analysis Pipeline v2");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(8));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 2 0;");

        Label titleLabel = new Label("📊 Disaster Analysis Pipeline v2");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #ecf0f1;");

        Button resetBtn = new Button("← Reset");
        resetBtn.setStyle("-fx-font-size: 11; -fx-padding: 5 15;");
        resetBtn.setOnAction(e -> {
            currentMode = null;
            dataSource = null;
            rawTexts.clear();
            rawDates.clear();
            showModeSelection();
        });

        topBar.getChildren().addAll(titleLabel, spacer, statusLabel, resetBtn);
        return topBar;
    }

    private void showModeSelection() {
        VBox modePanel = new VBox(20);
        modePanel.setAlignment(Pos.CENTER);
        modePanel.setPadding(new Insets(40));
        modePanel.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label titleLabel = new Label("Choose Your Workflow");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox buttonBox = new HBox(40);
        buttonBox.setAlignment(Pos.CENTER);

        Button readyDataBtn = createModeButton("📁 Use Ready-Data", 
            "Analyze existing CSV files\n(YagiComments.csv or YagiNews_normalized.csv)",
            () -> showDataSourceSelection(true));

        Button addDataBtn = createModeButton("➕ Add Data",
            "Crawl new data from URLs\nthen preprocess and analyze",
            () -> showDataSourceSelection(false));

        buttonBox.getChildren().addAll(readyDataBtn, addDataBtn);
        modePanel.getChildren().addAll(titleLabel, buttonBox);

        centerPane.getChildren().clear();
        centerPane.getChildren().add(modePanel);
    }

    private Button createModeButton(String title, String desc, Runnable action) {
        VBox btnBox = new VBox(8);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20));
        btnBox.setStyle("-fx-background-color: #3498db; -fx-border-color: #2980b9; -fx-border-radius: 8; -fx-cursor: hand;");
        btnBox.setPrefSize(250, 150);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #ecf0f1; -fx-wrap-text: true;");

        btnBox.getChildren().addAll(titleLbl, descLbl);

        Button btn = new Button();
        btn.setGraphic(btnBox);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        btn.setOnAction(e -> action.run());

        return btn;
    }

    private void showDataSourceSelection(boolean isReady) {
        currentMode = isReady ? "ready-data" : "add-data";

        VBox sourcePanel = new VBox(20);
        sourcePanel.setAlignment(Pos.CENTER);
        sourcePanel.setPadding(new Insets(40));
        sourcePanel.setStyle("-fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("Select Data Source");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox buttonBox = new HBox(30);
        buttonBox.setAlignment(Pos.CENTER);

        Button fbBtn = createSourceButton("👤 Facebook", "FB comments from YagiComments.csv", 
            () -> selectDataSource("Facebook", isReady));
        Button newsBtn = createSourceButton("📰 News", "News articles from YagiNews_normalized.csv", 
            () -> selectDataSource("News", isReady));

        buttonBox.getChildren().addAll(fbBtn, newsBtn);
        sourcePanel.getChildren().addAll(titleLabel, buttonBox);

        centerPane.getChildren().clear();
        centerPane.getChildren().add(sourcePanel);
    }

    private Button createSourceButton(String title, String desc, Runnable action) {
        VBox btnBox = new VBox(8);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20));
        btnBox.setStyle("-fx-background-color: #9b59b6; -fx-border-color: #8e44ad; -fx-border-radius: 8;");
        btnBox.setPrefSize(200, 120);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #ecf0f1; -fx-wrap-text: true;");

        btnBox.getChildren().addAll(titleLbl, descLbl);

        Button btn = new Button();
        btn.setGraphic(btnBox);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        btn.setOnAction(e -> action.run());

        return btn;
    }

    private void selectDataSource(String source, boolean isReady) {
        dataSource = source;

        if (isReady) {
            showReadyDataAnalyze();
        } else {
            showAddDataWorkflow();
        }
    }

    private void showReadyDataAnalyze() {
        setStatus("Ready to analyze " + dataSource + " data", false);

        VBox workflowPanel = new VBox(15);
        workflowPanel.setPadding(new Insets(30));
        workflowPanel.setStyle("-fx-background-color: #ecf0f1;");

        HBox infoBox = new HBox(10);
        infoBox.setStyle("-fx-background-color: #3498db; -fx-padding: 15; -fx-border-radius: 5;");
        Label infoLabel = new Label("📊 Using " + dataSource + " Data | Analyze Mode Only");
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        infoBox.getChildren().add(infoLabel);

        VBox analysisPanel = createAnalysisPanel();

        workflowPanel.getChildren().addAll(infoBox, analysisPanel);

        // Load data for analysis
        String csvFile = dataSource.equals("Facebook") ? "YagiComments.csv" : "YagiNews_normalized.csv";
        loadCsvData(csvFile);

        centerPane.getChildren().clear();
        centerPane.getChildren().add(new ScrollPane(workflowPanel));
    }

    private void showAddDataWorkflow() {
        VBox workflowPanel = new VBox(15);
        workflowPanel.setPadding(new Insets(30));
        workflowPanel.setStyle("-fx-background-color: #ecf0f1;");

        HBox infoBox = new HBox(10);
        infoBox.setStyle("-fx-background-color: #e74c3c; -fx-padding: 15; -fx-border-radius: 5;");
        Label infoLabel = new Label("🔄 Add New " + dataSource + " Data | Crawl → Preprocess → Analyze");
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        infoBox.getChildren().add(infoLabel);

        VBox inputPanel = createInputPanel();
        VBox actionPanel = createActionPanel();
        VBox analysisPanel = createAnalysisPanel();

        workflowPanel.getChildren().addAll(infoBox, inputPanel, actionPanel, analysisPanel);

        centerPane.getChildren().clear();
        centerPane.getChildren().add(new ScrollPane(workflowPanel));
    }

    private VBox createInputPanel() {
        VBox inputPanel = new VBox(10);
        inputPanel.setPadding(new Insets(15));
        inputPanel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label inputLabel = new Label("📝 Input");
        inputLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField urlField = new TextField();
        urlField.setPromptText("Paste URL here...");
        urlField.setPrefHeight(40);

        TextField dateField = new TextField();
        dateField.setPromptText(dataSource.equals("Facebook") ? "Date (dd/mm/yyyy)" : "Date not needed for News");
        dateField.setPrefHeight(40);
        dateField.setDisable(dataSource.equals("News"));

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getChildren().addAll(
            new Label("URL:"), new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, urlField);

        if (dataSource.equals("Facebook")) {
            HBox dateBox = new HBox(10);
            dateBox.setAlignment(Pos.CENTER_LEFT);
            dateBox.getChildren().addAll(
                new Label("Date:"), new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, dateField);
            inputPanel.getChildren().addAll(inputLabel, inputBox, dateBox);
        } else {
            inputPanel.getChildren().addAll(inputLabel, inputBox);
        }

        // Store fields in a way we can retrieve them
        inputPanel.setUserData(new Object[] { urlField, dateField });

        return inputPanel;
    }

    private VBox createActionPanel() {
        VBox actionPanel = new VBox(10);
        actionPanel.setPadding(new Insets(15));
        actionPanel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label actionLabel = new Label("🎬 Actions");
        actionLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button crawlBtn = new Button("🔗 Crawl");
        crawlBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30; -fx-background-color: #3498db; -fx-text-fill: white;");
        crawlBtn.setPrefWidth(150);

        Button preprocessBtn = new Button("⚙️ Preprocess");
        preprocessBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30; -fx-background-color: #f39c12; -fx-text-fill: white;");
        preprocessBtn.setPrefWidth(150);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(crawlBtn, preprocessBtn);

        // Get input fields
        crawlBtn.setOnAction(e -> {
            Object[] userData = (Object[]) actionPanel.getParent().lookup("VBox[style*='input']").getUserData();
            if (userData != null) {
                TextField urlField = (TextField) userData[0];
                TextField dateField = (TextField) userData[1];
                performCrawl(urlField.getText(), dateField.getText());
            }
        });

        preprocessBtn.setOnAction(e -> performPreprocess());

        actionPanel.getChildren().addAll(actionLabel, buttonBox);

        return actionPanel;
    }

    private VBox createAnalysisPanel() {
        VBox analysisPanel = new VBox(10);
        analysisPanel.setPadding(new Insets(15));
        analysisPanel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label analysisLabel = new Label("📊 Analysis (4 Problems)");
        analysisLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button btn1 = createProblemButton("1️⃣ Sentiment Time Series", e -> runProblem1());
        Button btn2 = createProblemButton("2️⃣ Damage Classification", e -> runProblem2());
        Button btn3 = createProblemButton("3️⃣ Relief Sentiment", e -> runProblem3());
        Button btn4 = createProblemButton("4️⃣ Relief Time Series", e -> runProblem4());

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.add(btn1, 0, 0);
        gridPane.add(btn2, 1, 0);
        gridPane.add(btn3, 0, 1);
        gridPane.add(btn4, 1, 1);

        analysisPanel.getChildren().addAll(analysisLabel, gridPane);

        return analysisPanel;
    }

    private Button createProblemButton(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(label);
        btn.setStyle("-fx-font-size: 11; -fx-padding: 10; -fx-background-color: #27ae60; -fx-text-fill: white;");
        btn.setPrefSize(180, 50);
        btn.setWrapText(true);
        btn.setOnAction(handler);
        return btn;
    }

    private void performCrawl(String url, String date) {
        if (url.isEmpty()) {
            setStatus("Please enter a URL", true);
            return;
        }

        setStatus("Crawling: " + url, false);

        new Thread(() -> {
            try {
                if (dataSource.equals("Facebook")) {
                    FacebookCrawler fbCrawler = new FacebookCrawler();
                    fbCrawler.setCrawlDate(date.isEmpty() ? DateExtract.getCurrentDateDDMMYYYY() : date);
                    fbCrawler.crawl(url);
                    fbCrawler.tearDown();
                } else {
                    NewsCrawler newsCrawler = NewsCrawlerFactory.getCrawler(url);
                    newsCrawler.crawl(url);
                }
                Platform.runLater(() -> setStatus("✅ Crawl complete!", false));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("❌ Crawl failed: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void performPreprocess() {
        setStatus("Preprocessing...", false);

        new Thread(() -> {
            try {
                if (dataSource.equals("Facebook")) {
                    // For FB: apply DateExtract then StripLevel
                    setStatus("Applying DateExtract...", false);
                    // Assuming user will set the date range - for now use simple range
                    DateExtract.fillDateRange("YagiComments_fixed.csv", "YagiComments_fixed_dated.csv", 1, 999, DateExtract.getCurrentDateDDMMYYYY());
                    
                    setStatus("Applying StripLevel...", false);
                    StripLevel.processFile("YagiComments_fixed_dated.csv");
                } else {
                    // For News: apply NewsPreprocess
                    setStatus("Normalizing news dates...", false);
                    NewsPreprocess.normalizeNewsDateColumn();
                }
                Platform.runLater(() -> setStatus("✅ Preprocessing complete!", false));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("❌ Preprocessing failed: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void loadCsvData(String csvFile) {
        try {
            File f = new File(csvFile);
            if (!f.exists()) {
                setStatus("CSV file not found: " + csvFile, true);
                return;
            }

            rawTexts.clear();
            rawDates.clear();

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String header = br.readLine(); // Skip header
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    List<String> fields = parseCsvLine(line);
                    if (fields.size() >= 2) {
                        String date = fields.get(0).trim();
                        String text = fields.get(1).trim();
                        
                        // Normalize date to dd/mm/yyyy format for consistency
                        date = normalizeDateFormat(date);
                        
                        rawDates.add(date);
                        rawTexts.add(text);
                    }
                }
            }

            setStatus("✅ Loaded " + rawTexts.size() + " records from " + csvFile, false);
        } catch (Exception ex) {
            System.err.println("ERROR loading CSV: " + ex);
            ex.printStackTrace();
            setStatus("Error loading CSV: " + ex.getMessage(), true);
        }
    }
    
    /**
     * Normalize date strings to consistent dd/mm/yyyy format
     */
    private String normalizeDateFormat(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }
        
        dateStr = dateStr.trim();
        
        // Already in dd/mm/yyyy format (4 digit year)?
        if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return dateStr;
        }
        
        // Convert d/m/yyyy or dd/m/yyyy to dd/mm/yyyy
        if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = dateStr.split("/");
            String day = String.format("%02d", Integer.parseInt(parts[0]));
            String month = String.format("%02d", Integer.parseInt(parts[1]));
            return day + "/" + month + "/" + parts[2];
        }
        
        // If we can't parse it, return as-is
        return dateStr;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                String field = current.toString().trim();
                if (field.startsWith("\"") && field.endsWith("\"")) {
                    field = field.substring(1, field.length() - 1);
                }
                fields.add(field);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String field = current.toString().trim();
        if (field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1);
        }
        fields.add(field);

        return fields;
    }

    private void runProblem1() {
        if (rawTexts.isEmpty()) {
            setStatus("No data to analyze", true);
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Running Sentiment Time Series...", false);
                var data = client.getSentimentTimeSeries(rawTexts, rawDates);
                Platform.runLater(() -> displaySentimentTimeSeries(data));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void runProblem2() {
        if (rawTexts.isEmpty()) {
            setStatus("No data to analyze", true);
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Running Damage Classification...", false);
                var data = client.getDamageClassification(rawTexts);
                Platform.runLater(() -> displayDamageTypes(data));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void runProblem3() {
        if (rawTexts.isEmpty()) {
            setStatus("No data to analyze", true);
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Running Relief Sentiment...", false);
                var data = client.getReliefSentiment(rawTexts);
                Platform.runLater(() -> displayReliefSentiment(data));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void runProblem4() {
        if (rawTexts.isEmpty() || rawDates.isEmpty()) {
            setStatus("No data to analyze", true);
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Running Relief Time Series...", false);
                var data = client.getReliefTimeSeries(rawTexts, rawDates);
                Platform.runLater(() -> displayReliefTimeSeries(data));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Error: " + ex.getMessage(), true));
            }
        }).start();
    }

    private void displaySentimentTimeSeries(List<java.util.Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Public Sentiment Over Time");
        chart.setStyle("-fx-background-color: #ecf0f1;");

        XYChart.Series<String, Number> pos = new XYChart.Series<>();
        pos.setName("Positive");
        XYChart.Series<String, Number> neg = new XYChart.Series<>();
        neg.setName("Negative");
        XYChart.Series<String, Number> neu = new XYChart.Series<>();
        neu.setName("Neutral");

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
        for (String c : categories) {
            counts.put(c, counts.getOrDefault(c, 0L) + 1);
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Damage Category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Damage Classification");
        chart.setStyle("-fx-background-color: #ecf0f1;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((cat, cnt) -> series.getData().add(new XYChart.Data<>(cat, cnt)));
        chart.getData().add(series);
        displayChart(chart);
    }

    private void displayReliefSentiment(java.util.Map<String, java.util.Map<String, Double>> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Relief Sector");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Mentions");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Relief Sentiment by Sector");
        chart.setStyle("-fx-background-color: #ecf0f1;");

        XYChart.Series<String, Number> posSeries = new XYChart.Series<>();
        posSeries.setName("Positive");
        XYChart.Series<String, Number> negSeries = new XYChart.Series<>();
        negSeries.setName("Negative");

        data.forEach((cat, scores) -> {
            posSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("positive", 0.0)));
            negSeries.getData().add(new XYChart.Data<>(cat, scores.getOrDefault("negative", 0.0)));
        });

        chart.getData().addAll(negSeries, posSeries);
        displayChart(chart);
    }

    private void displayReliefTimeSeries(List<java.util.Map<String, Object>> data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Positive Mentions");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Relief Effectiveness Trends");
        chart.setStyle("-fx-background-color: #ecf0f1;");

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
        VBox.setVgrow(chart, Priority.ALWAYS);
        centerPane.getChildren().clear();
        centerPane.getChildren().add(container);
    }

    private void setStatus(String msg, boolean error) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            if (error) {
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #ecf0f1;");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
