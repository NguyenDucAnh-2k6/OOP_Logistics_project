package com.oop.logistics.scraping;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class FacebookCrawler {

    public static void main(String[] args) {
        String url = "https://www.facebook.com/watch/?v=531119789577807";

        try {
            // Simulate Chrome browser to avoid FB redirecting to login
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                             + "AppleWebKit/537.36 (KHTML, like Gecko) "
                             + "Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(10_000)
                    .method(Connection.Method.GET)
                    .followRedirects(true);

            Document doc = connection.get();

            System.out.println("► Page Title:");
            System.out.println(doc.title());

            System.out.println("\n► og:title (if exists):");
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) System.out.println(ogTitle.attr("content"));

            System.out.println("\n► og:description (if exists):");
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null) System.out.println(ogDesc.attr("content"));

            // Extract embedded JSON (Facebook stores all data here)
            System.out.println("\n► Extracting JSON embedded in <script> tags...");
            for (Element script : doc.select("script")) {
                if (script.data().contains("video_id") || script.data().contains("graphql")) {
                    System.out.println("\n--- JSON Found ---");
                    System.out.println(script.data());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching page. Facebook may require login cookies.");
        }
    }
}

