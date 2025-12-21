package com.oop.logistics.gpt_crawler;
public class Comment {

    public String author;
    public String text;
    public int level; // 0 = top-level comment, 1 = reply

    public Comment(String author, String text, int level) {
        this.author = author;
        this.text = text;
        this.level = level;
    }
}

