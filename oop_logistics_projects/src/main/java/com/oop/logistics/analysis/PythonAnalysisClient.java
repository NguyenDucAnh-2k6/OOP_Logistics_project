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
import com.oop.logistics.models.AnalysisRequest;

public class PythonAnalysisClient implements AnalysisAPI { 
    
    private final String apiUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public PythonAnalysisClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60)) // Increased timeout for AI models
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
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/"))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // --- Problem 1: Sentiment Time Series ---
    @Override
    public List<Map<String, Object>> getSentimentTimeSeries(List<String> texts, List<String> dates, String modelType) throws Exception {
        if (texts == null || texts.isEmpty()) throw new RuntimeException("No texts to analyze");
        
        AnalysisRequest req = new AnalysisRequest(texts, dates);
        req.setModelType(modelType); // Pass the selection
        
        String response = sendPost("/analyze/sentiment_timeseries", req);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // --- Problem 2: Damage Classification ---
    @Override
    public List<String> getDamageClassification(List<String> texts, String modelType) throws Exception {
        if (texts == null || texts.isEmpty()) throw new RuntimeException("No texts to analyze");

        AnalysisRequest req = new AnalysisRequest(texts, null);
        req.setModelType(modelType); // Pass the selection

        String response = sendPost("/analyze/damage", req);
        Type listType = new TypeToken<ArrayList<String>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // --- Problem 3: Relief Sentiment ---
    @Override
    public Map<String, Map<String, Double>> getReliefSentiment(List<String> texts, String modelType) throws Exception {
        if (texts == null || texts.isEmpty()) throw new RuntimeException("No texts to analyze");

        AnalysisRequest req = new AnalysisRequest(texts, null);
        req.setModelType(modelType); // Pass the selection

        String response = sendPost("/analyze/relief_sentiment", req);
        Type mapType = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();
        return gson.fromJson(response, mapType);
    }

    // --- Problem 4: Relief Time Series ---
    @Override
    public List<Map<String, Object>> getReliefTimeSeries(List<String> texts, List<String> dates, String modelType) throws Exception {
        if (texts == null || texts.isEmpty()) throw new RuntimeException("No texts to analyze");

        AnalysisRequest req = new AnalysisRequest(texts, dates);
        req.setModelType(modelType); // Pass the selection

        String response = sendPost("/analyze/relief_timeseries", req);
        Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // --- Helper ---
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
}