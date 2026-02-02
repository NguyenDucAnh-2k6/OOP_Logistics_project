# Exception Handling

## Strategy and Principles

- Centralize exception translation at API boundaries (controllers) so internal exceptions map to appropriate HTTP responses.
- Prefer specific exceptions and avoid catching `Exception` unless re-wrapping for a defined response.
- Use custom exception types to represent domain errors: `DataAccessException`, `AnalysisException`, `ServiceException`.
- Always log exceptions with context (request id, parameters) and avoid leaking internal details to API clients.

## Common Exception Types by Area

- I/O and File Parsing
  - `IOException` / `FileNotFoundException`: reading CSV or config files.
  - CSV parsing errors: wrap in `DataAccessException` or `ParseException` with row metadata.

- Network / External Services
  - `IOException`, `TimeoutException`: network failures when calling external APIs or the Python service.
  - `InterruptedException`: thread interrupt when awaiting remote responses.

- PythonAnalysisClient
  - Handle remote call failures, timeouts, and protocol errors. Wrap into `AnalysisException` with retry logic where appropriate.

- Analysis / Business Logic
  - `IllegalArgumentException` for invalid inputs.
  - `NullPointerException` prevention via validations; if thrown, wrap and log with full context.

- Concurrency
  - `ExecutionException` / `RejectedExecutionException` from thread pools; ensure graceful degradation or fallback policies.

## Recommended Handling Patterns (Java)

- Use try-with-resources for streams/readers to ensure closure.

  try (BufferedReader r = Files.newBufferedReader(path)) {
      // parse
  } catch (IOException e) {
      throw new DataAccessException("Failed reading data file: " + path, e);
  }

- Wrap third-party exceptions into domain exceptions at the module boundary:

  try {
      pythonClient.runAnalysis(payload);
  } catch (IOException | TimeoutException e) {
      throw new AnalysisException("Remote analysis failed", e);
  }

- In controllers (API layer) map domain exceptions to HTTP responses:

  - `DataAccessException` -> 503 Service Unavailable (or 500 depending on cause)
  - `AnalysisException` -> 502 Bad Gateway or 500
  - `IllegalArgumentException` -> 400 Bad Request
  - `AuthenticationException` -> 401 Unauthorized

## Logging and Observability

- Log full stack traces server-side with contextual fields (request id, user id, input snippet).
- Emit metrics on exception rates per endpoint and per exception type.

## Retry and Backoff

- For transient network errors (Python service, external APIs), use exponential backoff with a capped retry count. Circuit breaker patterns are recommended for repeated failures.

## Tests and Validation

- Unit tests should assert that modules convert and rethrow exceptions into the expected domain exceptions.
- Integration tests should simulate failures (network, parsing) and verify API-level responses and logging.
