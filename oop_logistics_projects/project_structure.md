# Project Structure

```
disaster-logistics-system/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── oop/
│   │   │           └── logistics/
│   │   │               │
│   │   │               ├── DisasterLogisticsApp.java (Main Entry Point)
│   │   │               │
│   │   │               ├── core/                      (Core Interfaces & Pipeline)
│   │   │               │   ├── DataSource.java
│   │   │               │   ├── SourceConfiguration.java
│   │   │               │   ├── DataCollector.java
│   │   │               │   └── DisasterLogisticsPipeline.java
│   │   │               │
│   │   │               ├── datasources/               (Data Source Implementations)
│   │   │               │   ├── FacebookDataSource.java
│   │   │               │   ├── TwitterDataSource.java     (TODO)
│   │   │               │   └── NewsAPIDataSource.java     (TODO)
│   │   │               │
│   │   │               ├── Facebook/                  (Facebook Integration)
│   │   │               │   ├── FacebookClient.java
│   │   │               │   ├── FacebookPost.java
│   │   │               │   └── FacebookService.java
│   │   │               │
│   │   │               ├── models/                    (Data Models)
│   │   │               │   ├── DisasterEvent.java
│   │   │               │   ├── AnalysisRequest.java
│   │   │               │   ├── AnalysisResponse.java
│   │   │               │   ├── DisasterAnalysisReport.java
│   │   │               │   ├── TimelineDataPoint.java
│   │   │               │   ├── SentimentAnalysisResult.java
│   │   │               │   └── LocationAnalysis.java
│   │   │               │
│   │   │               ├── preprocessing/             (Data Preprocessing)
│   │   │               │   ├── DataPreprocessor.java
│   │   │               │   └── LocationExtractor.java
│   │   │               │
│   │   │               ├── config/                    (Configuration Management)
│   │   │               │   ├── KeywordManager.java
│   │   │               │   └── CategoryManager.java
│   │   │               │
│   │   │               ├── analysis/                  (Analysis Modules)
│   │   │               │   ├── AnalysisAPI.java
│   │   │               │   ├── PythonAnalysisClient.java
│   │   │               │   ├── DisasterAnalyzer.java
│   │   │               │   └── JavaSentimentAnalyzer.java  (Optional)
│   │   │            
│   │   │               ├── ui/                        (User Interface)
│   │   │               │   ├── DashboardFrame.java
│   │   │               │   ├── EventDetailsDialog.java     (TODO)
│   │   │               │   ├── ConfigurationDialog.java    (TODO)
│   │   │               │   └── ChartPanel.java            (TODO)
│   │   │               │
│   │   │               └── utils/                     (Utilities)
│   │   │                   ├── HttpClientUtil.java
│   │   │                   ├── JsonUtil.java              (TODO)
│   │   │                   └── DateTimeUtil.java          (TODO)
│   │   │
│   │   └── resources/
│   │       ├── keywords.json                  (Default keywords)
│   │       ├── categories.json                (Default categories)
│   │       ├── logback.xml                    (Logging configuration)
│   │       └── application.properties         (App configuration)
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── oop/
│                   └── logistics/
│                       ├── core/
│                       │   ├── DataCollectorTest.java
│                       │   └── PipelineTest.java
│                       ├── preprocessing/
│                       │   ├── DataPreprocessorTest.java
│                       │   └── LocationExtractorTest.java
│                       ├── analysis/
│                       │   └── DisasterAnalyzerTest.java
│                       └── config/
│                           ├── KeywordManagerTest.java
│                           └── CategoryManagerTest.java
│
├── python-api/                                (Python Sentiment Analysis)
│   ├── sentiment_api.py                      (Flask API)
│   ├── requirements.txt                       (Python dependencies)
│   ├── models/                               (Custom models)
│   │   └── vietnamese_sentiment.py           (Optional)
│   └── tests/
│       └── test_api.py
│
├── config/                                    (External Configuration)
│   ├── keywords.json
│   ├── categories.json
│   └── datasources.json
│
├── docs/                                      (Documentation)
│   ├── API.md                                (API documentation)
│   ├── ARCHITECTURE.md                       (Architecture guide)
│   ├── EXTENDING.md                          (Extension guide)
│   └── USER_GUIDE.md                         (User manual)
│
├── scripts/                                   (Utility Scripts)
│   ├── start-api.sh                          (Start Python API)
│   ├── build.sh                              (Build script)
│   └── deploy.sh                             (Deployment script)
│
├── pom.xml                                    (Maven configuration)
├── README.md                                  (Main documentation)
├── LICENSE
└── .gitignore

```

## Key Design Patterns Used

### 1. **Strategy Pattern**
- `DataSource` interface allows different data collection strategies
- `AnalysisAPI` interface enables swappable sentiment analysis models

### 2. **Factory Pattern**
- `SourceConfiguration` for creating data source configurations
- Can be extended for creating different types of analyzers

### 3. **Pipeline Pattern**
- `DisasterLogisticsPipeline` orchestrates data flow through stages:
  - Collection → Preprocessing → Analysis

### 4. **Observer Pattern** (Potential)
- Can be implemented for real-time updates in GUI
- Event listeners for data collection progress

### 5. **Singleton Pattern** (Optional)
- Configuration managers can be singletons
- Resource pools (HTTP clients, thread pools)

## Module Dependencies

```
DisasterLogisticsApp
    ↓
DisasterLogisticsPipeline
    ↓
    ├─→ DataCollector → DataSource (Facebook, Twitter, News)
    ├─→ DataPreprocessor → (KeywordManager, LocationExtractor)
    └─→ DisasterAnalyzer → (CategoryManager, AnalysisAPI)
                               ↓
                         PythonAnalysisClient (HTTP) → Python API
```

## Configuration Flow

```
1. Load keywords.json → KeywordManager
2. Load categories.json → CategoryManager
3. Load datasources.json → Configure DataSources
4. Initialize AnalysisAPI → Connect to Python service
5. Create Pipeline with all components
6. Launch GUI with Pipeline reference
```

## Data Flow

```
1. User triggers collection
2. DataCollector fetches from all DataSources (parallel)
3. Raw DisasterEvents collected
4. DataPreprocessor:
   - Cleans text
   - Extracts locations
   - Removes duplicates
   - Enriches with metadata
5. DisasterAnalyzer:
   - Categorizes events
   - Calculates statistics
   - Performs sentiment analysis
   - Generates report
6. Results displayed in GUI
```

## Extension Points

### Add New Data Source
1. Implement `DataSource` interface
2. Add to `datasources` package
3. Register in `DisasterLogisticsApp`

### Add New Analysis
1. Extend `DisasterAnalyzer`
2. Add custom analysis methods
3. Update report models if needed

### Customize Preprocessing
1. Extend `DataPreprocessor`
2. Override preprocessing steps
3. Add custom filters

### Change UI
1. Extend `DashboardFrame`
2. Add new panels/tabs
3. Integrate with pipeline

## Best Practices

1. **Separation of Concerns**: Each package has clear responsibility
2. **Interface-based Design**: Easy to swap implementations
3. **Configuration Externalization**: JSON-based configuration
4. **Error Handling**: Try-catch at appropriate levels
5. **Logging**: Use SLF4J for consistent logging
6. **Testing**: Unit tests for core logic
7. **Documentation**: Javadoc for public APIs

## Future Enhancements

- [ ] Real-time data streaming
- [ ] Web-based dashboard
- [ ] Advanced visualization
- [ ] Machine learning integration
- [ ] Multi-language support
- [ ] Database persistence
- [ ] RESTful API for external access
- [ ] Docker containerization
