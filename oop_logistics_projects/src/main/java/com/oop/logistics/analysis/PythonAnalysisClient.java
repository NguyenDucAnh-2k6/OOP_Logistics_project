package com.oop.logistics.analysis;

import com.google.gson.Gson;
import com.oop.logistics.models.AnalysisRequest;
import com.oop.logistics.models.AnalysisResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for communicating with Python-based sentiment analysis API
 */
public class PythonAnalysisClient implements AnalysisAPI {
    
    private final String apiUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private final int timeoutSeconds;
    
    public PythonAnalysisClient(String apiUrl) {
        this(apiUrl, 30);
    }
    
    public PythonAnalysisClient(String apiUrl, int timeoutSeconds) {
        this.apiUrl = apiUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
        this.gson = new Gson();
    }
    
    @Override
    public AnalysisResponse analyzeSentiment(AnalysisRequest request) {
        try {
            String jsonRequest = gson.toJson(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(new URI(apiUrl + "/analyze"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                httpRequest, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), AnalysisResponse.class);
            } else {
                AnalysisResponse errorResponse = new AnalysisResponse();
                errorResponse.setError("API returned status code: " + response.statusCode());
                return errorResponse;
            }
            
        } catch (Exception e) {
            AnalysisResponse errorResponse = new AnalysisResponse();
            errorResponse.setError("Failed to connect to analysis API: " + e.getMessage());
            return errorResponse;
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "Python Sentiment Analysis API";
    }
    
    @Override
    public String getConfiguration() {
        return String.format("API URL: %s, Timeout: %ds", apiUrl, timeoutSeconds);
    }
    
    /**
     * Batch analyze multiple texts
     */
    public AnalysisResponse analyzeBatch(java.util.List<String> texts) {
        AnalysisRequest request = new AnalysisRequest(texts);
        return analyzeSentiment(request);
    }
}