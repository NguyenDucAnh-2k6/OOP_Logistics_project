package com.oop.logistics.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Request model for analysis APIs
 */
public class AnalysisRequest {
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("texts")
    private List<String> texts;
    
    @SerializedName("language")
    private String language; // "en", "vi", etc.
    
    @SerializedName("model")
    private String model; // specific model to use
    
    @SerializedName("parameters")
    private Map<String, Object> parameters;
    
    public AnalysisRequest() {}
    
    public AnalysisRequest(String text) {
        this.text = text;
        this.language = "vi"; // default Vietnamese
    }
    
    public AnalysisRequest(List<String> texts) {
        this.texts = texts;
        this.language = "vi";
    }
    
    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public List<String> getTexts() { return texts; }
    public void setTexts(List<String> texts) { this.texts = texts; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}

/**
 * Response model from analysis APIs
 */
