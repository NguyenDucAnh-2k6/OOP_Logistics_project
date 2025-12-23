package com.oop.logistics.gem_crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class NewsCrawler {

    protected static final String OUTPUT = "YagiNews.csv";

    protected void writeCsv(String date, String text) {
        try {
            File f = new File(OUTPUT);
            boolean needHeader = !f.exists();
            try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT, true))) {
                if (needHeader) {
                    writer.println("Date,Text");
                }
                writer.println(csvEscape(date) + "," + csvEscape(text));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String csvEscape(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public abstract void crawl(String url);
}
