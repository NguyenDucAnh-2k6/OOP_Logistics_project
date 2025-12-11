package com.oop.logistics.analysis;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;  // Required for Interface
import java.net.http.HttpRequest; // Required for Interface
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oop.logistics.models.AnalysisRequest;
import com.oop.logistics.models.AnalysisResponse;

/**
 * Client for interacting with the Python Sentiment Analysis API.
 * Implements AnalysisAPI to support the core pipeline, 
 * and provides specialized methods for the JavaFX GUI.
 */
public class PythonAnalysisClient implements AnalysisAPI { 
    
    private final String apiUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public PythonAnalysisClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
    }

    // ==================================================================================
    //  PART 1: AnalysisAPI Interface Implementation (For DisasterLogisticsPipeline)
    // ==================================================================================

    @Override
    public String getProviderName() {
        return "Python FastAPI Backend";
    }

    @Override
    public String getConfiguration() {
        return "URL: " + apiUrl;
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiUrl + "/docs")) // Check if docs exist as health check
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AnalysisResponse analyzeSentiment(AnalysisRequest request) {
        // This method is called by DisasterAnalyzer in a loop.
        // We will map it to the /damage-classification endpoint or similar for a single result.
        // Since the Python API is designed for batches, this is a simplified adapter.
        
        AnalysisResponse response = new AnalysisResponse();
        try {
            // Adapt single request to list format for Python
            List<String> texts = Collections.singletonList(request.getText());
            List<String> results = getDamageClassification(texts); // Reusing existing method
            
            if (!results.isEmpty()) {
                response.setSentiment(results.get(0)); // Set result
                response.setConfidence(1.0);
            } else {
                response.setSentiment("Neutral");
            }
        } catch (Exception e) {
            response.setError("API Error: " + e.getMessage());
        }
        return response;
    }

    // ==================================================================================
    //  PART 2: Core HTTP Logic
    // ==================================================================================

    private String sendPost(String endpoint, Object payload) throws Exception {
        String json = gson.toJson(payload);
        
        // DEBUG: Print payload to console
        // System.out.println("DEBUG POST " + endpoint + ": " + (json.length() > 100 ? json.substring(0, 100) + "..." : json));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUrl + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    // ==================================================================================
    //  PART 3: GUI Specific Methods (Problem 1, 2, 3, 4)
    // ==================================================================================

    public List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        payload.put("dates", dates);
        
        String response = sendPost("/sentiment-time-series", payload);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    public List<String> getDamageClassification(List<String> texts) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        
        String response = sendPost("/damage-classification", payload);
        Type listType = new TypeToken<ArrayList<String>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    public Map<String, Map<String, Double>> getReliefSentiment(List<String> texts) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        
        String response = sendPost("/relief-sentiment", payload);
        Type mapType = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();
        return gson.fromJson(response, mapType);
    }

    public List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        payload.put("dates", dates);
        
        String response = sendPost("/relief-time-series", payload);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }
}