package com.oop.logistics.analysis;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

    private String sendPost(String endpoint, Object payload) throws Exception {
        String json = gson.toJson(payload);
        
        // DEBUG: Print payload (Note: Windows console might still show ? here, but that's just display)
        System.out.println("DEBUG POST " + endpoint + ": " + (json.length() > 200 ? json.substring(0, 200) + "..." : json));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUrl + endpoint))
            // 1. Explicitly state charset in the header
            .header("Content-Type", "application/json; charset=utf-8")
            // 2. Explicitly encode the String bytes as UTF-8
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) 
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    // --- PROBLEM 1: Sentiment Trends ---
    // REMOVED try-catch, added 'throws Exception'
    public List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        payload.put("dates", dates);
        
        String response = sendPost("/sentiment-time-series", payload);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // --- PROBLEM 2: Damage Classification ---
    public List<String> getDamageClassification(List<String> texts) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        
        String response = sendPost("/damage-classification", payload);
        Type listType = new TypeToken<ArrayList<String>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // --- PROBLEM 3: Relief Sentiment ---
    public Map<String, Map<String, Double>> getReliefSentiment(List<String> texts) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        
        String response = sendPost("/relief-sentiment", payload);
        Type mapType = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();
        return gson.fromJson(response, mapType);
    }

    // --- PROBLEM 4: Relief Time Series ---
    public List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("texts", texts);
        payload.put("dates", dates);
        
        String response = sendPost("/relief-time-series", payload);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }
}