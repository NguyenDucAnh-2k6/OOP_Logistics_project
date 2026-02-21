# OOP Logistics Project

A comprehensive data collection, preprocessing, and analysis platform for humanitarian logistics and disaster response. The system aggregates data from multiple Vietnamese news platforms, performs advanced text analysis including sentiment trends, damage classification, and relief sentiment analysis.

## Table of Contents
- [Project Overview](#project-overview)
- [Quick Start](#quick-start)
- [System Architecture](#system-architecture)
- [OOP Design Principles](#oop-design-principles)
- [Dependencies & Libraries](#dependencies--libraries)
- [Exception Handling](#exception-handling)
- [Testing](#testing)
- [Logging & Monitoring](#logging--monitoring)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)

---

## Project Overview

This application solves six key problems for humanitarian logistics and disaster response:

1. **Problem 1: Sentiment Time Series Analysis** - Track sentiment trends over time to identify community emotions and needs in disaster situations
2. **Problem 2: Damage Classification** - Automatically categorize disaster damage by type (infrastructure, people, environment, etc.) for targeted response planning
3. **Problem 3: Relief Sentiment Aggregation** - Analyze sentiment around relief efforts and resource allocation to optimize community acceptance and trust
4. **Problem 4: Relief Needs Trends Over Time** - Track how relief requirements evolve during prolonged disasters for dynamic resource planning
5. **Problem 5: Supply vs Demand Intent Classification** - Distinguish between people offering help/supplies and those requesting assistance to match supply with demand
6. **Problem 6: Resource Allocation & Prioritization** - Combine all analyses (damage, sentiment, intent, relief trends) with geographic clustering to recommend optimal resource allocation (routing, supply distribution, responder assignment)

The system combines:
- **Java backend**: Data crawling from multiple news sources, preprocessing, UI management
- **Python ML services**: Keyword search, machine learning, and deep learning models for sentiment, damage, relief, and intent analysis
- **JavaFX GUI**: User-friendly desktop application for interactive data exploration

---

## Quick Start

### Prerequisites

- **Java 21+** (Maven project)
- **Python 3.10+**
- **Maven 3.8+**
- **Git**

### Setup & Run (5 minutes)

#### 1. Clone and Setup Java Project

```bash
cd oop_logistics_projects
mvn clean install
```

#### 2. Setup Python Backend

```bash
cd python_model
python -m venv venv
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

pip install -r requirements.txt
```

#### 3. Start Python Server

```bash
# From python_model directory
uvicorn main:app --reload
```

This starts a FastAPI server on `http://localhost:8000`

#### 4. Launch Java Application

```bash
cd oop_logistics_projects
mvn clean javafx:run
# OR
mvn exec:java -Dexec.mainClass="com.oop.logistics.Launcher"
```

The JavaFX GUI will open, connecting to the Python backend at `http://localhost:8000`

---

## System Architecture

### High-Level Pipeline

```
┌─────────────────┐
│  News Sources   │ (ThanhNien, VnExpress, DanTri, TuoiTre, Facebook, DuckDuckGo, Bing)
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  Crawler Layer          │ (Factory Pattern)
│ - NewsCrawlerFactory    │
│ - Site-specific crawlers│
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Preprocessing Layer    │
│ - DateExtract           │
│ - LocationExtractor     │
│ - NewsPreprocess        │
│ - RemoveDuplicates      │
│ - CSV Processing        │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  Analysis Layer         │
│ - AnalysisAPI (Interface)
│ - PythonAnalysisClient  │
└────────┬────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  Python FastAPI Backend      │
│ - Problem 1: Sentiment       │
│ - Problem 2: Damage Class    │
│ - Problem 3: Relief Sentiment│
│ - Problem 4: Relief Trends   │
│ - Problem 5: Intent Class    │
│ - Problem 6: Prioritization  │
└────────┬─────────────────────┘
         │
         ▼
┌─────────────────────────┐
│  JavaFX UI              │
│ - DisasterFXApp         │
│ - Results Visualization │
│ - Resource Mapping      │
└─────────────────────────┘
```

---

## OOP Design Principles

### 1. **Factory Pattern** (Crawler Creation)
```java
// NewsCrawlerFactory.java
public static NewsCrawler getCrawler(String url) {
    if (url.contains("thanhnien.vn")) return new ThanhNienCrawler();
    if (url.contains("vnexpress.net")) return new VnExpressCrawler();
    // ... site-specific crawlers
}
```
**Benefit**: Decouples crawler instantiation from client code; easy to add new sources.

### 2. **Strategy Pattern** (Search Strategies)
- `SearchStrategy` interface
- Concrete implementations: `GoogleNewsRssStrategy`, `BingRssStrategy`, `DuckDuckGoStrategy`, `BingDirectStrategy`

**Benefit**: Swap search algorithms at runtime without modifying client code.

### 3. **Dependency Injection** (Analysis API)
```java
// Interface-based design
public interface AnalysisAPI {
    List<Map<String, Object>> getSentimentTimeSeries(...);
    List<Map<String, Object>> getDamageAnalysis(...);
    // ...
}

// Implementation
public class PythonAnalysisClient implements AnalysisAPI {
    // Connects to Python backend
}
```

**Benefit**: Swap implementations easily (Java models → Python models, mock implementations for testing).

### 4. **Configuration Management** (CategoryManager)
- Centralized loading/saving of JSON configs
- Automatic Category object creation from JSON
- Simple dictionary format for keyword management

**Benefit**: External config handling without hardcoding; easy updates.

### 5. **Single Responsibility Principle (SRP)**
- `DateExtract` - Only date extraction
- `LocationExtractor` - Only location extraction
- `NewsPreprocess` - Text cleaning and normalization
- `CsvWriter` - CSV file writing

Each class has one reason to change.

### 6. **Open/Closed Principle**
- New crawlers added by extending `NewsCrawler` base class
- New search strategies added by implementing `SearchStrategy` interface
- No modification to existing code needed

### 7. **Model-View-Controller (MVC) Pattern** (JavaFX UI)
```
┌─────────────────────────────────────────────────────────────┐
│                    MVC Architecture                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  View (FXML + JavaFX Components)                           │
│  ├── MainView.fxml          - Application main layout      │
│  ├── InputPanel.fxml        - User input interface         │
│  ├── DataSourceSelection.fxml - Data source selection     │
│  ├── AnalysisPanel.fxml     - Analysis results display    │
│  ├── ModeSelection.fxml     - Mode selection interface    │
│  └── KeywordContribution.fxml - Keyword contribution view │
│                                                             │
│  ▲                                                         │
│  │ Updates UI                                             │
│  │                                                         │
│  │ User Interactions                                       │
│  │                                                         │
│  ▼                                                         │
│                                                             │
│  Controller (UI Controllers)                               │
│  ├── Handles user events (button clicks, form inputs)     │
│  ├── Updates Model based on user actions                  │
│  ├── Retrieves data from Model to update View             │
│  └── Coordinates between View and Model                   │
│                                                             │
│  ▲                                                         │
│  │ Queries & Updates                                      │
│  │                                                         │
│  ▼                                                         │
│                                                             │
│  Model (Business Logic & Data)                             │
│  ├── AnalysisAPI - Analysis interface                     │
│  ├── PythonAnalysisClient - Data access                   │
│  ├── CategoryManager - Configuration management           │
│  ├── Crawler objects - News data models                   │
│  └── NewsResult - Data model for results                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Benefit**: Clear separation of concerns; UI, business logic, and data are independent; easy to test and maintain; UI can be updated without affecting business logic.

---

## Dependencies & Libraries

### Java Dependencies (Maven)

#### Core Libraries
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **Gson** | 2.10.1 | JSON serialization/deserialization |
| **Selenium WebDriver** | 4.25.0 | Browser automation for web scraping |
| **WebDriverManager** | 5.6.2 | Automatic WebDriver management |
| **JavaFX** | 21 | Modern GUI framework |

#### Logging
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **SLF4J API** | 2.0.9 | Logging facade |
| **Logback** | 1.4.11 | Logging implementation |

#### Testing
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **JUnit 5** | 5.10.0 | Unit testing framework |
| **Mockito** | 5.5.0 | Mocking for unit tests |

#### Crawlers & Web Scraping
- **Selenium**: Automates browser for JavaScript-heavy news sites
- **HTTP Client (Built-in)**: Lightweight HTTP requests (Java 11+ HttpClient)
- **JSoup**: HTML parsing (if added)
- **DuckDuckGo, Google News, Bing RSS**: Various search source integrations

### Python Dependencies

#### Framework & API
```
fastapi          # Web framework for ML API
uvicorn          # ASGI server
pydantic         # Request/response validation
```

#### Machine Learning & NLP
```
torch            # Deep learning framework (PyTorch)
transformers     # Pre-trained NLP models (HuggingFace)
scipy            # Scientific computing utilities
```

#### Data Processing & Visualization
```
pandas           # Data manipulation and analysis
numpy            # Numerical computing
matplotlib       # Data visualization
requests         # HTTP client for inter-service communication
```

---

## Testing

### Testing Strategy & Benefits

The project uses a multi-layered testing approach to ensure reliability, maintainability, and correctness:

#### Benefits of Comprehensive Testing
- **Early Bug Detection**: Unit tests catch issues before integration
- **Regression Prevention**: Tests ensure new changes don't break existing functionality
- **Documentation**: Tests serve as executable specifications of expected behavior
- **Confidence in Refactoring**: Safe to improve code with test coverage
- **Reduced Debugging Time**: Failed tests pinpoint issues quickly

### Java Unit Testing (JUnit 5 + Mockito)

#### Test Coverage

**Unit Tests** in `src/test/java/com/oop/logistics/`:

| Test Class | Coverage | Purpose |
|-----------|----------|---------|
| `TestCategoryManager` | Configuration loading | Verifies JSON category configs load correctly |
| `TestNewsCrawlerFactory` | Crawler instantiation | Ensures correct crawler chosen for each news domain |
| `TestVnExpressCrawler` | Site-specific crawling | Tests article extraction from VnExpress |
| `TestDateExtract` | Date parsing | Validates date extraction from various formats |
| `TestLocationExtractor` | Location extraction | Verifies location entity recognition |
| `TestDatabasePreprocessor` | Data preprocessing | Tests text normalization and cleaning |
| `TestProcessCSV` | CSV handling | Validates CSV reading/writing operations |
| `TestDataRepository` | Data persistence | Tests database operations |

#### Running Tests

```bash
cd oop_logistics_projects

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TestVnExpressCrawler

# Run with coverage report
mvn test jacoco:report
```

#### Test Example: Crawler Factory Pattern
```java
@Test
public void testGetCrawlerVnExpress() {
    NewsCrawler crawler = NewsCrawlerFactory.getCrawler("https://vnexpress.net/article");
    assertThat(crawler).isInstanceOf(VnExpressCrawler.class);
}

@Test
public void testGetCrawlerThanhNien() {
    NewsCrawler crawler = NewsCrawlerFactory.getCrawler("https://thanhnien.vn/news");
    assertThat(crawler).isInstanceOf(ThanhNienCrawler.class);
}

@Test
public void testInvalidCrawlerThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
        NewsCrawlerFactory.getCrawler("https://unknown-site.com");
    });
}
```

**Benefits**: Factory Pattern tests ensure correct crawler selection without manual URL checking; prevents runtime errors.

#### Test Example: Mocking External Dependencies
```java
@Test
public void testAnalysisClientHandlesPythonServerDown() {
    PythonAnalysisClient client = new PythonAnalysisClient("http://bogus-url");
    assertFalse(client.isAvailable());
    // Tests graceful degradation when Python API unavailable
}
```

**Benefits**: Mocking prevents tests from depending on external service availability; tests run fast and reliably.

### Python API Testing

#### Testing Framework: pytest (recommended addition)

```python
# tests/test_sentiment_service.py
import pytest
from services.sentiment_service import predict_ai_batch, predict_keyword

def test_predict_keyword_positive():
    """Test keyword-based sentiment detection"""
    result = predict_keyword("Đây là tin tốt lành")  # "This is good news"
    assert result == "positive"

def test_predict_keyword_negative():
    """Test negative sentiment detection"""
    result = predict_keyword("Thảm họa kinh khủng")  # "Terrible disaster"
    assert result == "negative"

def test_predict_ai_batch():
    """Test batch processing efficiency"""
    texts = [
        "Tin vui từ cứu trợ",  # "Good news from relief"
        "Tình hình xấu đi",     # "Situation getting worse"
    ]
    results = predict_ai_batch(texts)
    assert len(results) == len(texts)
    assert results[0] in ["positive", "neutral", "negative"]

@pytest.fixture
def sample_intent_texts():
    """Sample texts for intent classification testing"""
    return {
        "demand": ["Chúng tôi cần thức ăn", "Xin hỗ trợ y tế"],
        "supply": ["Tôi có thể hiến máu", "Công ty tặng 1000 cái mũ bảo hiểm"],
        "news": ["Bão sắp đến miền Bắc", "Lũ kiểm soát được"]
    }

def test_intent_classification(sample_intent_texts):
    """Test intent classification accuracy"""
    for intent_type, texts in sample_intent_texts.items():
        results = classify_intent_batch(texts)
        # Verify results contain expected intent categories
        assert any(intent_type.lower() in str(r).lower() for r in results)
```

**Benefits**: Python tests validate ML models produce sensible outputs; catch data shape mismatches before Java integration.

#### Integration Testing: Java ↔ Python

```java
@Test
public void testPythonAnalysisEndToEnd() throws Exception {
    // Requires Python server running
    List<String> texts = Arrays.asList("Tốt lành", "Quá đau lòng");
    List<String> dates = Arrays.asList("2024-01-01", "2024-01-02");
    
    List<Map<String, Object>> result = analysisClient.getSentimentTimeSeries(
        texts, dates, "keyword"
    );
    
    assertNotNull(result);
    assertEquals(2, result.size());  // Same as input count
}
```

**Benefits**: Validates Java-Python communication; ensures serialization/deserialization works correctly.

### Manual Testing Checklist

- [ ] **UI Responsiveness**: GUI doesn't freeze during crawling
- [ ] **Error Messages**: User-friendly error messages for failures
- [ ] **Data Accuracy**: Crawled data matches webpage content
- [ ] **Analysis Results**: Sentiment/damage/relief classifications make sense
- [ ] **Export Functionality**: CSV exports contain correct data
- [ ] **Cross-platform**: App runs on Windows/macOS/Linux

---

## Logging & Monitoring

### Logging Architecture

The project implements **three-tier component-based logging** for clear separation of concerns:

```
logs/
├── logistics-app.log           (Java core application, search service)
├── crawlers.log                (Web crawler activity)
├── python-api-client.log       (Java ↔ Python API communication)
└── python-api.log              (Python FastAPI services)
```

#### Benefits of Structured Logging

- **Debugging Efficiency**: Isolate issues to specific components without log noise
- **Performance Monitoring**: Track response times and resource usage
- **Security Auditing**: Log authentication, authorization, and sensitive operations
- **Production Insights**: Understand system behavior in real-world conditions
- **Compliance**: Maintain audit trails for reliability requirements

### Java Logging (Logback)

#### Configuration: `src/main/resources/logback.xml`

```xml
<!-- 3 separate file appenders -->
<appender name="MAIN_FILE">
    <file>logs/logistics-app.log</file>
    <!-- Rotates daily, keeps 30 days, max 1GB total -->
</appender>

<appender name="CRAWLERS_FILE">
    <file>logs/crawlers.log</file>
    <!-- Max 500MB, 30-day retention -->
</appender>

<appender name="PYTHON_API_FILE">
    <file>logs/python-api-client.log</file>
    <!-- Tracks all Java → Python API calls -->
</appender>

<!-- Route crawler logs to CRAWLERS_FILE only -->
<logger name="com.oop.logistics.crawler" 
        level="INFO" 
        additivity="false">
    <appender-ref ref="CRAWLERS_FILE" />
</logger>

<!-- Route Python API calls to dedicated file -->
<logger name="com.oop.logistics.analysis.PythonAnalysisClient" 
        level="INFO" 
        additivity="false">
    <appender-ref ref="PYTHON_API_FILE" />
</logger>
```

#### Java Crawling Logs

```
2026-02-21 14:54:20 [Thread-3] INFO  c.o.l.crawler.ThanhNienCrawler - 
  Crawling: https://thanhnien.vn/news-article-12345.htm
2026-02-21 14:54:21 [Thread-3] INFO  c.o.l.crawler.ThanhNienCrawler - 
  Title: "Disaster Relief Efforts Mobilized"
2026-02-21 14:54:22 [Thread-3] INFO  c.o.l.crawler.ThanhNienCrawler - 
  Date: 2026-02-21 | Location: Hanoi | Word Count: 450
```

**Benefits**: Quickly identify which crawlers are slow, failing, or returning bad data.

#### Python API Client Logs

```
2026-02-21 15:00:15 [Thread-5] INFO  c.o.l.analysis.PythonAnalysisClient - 
  Sending sentiment analysis request: 150 texts
2026-02-21 15:00:18 [Thread-5] INFO  c.o.l.analysis.PythonAnalysisClient - 
  Sentiment response received: positive=45, neutral=60, negative=45
2026-02-21 15:00:20 [Thread-5] ERROR c.o.l.analysis.PythonAnalysisClient - 
  Damage classification failed (timeout after 30s). Python server laggy?
```

**Benefits**: Understand system bottlenecks, validate API contract, detect Python server issues.

### Python Logging (Python `logging` module)

#### Configuration: `python_model/logging_config.py`

```python
import logging
from logging.handlers import RotatingFileHandler

# Setup structured logging for all Python services
logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

# File handler: 10MB per file, keep 10 backups
file_handler = RotatingFileHandler(
    "logs/python-api.log",
    maxBytes=10 * 1024 * 1024,
    backupCount=10
)

# Format: timestamp [logger_name] level - message
formatter = logging.Formatter(
    '%(asctime)s [%(name)s] %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

file_handler.setFormatter(formatter)
logger.addHandler(file_handler)
```

#### Python Service Logs

```
2026-02-21 15:00:10 [services.sentiment_service] INFO - 
  Loading Sentiment AI Model (PhoBERT)...
2026-02-21 15:00:15 [services.sentiment_service] INFO - 
  Sentiment AI Model loaded successfully

2026-02-21 15:00:20 [main] INFO - 
  [Problem 1] Sentiment Request (model_type=transformer, texts_count=150)
2026-02-21 15:00:22 [services.sentiment_service] DEBUG - 
  AI Classifying Sentiment for 150 items...
2026-02-21 15:00:25 [main] INFO - 
  [Problem 1] Sentiment analysis completed successfully

2026-02-21 15:00:30 [main] ERROR - 
  [Problem 2] Error during damage classification: CUDA out of memory
  Traceback (most recent call last):
    File "main.py", line 65, in analyze_damage
      result = classify_damage_batch(req.texts, req.model_type)
    ...
```

**Benefits**: Detects ML model issues (OOM, slow inference), tracks API request flow, helps debug model problems.

#### Uvicorn Server Logs

```
2026-02-21 15:00:00 INFO:     Uvicorn running on http://127.0.0.1:8000
2026-02-21 15:00:05 INFO:     127.0.0.1:54321 - "POST /analyze/sentiment_timeseries HTTP/1.1" 200
2026-02-21 15:00:10 INFO:     127.0.0.1:54322 - "POST /analyze/damage HTTP/1.1" 500
2026-02-21 15:00:15 INFO:     127.0.0.1:54323 - "POST /analyze/intent HTTP/1.1" 200
```

### Monitoring Logs in Real-Time

#### Tail Java logs (search service activity):
```bash
# Windows PowerShell
Get-Content logs\logistics-app.log -Tail 50 -Wait

# macOS/Linux
tail -f logs/logistics-app.log
```

#### Tail crawler logs only:
```bash
tail -f logs/crawlers.log
```

#### Tail Python API logs:
```bash
tail -f logs/python-api.log
```

#### Search logs for errors:
```bash
# Find all ERROR entries
grep "ERROR" logs/logistics-app.log

# Find errors in last 24 hours
grep "2026-02-21" logs/logistics-app.log | grep "ERROR"

# Count errors by component
grep "ERROR" logs/*.log | wc -l
```

### Log Rotation & Retention

#### Java (Logback)
- **Rotation**: Daily (e.g., `logistics-app-2026-02-21.log`)
- **Retention**: 30 days
- **Size Cap**: 1GB for main log, 500MB per category
- **Auto-cleanup**: Oldest files deleted when cap exceeded

#### Python
- **Size-based**: Rotates at 10MB per file
- **Backups**: Keeps 10 historical files (~100MB total)
- **Manual cleanup**: Delete old backups from `logs/` directory

### Performance & Disk Usage

| Log File | Typical Size/Day | Purpose |
|----------|-----------------|---------|
| logistics-app.log | 5-10 MB | Core operations, search |
| crawlers.log | 20-50 MB | High volume (many URLs) |
| python-api-client.log | 2-5 MB | API communication |
| python-api.log | 15-30 MB | ML model inference |

**Total monthly storage**: ~3-4 GB (with 30-day retention on Java, auto-rotation on Python)

**Optimization tips**:
- Reduce log level from DEBUG to INFO in production
- Archive old logs to compressed storage
- Use log aggregation (ELK stack, Splunk) for large deployments

---

## Exception Handling

### Java Exception Strategy

#### Custom Exceptions (if defined)
```java
// Handle site-specific crawling failures
throw new IllegalArgumentException("Unsupported news site: " + url);

// HTTP connection timeouts
throws Exception // from PythonAnalysisClient

// File I/O errors
throws IOException // from CategoryManager
```

#### Exception Hierarchy
```
Exception
├── IOException (File operations)
├── DateTimeParseException (Date extraction)
├── IllegalArgumentException (Invalid crawler URL)
└── HttpConnectException (Python API failures)
```

#### Handling Pattern
```java
// In PythonAnalysisClient
try {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(apiUrl + "/"))
        .GET()
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        .statusCode() == 200;
} catch (Exception e) { 
    logger.error("Python API unavailable", e);
    return false;
}
```

#### Key Exception Scenarios
| Scenario | Exception | Handling |
|----------|-----------|----------|
| Python server down | HttpTimeoutException | Retry with exponential backoff |
| Invalid CSV format | IOException | Log error, skip malformed rows |
| Unsupported news site | IllegalArgumentException | Log & show user message |
| Date parsing fails | DateTimeParseException | Use default date or skip record |
| Crawler network error | IOException | Retry or fallback strategy |

### Python Exception Handling

```python
@app.post("/analyze/sentiment_timeseries")
def analyze_sentiment_timeseries(req: SentimentTimeSeriesRequest):
    logger.info(f"[Problem 1] Sentiment Request (model_type={req.model_type})")
    try:
        result = aggregate_by_date(req.texts, req.dates, req.model_type)
        logger.info("[Problem 1] Sentiment analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 1] Error: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})
```

**Strategy**: 
- Pydantic validates request schemas automatically
- Services throw exceptions with context
- Handlers catch and return HTTP 5xx with error details to Java client
- Logging captures full stack traces for debugging

#### Error Handling by Problem

| Problem | Common Errors | Recovery Strategy |
|---------|---------------|-------------------|
| 1-5: Analysis | Model not loaded, OOM, timeout | Fallback to keyword-based method |
| 1-5: Analysis | Invalid text input | Skip problematic items, process rest |
| 5: Intent | Empty or very short text | Classify as "Unknown" |
| 6: Prioritization | Missing geographic data | Use alternative ranking criteria |

---

## Testing

### Run Unit Tests
```bash
cd oop_logistics_projects
mvn test
```

Test files in `src/test/java/com/oop/logistics/`:
- `crawler/TestNewsCrawler.java` - Crawler functionality
- `preprocessing/TestNewsPreprocess.java` - Text preprocessing
- `search/TestSearch.java` - Search strategies

---

```
OOP_Logistics_project/
├── oop_logistics_projects/          # Java Maven project
│   ├── pom.xml                      # Maven configuration
│   ├── external config/
│   │   ├── damage_keywords.json     # Damage classification keywords
│   │   ├── relief_keywords.json     # Relief operation keywords
│   │   ├── sentiment_keywords.json  # Sentiment analysis keywords
│   │   └── disasters.json           # Disaster type definitions
│   ├── src/main/java/com/oop/logistics/
│   │   ├── Launcher.java            # Entry point
│   │   ├── analysis/
│   │   │   ├── AnalysisAPI.java     # Interface
│   │   │   └── PythonAnalysisClient.java
│   │   ├── config/
│   │   │   ├── Category.java
│   │   │   ├── CategoryManager.java # Config loader
│   │   │   └── KeywordManager.java
│   │   ├── crawler/
│   │   │   ├── NewsCrawler.java     # Base class
│   │   │   ├── NewsCrawlerFactory.java
│   │   │   ├── ThanhNienCrawler.java
│   │   │   ├── VnExpressCrawler.java
│   │   │   ├── DanTriCrawler.java
│   │   │   ├── TuoiTreCrawler.java
│   │   │   ├── FacebookCrawler.java
│   │   │   ├── NewsResult.java
│   │   │   └── CsvWriter.java
│   │   ├── models/
│   │   │   ├── AnalysisRequest.java
│   │   │   └── AnalysisResponse.java
│   │   ├── preprocessing/
│   │   │   ├── DateExtract.java
│   │   │   ├── LocationExtractor.java
│   │   │   ├── NewsPreprocess.java
│   │   │   ├── ProcessCSV.java
│   │   │   ├── RemoveDuplicates.java
│   │   │   └── StripLevel.java
│   │   ├── search/
│   │   │   ├── SearchStrategy.java  # Interface
│   │   │   ├── GoogleNewsRssStrategy.java
│   │   │   ├── BingRssStrategy.java
│   │   │   ├── DuckDuckGoStrategy.java
│   │   │   ├── BingDirectStrategy.java
│   │   │   ├── DisasterSearchService.java
│   │   │   ├── DateUtils.java
│   │   │   ├── SearchUtils.java
│   │   │   └── UrlUtils.java
│   │   └── ui/
│   │       └── DisasterFXApp.java   # JavaFX GUI
│   ├── src/main/resources/
│   │   └── stopwords.txt
│   └── target/                      # Build output
├── python_model/                    # Python FastAPI backend
│   ├── main.py                      # Entry point
│   ├── config.py                    # Python config
│   ├── models/
│   │   ├── schemas.py               # Pydantic models
│   │   └── __init__.py
│   ├── services/
│   │   ├── sentiment_service.py
│   │   ├── damage_service.py
│   │   ├── relief_service.py
│   │   ├── model_loader.py
│   │   └── __init__.py
│   ├── requirements.txt
│   └── venv/                        # Virtual environment
├── README.md                        # This file
└── requirements.txt                 # Python dependencies
```

---

## Running the Application

### Full Application Flow

#### Step 1: Start Python Backend
```bash
cd python_model
source venv/bin/activate  # or venv\Scripts\activate on Windows
uvicorn main:app --reload
```
**Output**: `Uvicorn running on http://127.0.0.1:8000`

#### Step 2: Verify Python Backend
```bash
curl http://localhost:8000/
# Expected: {"status": "running", "message": "Python Analysis Backend is Active"}
```

#### Step 3: Build Java Project
```bash
cd oop_logistics_projects
mvn clean install
```

#### Step 4: Run Java Application
```bash
mvn package
mvn exec:java -Dexec.mainClass="com.oop.logistics.Launcher"
```

#### Step 5: Use the GUI
- Click "Crawl News" to collect data from Vietnamese news sources
- Select preprocessing options (remove duplicates, extract dates, etc.)
- View results and export to CSV
- Run analysis (Sentiment, Damage Classification, Relief Sentiment)

### Maven Commands

```bash
# Build project
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn clean package

# Run directly
mvn exec:java -Dexec.mainClass="com.oop.logistics.Launcher"

# Run with JavaFX
mvn clean javafx:run

# View dependencies
mvn dependency:tree
```

### Python Testing

```bash
# Test Python API directly
python -c "
import requests
response = requests.post('http://localhost:8000/analyze/sentiment_timeseries', json={
    'texts': ['Happy news!', 'Sad event'],
    'dates': ['2024-01-01', '2024-01-02'],
    'model_type': 'transformer'
})
print(response.json())
"
```

---

## Configuration

### Java Configuration Files

#### `external config/damage_keywords.json`
```json
{
  "Infrastructure": ["..."],
  "People": ["..."],
  "Environment": ["..."]
}
```

#### `external config/relief_keywords.json`
```json
{
  "Medical": ["..."],
  "Food": ["..."],
  "Shelter": ["..."]
}
```

#### `external config/sentiment_keywords.json`
```json
{
  "Positive": ["..."],
  "Negative": ["..."]
}
``` 
#### `external config/disasters.json`

### Python Configuration

Edit `python_model/config.py` for:
- Model types (transformer, bert, gpt)
- API port (default 8000)
- Batch sizes for ML inference
- Timeout values

---

## Testing

### Run Unit Tests
```bash
cd oop_logistics_projects
mvn test
```

Test files in `src/test/java/com/oop/logistics/`:
- `crawler/TestNewsCrawler.java` - Crawler functionality
- `preprocessing/TestNewsPreprocess.java` - Text preprocessing
- `search/TestSearch.java` - Search strategies

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Python server won't start | Check port 8000 is free; verify Python 3.10+ installed |
| Java can't connect to Python | Ensure Python server running; check firewall settings |
| Crawler fails on website | Site structure may have changed; update site-specific crawler |
| Out of memory during crawling | Increase JVM memory: `export _JAVA_OPTIONS="-Xmx4g"` |
| Tests fail | Run `mvn clean install` first to fetch all dependencies |

---

## Contributors & Contact

For issues or contributions, please refer to the project wiki or create an issue in the repository.

**Last Updated**: February 2026


