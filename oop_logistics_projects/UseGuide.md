# Usage Guide

## Quick Start

- Build the Java project with Maven:

  mvn clean package

- Run tests:

  mvn test

- Start the API (Linux/macOS):

  ./scripts/start-api.sh

On Windows, run the equivalent start command via the `scripts` folder or run the main class from your IDE.

## Configuration

- `config.properties` — main application properties. Set environment-specific values here.
- `external config/datasources.json` — configure which data sources to enable.
- `external config/categories.json` and `keywords.json` — category and keyword definitions used by analyzers.

## Data

- Example data files are present under `target/classes`: `YagiNews.csv` and `YagiNews_normalized.csv`.

## Python ML Service

- The project includes a lightweight Python analysis service in `python-api/`. To run it:

  cd python-api
  pip install -r requirements.txt
  python sentiment-api.py

This service is contacted by `PythonAnalysisClient` from Java for advanced analysis tasks.

## Common Workflows

- To add a new data source:
  - Implement a `DataSource` subclass in `src/main/java/com/oop/logistics/core` or relevant package.
  - Register it in `external config/datasources.json` or update the factory.
  - Add unit tests under `test/java/...`.

- To add a new analyzer:
  - Implement a class conforming to the `Analyzer` interface (or extend base analyzer).
  - Wire the analyzer into the pipeline or register via factory/config.

## Where to Look

- API entry points: `src/main/java/com/oop/logistics/analysis/AnalysisAPI.java`
- Core pipeline: `src/main/java/com/oop/logistics/core/DisasterLogisticsPipeline.java`
- Data sources: `src/main/java/com/oop/logistics/core` and `gem_crawler` packages
- Python analysis client: `src/main/java/com/oop/logistics/analysis/PythonAnalysisClient.java`
