package com.oop.logistics.crawler;

import java.io.FileWriter;
import java.io.PrintWriter;

public class CsvWriter {

    private static final String FILE = "YagiNews.csv";

    public static void append(NewsResult r) throws Exception {

        try (PrintWriter w = new PrintWriter(new FileWriter(FILE, true))) {

            w.println(
                escape(r.date) + "," +
                escape(r.text)
            );
        }
    }

    private static String escape(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
