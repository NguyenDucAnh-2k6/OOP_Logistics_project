package com.oop.logistics.gem_crawler;

import com.oop.logistics.preprocessing.DateExtract;

public class TestCrawler {
    public static void main(String[] args) {
        // REPLACE WITH YOUR ACTUAL COOKIES
        String c_user = "100072127640070"; 
        String xs = "1%3A4gKRYOhELNt_5A%3A2%3A1766494665%3A-1%3A-1%3A%3AAczavsPFjtFKvvGavjXdn9Dq5VceFdtSkrtdVsb4qg0";
        String fr = "1N3x6QOYTpbRxmIoA.AWeIhtXEwsqK2Uy4VdT9DzxIHRcAlfFIzy55u6mMyzUqRhYF1RI.BpS6U-..AAA.0.0.BpS6U-.AWcIf_sEUglsiy0Zu5Y1pZ2Xn_A"; 

        FacebookCrawler crawler = new FacebookCrawler();

        try {
            // Capture the current date when crawling starts
            String crawlDate = DateExtract.getCurrentDateDDMMYYYY();
            String crawlDateTime = DateExtract.getCurrentDateTime();
            
            System.out.println("🕐 Crawl Date (dd/mm/yyyy): " + crawlDate);
            System.out.println("🕐 Crawl Start Time: " + crawlDateTime);
            
            crawler.loginWithCookies(c_user, xs, fr);
            
            // The specific normal post requested
            String targetUrl = "https://www.facebook.com/share/p/1Bvxovtx8L/";
            
            System.out.println("📍 Target URL: " + targetUrl);
            System.out.println("⏳ Starting crawl...");
            
            // Set the crawl date for all comments extracted from this post
            crawler.setCrawlDate(crawlDate);
            crawler.crawl(targetUrl);
            
            System.out.println("✅ Crawl complete! All comments assigned date: " + crawlDate);
            System.out.println("📁 Output saved to YagiComments.csv");
            
        } catch (Exception e) {
            System.err.println("❌ Error during crawl:");
            e.printStackTrace();
        } finally {
            crawler.tearDown();
        }
    }
}