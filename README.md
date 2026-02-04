# OOP Logistics Project

A comprehensive data collection, preprocessing, and analysis platform for humanitarian logistics and disaster response. The system aggregates data from multiple Vietnamese news platforms, performs advanced text analysis including sentiment trends, damage classification, and relief sentiment analysis.

## Table of Contents
- [Project Overview](#project-overview)
- [Quick Start](#quick-start)
- [System Architecture](#system-architecture)
- [OOP Design Principles](#oop-design-principles)
- [Dependencies & Libraries](#dependencies--libraries)
- [Exception Handling](#exception-handling)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)

---

## Project Overview

This application solves three key problems for humanitarian logistics:

1. **Problem 1: Sentiment Time Series Analysis** - Track sentiment trends over time to identify community needs
2. **Problem 2: Damage Classification** - Automatically categorize disaster damage by type (infrastructure, people, etc.)
3. **Problem 3: Relief Sentiment Aggregation** - Analyze sentiment around relief efforts and resource allocation

The system combines:
- **Java backend**: Data crawling, preprocessing, UI management
- **Python ML services**: Deep learning models for sentiment and damage analysis
- **JavaFX GUI**: User-friendly desktop application interface

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
python main.py
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
┌─────────────────────────┐
│  Python FastAPI Backend │
│ - Sentiment Analysis    │
│ - Damage Classification │
│ - Relief Sentiment      │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  JavaFX UI              │
│ - DisasterFXApp         │
│ - Results Visualization │
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
    try:
        return aggregate_by_date(req.texts, req.dates, req.model_type)
    except Exception as e:
        print(f"❌ Error: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})
```

**Strategy**: Pydantic validates requests; exceptions return HTTP 500 with error details to Java client.

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
python main.py
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
mvn javafx:run
# OR compile and run JAR:
mvn package
java -jar target/oop_logistics_projects-1.0-SNAPSHOT.jar
```

#### Step 5: Use the GUI
- Click "Crawl News" to collect data from Vietnamese news sources
- Select preprocessing options (remove duplicates, extract dates, etc.)
- Run analysis (Sentiment, Damage Classification, Relief Sentiment)
- View results and export to CSV

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
  "Infrastructure": ["building", "road", "bridge", "power"],
  "People": ["injury", "death", "missing"],
  "Environment": ["flooding", "landslide", "fire"]
}
```

#### `external config/relief_keywords.json`
```json
{
  "Medical": ["hospital", "medicine", "doctor"],
  "Food": ["rice", "water", "supplies"],
  "Shelter": ["tent", "housing", "evacuation"]
}
```

#### `external config/sentiment_keywords.json`
```json
{
  "Positive": ["help", "hope", "support"],
  "Negative": ["crisis", "disaster", "tragedy"]
}
```

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


