package com.oop.logistics.analysis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PythonAnalysisClient {
    
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

    // Helper to send POST requests
    private String sendPost(String endpoint, Object payload) throws Exception {
        String json = gson.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUrl + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    // --- PROBLEM 1: Sentiment Trends ---
    public List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("texts", texts);
            payload.put("dates", dates);
            
            String response = sendPost("/sentiment-time-series", payload);
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // --- PROBLEM 2: Damage Classification ---
    public List<String> getDamageClassification(List<String> texts) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("texts", texts);
            
            String response = sendPost("/damage-classification", payload);
            Type listType = new TypeToken<List<String>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // --- PROBLEM 3: Relief Sentiment (Satisfaction) ---
    public Map<String, Map<String, Double>> getReliefSentiment(List<String> texts) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("texts", texts);
            
            String response = sendPost("/relief-sentiment", payload);
            Type mapType = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();
            return gson.fromJson(response, mapType);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    // --- PROBLEM 4: Relief Time Series ---
    public List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("texts", texts);
            payload.put("dates", dates);
            
            String response = sendPost("/relief-time-series", payload);
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}