package com.oop.logistics.crawler;

import java.util.ArrayList;
import java.util.List;

public class FacebookResult {
    public String content = "";
    
    // Store structured data instead of just strings
    public List<CommentData> comments = new ArrayList<>();

    public static class CommentData {
        public String author;
        public String text;
        public String date; // We will normalize this to dd/MM/yyyy

        public CommentData(String author, String text, String date) {
            this.author = author;
            this.text = text;
            this.date = date;
        }
    }
}