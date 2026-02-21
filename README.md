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
2. **Problem 2: Damage Classification** - Automatically categorize disaster damage by type (infrastructure, people, environment, etc.) for targeted response planning. 
3. **Problem 3: Determine public satisfaction and dissatisfaction** By identifying which relief areas receive positive or negative public evaluation, humanitarian agencies and governments can prioritize resource allocation to the most urgent needs (for example shelter and transportation). This improves the effectiveness of relief efforts and helps ensure resources are used optimally
4. **Problem 4: Relief Needs Trends Over Time** - By analyzing sentiment over time for each relief category, the study provides detailed insight into the effectiveness of humanitarian logistics in each sector. For example, positive sentiment related to medical services and food suggests success in those areas, while negative sentiment about shelter and transportation points to gaps that need to be addressed.

5. **Problem 5: Supply vs Demand Intent Classification** - Distinguish between people offering help/supplies and those requesting assistance to match supply with demand using Pie Chart.
6. **Problem 6: Geographical detection & Prioritization** - Extract provinces most affected by disaster so as to prioritize resource delivery, alongside the frequency with which they are mentioned.

The system combines:
- **Java backend**: Article searching, data crawling from multiple news sources, Facebook, preprocessing, UI controllers.
- **Python ML services**: Keyword search, machine learning, and deep learning models routed to each specific problem.
- **JavaFX GUI**: User-friendly desktop application.

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
# OR SIMPLY CLICK "RUN JAVA"
```

The JavaFX GUI will open, connecting to the Python backend at `http://localhost:8000` via Python API client.

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
│ - DatabasePreprocessor  │
│ - ProcessCSV            │
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
│ - Problem 6: Location        |
└────────┬─────────────────────┘
         │
         ▼
┌─────────────────────────┐
│  JavaFX UI              │
│ - Load DB & analyze     │
│ - Results Visualization │
│ - Search & crawl        │
└─────────────────────────┘
```

---

## OOP Design Principles

### 1. **Factory Pattern** (Crawler Creation)
```java
// NewsCrawlerFactory.java
public static NewsCrawler getCrawler(String url) {
    if (url.contains("thanhnien.vn")) {
            logger.debug("Creating ThanhNienCrawler for {}", url);
            return new ThanhNienCrawler();
        }
    if (url.contains("vnexpress.net")) {
            logger.debug("Creating VnExpressCrawler for {}", url);
            return new VnExpressCrawler();
        }
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
    List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress) throws Exception;

        List<String> getDamageClassification(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception;

    // ... Contracts for API clients (extendable to a model implemented in Java)
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
- `DatabasePreprocessor` - Database operations and text formatting
- `ProcessCSV` - CSV file handling

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
@DisplayName("Should return correct crawler based on URL domain")
    void testGetCrawlerWithValidUrls(String url) {
        logger.debug("Testing factory with URL: {}", url);
        
        NewsCrawler crawler = NewsCrawlerFactory.getCrawler(url);
        
        assertNotNull(crawler, "Crawler should be instantiated for valid URL");
        
        // Optional: Check specific instance types based on the URL string
        if (url.contains("thanhnien.vn")) assertTrue(crawler instanceof ThanhNienCrawler);
        if (url.contains("vnexpress.net")) assertTrue(crawler instanceof VnExpressCrawler);
    }
```

**Benefits**: Factory Pattern tests ensure correct crawler selection without manual URL checking; prevents runtime errors.

#### Test Example: Use Mockito for DatabaseRepository testing
```java
@DisplayName("Should successfully get or create a disaster and return ID")
    void testGetOrCreateDisaster() throws Exception {
        // 1. Arrange: Create mock JDBC objects
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockInsertStmt = mock(PreparedStatement.class);
        PreparedStatement mockSelectStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        // 2. Arrange: Define the behavior of our mocks
        when(mockConn.prepareStatement(contains("INSERT"))).thenReturn(mockInsertStmt);
        when(mockConn.prepareStatement(contains("SELECT"))).thenReturn(mockSelectStmt);
        
        // Simulate the select statement finding ID '99'
        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); 
        when(mockResultSet.getInt("id")).thenReturn(99);

        // 3. Act: We must mock the static DatabaseManager.getConnection() 
        // inside a try-with-resources block so it closes properly after the test.
        try (MockedStatic<DatabaseManager> mockedDb = mockStatic(DatabaseManager.class)) {
            mockedDb.when(DatabaseManager::getConnection).thenReturn(mockConn);
            
            logger.info("Testing getOrCreateDisaster with mocked database connection");
            int disasterId = repository.getOrCreateDisaster("Typhoon Yagi");
            
            // 4. Assert
            assertEquals(99, disasterId, "Should return the ID retrieved from the database");
            
            // Verify our statements were actually called with the right data
            verify(mockInsertStmt).setString(1, "Typhoon Yagi");
            verify(mockInsertStmt).executeUpdate();
            verify(mockSelectStmt).setString(1, "Typhoon Yagi");
        }
    }
