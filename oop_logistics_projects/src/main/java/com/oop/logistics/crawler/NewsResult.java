package com.oop.logistics.crawler;

public class NewsResult {
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