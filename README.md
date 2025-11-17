# OOP_Logistics_project
This project collects data from Internet platforms (Facebook/Twitter/Tiktok/Youtube) -> process/analyse -> solve 2-3 problems 
Proposed pipeline: (for YouTube) <br>
```/data_pipeline 
    /collector
        YoutubeCollector.java
    /preprocessor
        TextCleaner.java
    /analyzers
        SentimentTimeSeriesAnalyzer.java
    /models
        SentimentAPI.java     // gọi API Python hoặc mô hình Java
    /app
        MainApp.java```