```

**Benefits**: Mocking prevents tests from depending on external service availability; tests run fast and reliably.

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
2026-02-21 19:04:33 [Thread-4] INFO  c.o.l.crawler.VnExpressCrawler - Successfully extracted data from URL.
2026-02-21 19:04:33 [Thread-4] INFO  c.o.l.crawler.VnExpressCrawler - Successfully extracted data from URL.
2026-02-21 19:04:33 [Thread-4] INFO  c.o.logistics.crawler.DanTriCrawler - Starting crawl for URL: http://www.bing.com/news/apiclick.aspx?ref=FexRss&aid=&tid=69999edabebc4c7d93dd8f5efe518c43&url=https%3a%2f%2fdantri.com.vn%2fo-to-xe-may%2fchu-lynk-co-09-cho-phu-tung-4-thang-hang-can-cai-thien-dich-vu-sau-ban-20260210155817892.htm&c=588608706095726375&mkt=en-ww
2026-02-21 19:04:34 [Thread-4] INFO  c.o.logistics.crawler.DanTriCrawler - Starting crawl for URL: http://www.bing.com/news/apiclick.aspx?ref=FexRss&aid=&tid=69999eea6f8644ed9267094125143830&url=https%3a%2f%2fdantri.com.vn%2fo-to-xe-may%2fchu-lynk-co-09-cho-phu-tung-4-thang-hang-can-cai-thien-dich-vu-sau-ban-20260210155817892.htm&c=588608706095726375&mkt=en-ww
```

**Benefits**: Quickly identify which crawlers are slow, failing, or returning bad data.

#### Python API Client Logs

```
2026-02-21 19:05:05 [Thread-7] INFO  c.o.l.analysis.PythonAnalysisClient - Preparing to send data to Python backend at endpoint: /analyze/sentiment_timeseries
2026-02-21 19:05:06 [Thread-7] INFO  c.o.l.analysis.PythonAnalysisClient - Successfully received response from /analyze/sentiment_timeseries
2026-02-21 19:05:07 [Thread-8] INFO  c.o.l.analysis.PythonAnalysisClient - Preparing to send data to Python backend at endpoint: /analyze/damage
```

**Benefits**: Understand system bottlenecks, validate API contract, detect Python client issues (low Internet...)

### Python Logging (Python `logging` module)

#### Configuration: `python_model/logging_config.py`

```python
import logging
from logging.handlers import RotatingFileHandler

# Setup structured logging for all Python services
logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

# File handler: 10MB per file, keep 10 backups
API_LOG_FILE = os.path.join(LOG_DIR, "python-api.log")


file_handler = RotatingFileHandler(
        API_LOG_FILE,
        maxBytes=10 * 1024 * 1024,  # 10MB
        backupCount=10,
        encoding='utf-8'  # <--- ADD THIS LINE
    )
    file_handler.setLevel(logging.DEBUG)
    
    # === CONSOLE HANDLER ===
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    
    # === FORMATTER ===
    formatter = logging.Formatter(
        '%(asctime)s [%(name)s] %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    
    file_handler.setFormatter(formatter)
    console_handler.setFormatter(formatter)
    
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    logging.getLogger("uvicorn.access").setLevel(logging.INFO)
    logging.getLogger("uvicorn.error").setLevel(logging.INFO)
    logging.getLogger("fastapi").setLevel(logging.INFO)
```

#### Python Service Logs

