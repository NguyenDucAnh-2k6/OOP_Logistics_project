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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oop.logistics.models.AnalysisRequest;
import com.oop.logistics.models.AnalysisResponse;

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

    // ... (Keep existing Interface methods: getProviderName, isAvailable, etc.) ...
    @Override
    public String getProviderName() { return "Python FastAPI Backend"; }
    @Override
    public String getConfiguration() { return "URL: " + apiUrl; }
    @Override
    public boolean isAvailable() { return true; } // Simplified for speed
    @Override
    public AnalysisResponse analyzeSentiment(AnalysisRequest request) { return new AnalysisResponse(); }

    // --- CORE HTTP LOGIC ---
    private String sendPost(String endpoint, Object payload) throws Exception {
        String json = gson.toJson(payload);
        
        // FIX 2: Convert to bytes manually to ensure UTF-8 before transmission
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // DEBUG: Verify clean output
        System.out.println("DEBUG POST " + endpoint + " Payload size: " + jsonBytes.length + " bytes");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUrl + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("JSON SENT:");
        System.out.println(json);
        if (response.statusCode() != 200) {
            System.err.println("API ERROR: " + response.statusCode());
            System.err.println("BODY: " + response.body());
            throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }

    // --- GUI Methods (Keep these exactly as they were) ---
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