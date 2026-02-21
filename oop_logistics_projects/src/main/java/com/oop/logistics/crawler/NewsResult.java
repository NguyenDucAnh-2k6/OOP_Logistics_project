package com.oop.logistics.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewsResult {
    private static final Logger logger = LoggerFactory.getLogger(NewsResult.class);

    public final String url;
    public final String title;
    public final String date;
    public final String text;

    public NewsResult(String url, String title, String date, String text) {
        this.url = url;
        this.title = title;
        this.date = date;
        this.text = text;
    }
}