```
2026-02-21 19:05:06 [root] INFO - [Problem 1] Sentiment Request (model_type=keyword, texts_count=1)
2026-02-21 19:05:06 [root] INFO - [Problem 1] Sentiment analysis completed successfully
2026-02-21 19:05:07 [root] INFO - [Problem 2] Damage Request (model_type=keyword, texts_count=1)
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
Get-Content logs\search.log -Tail 50 -Wait

# macOS/Linux
tail -f logs/search.log
```
#### Tail API client logs:
```bash
tail -f logs/python-api-client.log
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
grep "ERROR" logs/*.log
# Find errors in last 24 hours
grep "2026-02-21" logs/*.log | grep "ERROR"
# Count errors by component
grep "ERROR" logs/*.log | wc -l
```
On Windows Powershell: 
```bash
Select-String -Path "logs\*.log" -Pattern "ERROR"
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
| ```search.log``` | 5-10 MB | Core operations, search |
| ```crawlers.log``` | 20-50 MB | High volume (many URLs) |
| ```python-api-client.log``` | 2-5 MB | API communication |
|``` python-api.log``` | 15-30 MB | ML model inference |

**Total monthly storage**: ~3-4 GB (with 30-day retention on Java, auto-rotation on Python)

**Optimization tips**:
- Reduce log level from DEBUG to INFO in production
- Archive old logs to compressed storage
- Use log aggregation (ELK stack, Splunk) for large deployments

---

## Exception Handling

### Java Exception Strategy

#### Exception Hierarchy
```
Exception
├── IOException (File operations)
├── DateTimeParseException (Date extraction)
├── IllegalArgumentException (Invalid crawler URL)
└── HttpConnectException (Python API failures)
```

#### Key Exception Scenarios
| Scenario | Exception | Handling |
|----------|-----------|----------|
| Python server down | HttpTimeoutException | Log error, gracefully disable Python UI features |
| Invalid CSV format | IOException | Log error, skip malformed rows/URLs |
| Unsupported news site | IllegalArgumentException | Log & show user message |
| Crawler network error | IOException | Timeout gracefully and return partial results

### Python Exception Handling

```python
@app.post("/analyze/sentiment_timeseries")
def analyze_sentiment_timeseries(req: SentimentTimeSeriesRequest):
    """
    Problem 1: Sentiment trend over time.
    """
    logger.info(f"[Problem 1] Sentiment Request (model_type={req.model_type}, texts_count={len(req.texts)})")
    try:
        result = aggregate_by_date(req.texts, req.dates, req.model_type)
        logger.info(f"[Problem 1] Sentiment analysis completed successfully")
        return result
    except Exception as e:
        logger.error(f"[Problem 1] Error during sentiment analysis: {e}", exc_info=True)
        return JSONResponse(status_code=500, content={"error": str(e)})
```

**Strategy**: 
- Pydantic validates request schemas automatically
- Services throw exceptions with context
- Handlers catch and return HTTP 5xx with error details to Java client
- Logging captures full stack traces for debugging


---

## Testing

### Run Unit Tests
```bash
cd oop_logistics_projects
mvn test
```
---
## Project Structure
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
│   │   ├── analysis/                # API and Clients
│   │   ├── config/                  # JSON Config loaders
│   │   ├── crawler/                 # Web scrapers
│   │   ├── models/                  # Data structures
│   │   ├── preprocessing/           # Data cleaning & extraction
│   │   ├── search/                  # Search strategies
│   │   └── ui/                      # JavaFX GUI
│   ├── src/main/resources/          # CSS, FXML, Logging configs
│   └── target/                      # Build output
├── python_model/                    # Python FastAPI backend
│   ├── main.py                      # Entry point
│   ├── config.py                    # Python config
│   ├── logging_config.py            # Python logging config
│   ├── models/                      # Pydantic models
│   ├── services/                    # Analysis logic (sentiment, intent, etc.)
│   ├── requirements.txt
│   └── venv/                        # Virtual environment
└── README.md                        # This file
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
- Set disaster name on TopBar, the app automatically searches for URLs
- Click "Crawl" to collect data from those URLs
- Select preprocessing options (extract dates, database migration, etc.)
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
#### `external config/intent_service.json`
### Python Configuration

Edit `python_model/config.py` for:
- Model types (transformer, bert, gpt)
- API port (default 8000)
- Batch sizes for ML inference

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
| Crawler fails on website | Site HTML structure may have changed; update site-specific crawler |
| Out of memory during crawling | Increase JVM memory: `export _JAVA_OPTIONS="-Xms512m -Xmx1024m"` (Linux/Mac) or  `SET _JAVA_OPTIONS = -Xms512m -Xmx1024m`(Windows)|
| Tests fail | Check `pom.xml` to ensure dependencies of compatible version |

---

## Contributors & Contact

For issues or contributions, please refer to the project wiki or create an issue in the repository.

**Last Updated**: February 2026


