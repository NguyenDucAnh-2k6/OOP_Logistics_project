package com.oop.logistics.analysis;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oop.logistics.models.AnalysisRequest;

public class PythonAnalysisClient implements AnalysisAPI { 
    
    private final String apiUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private static final int BATCH_SIZE = 50; 

    public PythonAnalysisClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60))
            .build();
        this.gson = new Gson();
    }

    @Override
    public String getProviderName() { return "Python FastAPI Backend"; }
    @Override
    public String getConfiguration() { return "URL: " + apiUrl; }
    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl + "/")).GET().build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // --- Problem 1: Sentiment Time Series ---
    @Override
    public List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress) throws Exception {
        validateInputs(texts);
        List<Map<String, Object>> aggregatedResults = new ArrayList<>();
        Map<String, Map<String, Double>> tempMap = new HashMap<>(); 

        processBatches(texts, dates, modelType, onProgress, (batchTexts, batchDates, type) -> {
            try {
                AnalysisRequest req = new AnalysisRequest(batchTexts, batchDates);
                req.setModelType(type);
                String response = sendPost("/analyze/sentiment_timeseries", req);
                List<Map<String, Object>> batchResult = gson.fromJson(response, new TypeToken<ArrayList<Map<String, Object>>>(){}.getType());
                
                for (Map<String, Object> entry : batchResult) {
                    String date = (String) entry.get("date");
                    if (date == null) continue;
                    
                    tempMap.putIfAbsent(date, new HashMap<>(Map.of("positive", 0.0, "negative", 0.0, "neutral", 0.0)));
                    Map<String, Double> current = tempMap.get(date);
                    mergeCounts(current, entry);
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        for (String date : tempMap.keySet()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date);
            entry.putAll(tempMap.get(date));
            aggregatedResults.add(entry);
        }
        
        aggregatedResults.sort((m1, m2) -> compareDates((String) m1.get("date"), (String) m2.get("date")));
        return aggregatedResults;
    }

    // --- Problem 2: Damage Classification ---
    @Override
    public List<String> getDamageClassification(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception {
        validateInputs(texts);
        List<String> allResults = new ArrayList<>();

        processBatches(texts, null, modelType, onProgress, (batchTexts, batchDates, type) -> {
            try {
                AnalysisRequest req = new AnalysisRequest(batchTexts, null);
                req.setModelType(type);
                String response = sendPost("/analyze/damage", req);
                List<String> batchResult = gson.fromJson(response, new TypeToken<ArrayList<String>>(){}.getType());
                allResults.addAll(batchResult);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        return allResults;
    }

    // --- Problem 3: Relief Sentiment ---
    @Override
    public Map<String, Map<String, Double>> getReliefSentiment(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception {
        validateInputs(texts);
        Map<String, Map<String, Double>> finalStats = new HashMap<>();

        processBatches(texts, null, modelType, onProgress, (batchTexts, batchDates, type) -> {
            try {
                AnalysisRequest req = new AnalysisRequest(batchTexts, null);
                req.setModelType(type);
                String response = sendPost("/analyze/relief_sentiment", req);
                Map<String, Map<String, Double>> batchResult = gson.fromJson(response, new TypeToken<Map<String, Map<String, Double>>>(){}.getType());

                for (String category : batchResult.keySet()) {
                    finalStats.putIfAbsent(category, new HashMap<>(Map.of("positive", 0.0, "negative", 0.0, "neutral", 0.0)));
                    Map<String, Double> current = finalStats.get(category);
                    Map<String, Double> incoming = batchResult.get(category);
                    incoming.forEach((senti, val) -> current.merge(senti, val, Double::sum));
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        return finalStats;
    }

    // --- Problem 4: Relief Time Series (FIXED LOGIC) ---
    @Override
    public List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress) throws Exception {
        validateInputs(texts);
        List<Map<String, Object>> aggregatedResults = new ArrayList<>();
        
        // Nested Map: Date -> (Category -> Stats)
        // This avoids string splitting issues entirely
        Map<String, Map<String, Map<String, Double>>> tempMap = new HashMap<>(); 

        processBatches(texts, dates, modelType, onProgress, (batchTexts, batchDates, type) -> {
            try {
                AnalysisRequest req = new AnalysisRequest(batchTexts, batchDates);
                req.setModelType(type);
                String response = sendPost("/analyze/relief_timeseries", req);
                List<Map<String, Object>> batchResult = gson.fromJson(response, new TypeToken<ArrayList<Map<String, Object>>>(){}.getType());

                for (Map<String, Object> entry : batchResult) {
                    String date = (String) entry.get("date");
                    String category = (String) entry.get("category");
                    
                    if (date == null || category == null) continue;

                    // Create Date entry if missing
                    tempMap.putIfAbsent(date, new HashMap<>());
                    Map<String, Map<String, Double>> dateEntry = tempMap.get(date);

                    // Create Category entry if missing
                    dateEntry.putIfAbsent(category, new HashMap<>(Map.of("positive", 0.0, "negative", 0.0, "neutral", 0.0)));
                    
                    // Merge stats
                    mergeCounts(dateEntry.get(category), entry);
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        // Flatten logic
        for (String date : tempMap.keySet()) {
            Map<String, Map<String, Double>> cats = tempMap.get(date);
            for (String category : cats.keySet()) {
                Map<String, Object> flatEntry = new HashMap<>();
                flatEntry.put("date", date);
                flatEntry.put("category", category);
                flatEntry.putAll(cats.get(category));
                aggregatedResults.add(flatEntry);
            }
        }

        // Sort chronologically
        aggregatedResults.sort((m1, m2) -> compareDates((String) m1.get("date"), (String) m2.get("date")));

        return aggregatedResults;
    }

    // --- HELPERS ---

    private void mergeCounts(Map<String, Double> target, Map<String, Object> source) {
        target.put("positive", target.get("positive") + ((Number) source.get("positive")).doubleValue());
        target.put("negative", target.get("negative") + ((Number) source.get("negative")).doubleValue());
        target.put("neutral",  target.get("neutral")  + ((Number) source.get("neutral")).doubleValue());
    }

    private int compareDates(String d1, String d2) {
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        
        DateTimeFormatter fmtVN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            // Try parsing as Vietnamese format first
            return LocalDate.parse(d1, fmtVN).compareTo(LocalDate.parse(d2, fmtVN));
        } catch (DateTimeParseException e1) {
            try {
                // Fallback to ISO format
                return LocalDate.parse(d1, fmtISO).compareTo(LocalDate.parse(d2, fmtISO));
            } catch (DateTimeParseException e2) {
                // If both fail, fallback to string comparison
                return d1.compareTo(d2);
            }
        }
    }

    private interface BatchAction {
        void run(List<String> batchTexts, List<String> batchDates, String type);
    }

    private void processBatches(List<String> texts, List<String> dates, String modelType, Consumer<Double> onProgress, BatchAction action) {
        int total = texts.size();
        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(total, i + BATCH_SIZE);
            List<String> batchTexts = texts.subList(i, end);
            List<String> batchDates = (dates != null) ? dates.subList(i, end) : null;

            action.run(batchTexts, batchDates, modelType);

            double progress = (double) end / total;
            if (onProgress != null) onProgress.accept(progress);
        }
    }

    private void validateInputs(List<String> texts) {
        if (texts == null || texts.isEmpty()) throw new RuntimeException("No texts to analyze");
    }

    private String sendPost(String endpoint, Object requestObject) throws Exception {
        String jsonBody = gson.toJson(requestObject);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error (" + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }
    // --- Problem 5: Intent Classification (Supply vs Demand) ---
    @Override
    public Map<String, Integer> getIntentClassification(List<String> texts, String modelType, Consumer<Double> onProgress) throws Exception {
        validateInputs(texts);
        
        // Initialize the tracking map with zeroes
        Map<String, Integer> finalStats = new HashMap<>(Map.of("Request", 0, "Offer", 0, "News", 0));

        processBatches(texts, null, modelType, onProgress, (batchTexts, batchDates, type) -> {
            try {
                AnalysisRequest req = new AnalysisRequest(batchTexts, null);
                req.setModelType(type);
                
                // Call the new Python endpoint
                String response = sendPost("/analyze/intent", req);
                Map<String, Integer> batchResult = gson.fromJson(response, new TypeToken<Map<String, Integer>>(){}.getType());

                // Merge the batch counts into the total
                for (Map.Entry<String, Integer> entry : batchResult.entrySet()) {
                    finalStats.put(entry.getKey(), finalStats.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });

        return finalStats;
    }
}