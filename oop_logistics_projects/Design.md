# Design Overview

## Purpose
This document summarizes key design decisions and patterns used across the OOP Logistics project: abstract classes, interfaces, polymorphism, inheritance, generic programming, factory pattern, and an MVC mapping for backend/frontend components.

## Core Concepts

- **Abstract Class vs Interface**
  - Use abstract classes when a base implementation is useful (shared fields or partial behavior). Example: an abstract `DataSource` in `com.oop.logistics.core` provides common fields and utilities for concrete data sources.
  - Use interfaces to define capabilities (e.g., `Preprocessor`, `Analyzer`) so multiple unrelated classes can implement them.

- **Inheritance and Polymorphism**
  - Concrete data source classes (e.g., `TwitterDataSource`, `FacebookDataSource`) inherit from a common `DataSource` base and override fetch/parse behavior.
  - Polymorphism allows higher-level components (pipeline, search service) to depend on `DataSource` or `Analyzer` types and operate on any implementation.

- **Generic Programming**
  - Use generics for reusable components like repositories or converters: e.g., `DataRepository<T>` or `CsvParser<T>` to parse into different model types while keeping parsing logic generic and type-safe.

- **Factory Pattern**
  - Factories centralize construction logic, especially when instantiation depends on configuration (`datasources.json`) or runtime parameters.
  - Example: `DataSourceFactory.create(String type, Config cfg)` returns an appropriate `DataSource` implementation. This keeps construction code out of business logic and makes adding new providers simple.

## Patterns and Examples (pseudocode)

- Factory usage (conceptual):

  DataSource ds = DataSourceFactory.create(config.get("type"), config);

- Generic parser (conceptual):

  class CsvParser<T> {
      T parseRow(Map<String,String> row, Class<T> cls) { ... }
  }

## MVC Mapping (Backend / Frontend)

- **Model**
  - `com.oop.logistics.models.*`: domain objects (news items, categories, keywords). Also persisted or cached data.

- **View**
  - The `ui` package contains presentation logic used by any local UI modules.
  - For an HTTP/REST frontend, REST endpoints (e.g., `AnalysisAPI.java`) output JSON views.

- **Controller**
  - `AnalysisAPI` and other API classes act as controllers: accept requests, validate input, call services, and return responses.

## Backend Components

- **API Layer**: `AnalysisAPI.java` — REST entrypoints, request validation, response mapping.
- **Service Layer**: `analysis`, `core` packages — pipeline coordination, business logic (e.g., `DisasterLogisticsPipeline`, `DisasterSearchService`).
- **Data Layer**: `gem_crawler`, `DataCollector`, `DataSource` implementations — ingest and normalize raw data.
- **Preprocessing & Models**: `preprocessing`, `models` packages — text normalization, feature extraction.
- **External ML**: `PythonAnalysisClient` — delegates heavy ML/sentiment work to Python microservices.

## Frontend Components

- **UI Package**: local UI components or simple web views.
- **Client**: External clients call `AnalysisAPI` or the python-api endpoints.

## Extensibility & Tests

- Keep interfaces small and focused; prefer composition over deep inheritance. Add unit tests for factories and generic parsers to ensure type-safety and correct wiring.

## Summary

Design choices favor small, testable components wired via interfaces and factories. Use polymorphism to swap providers and generics for reusable parsing/persistence utilities.
