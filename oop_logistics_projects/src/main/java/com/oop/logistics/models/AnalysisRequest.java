package com.oop.logistics.models;

import java.util.List;

public class AnalysisRequest {
    private List<String> texts;
    private List<String> dates;
    private String model_type; // Exact name matching Python Pydantic model

    public AnalysisRequest(List<String> texts, List<String> dates) {
        this.texts = texts;
        this.dates = dates;
        this.model_type = "ai"; // Default
    }

    public void setModelType(String type) {
        this.model_type = type;
    }

    public List<String> getTexts() { return texts; }
    public List<String> getDates() { return dates; }
    public String getModelType() { return model_type; }
}