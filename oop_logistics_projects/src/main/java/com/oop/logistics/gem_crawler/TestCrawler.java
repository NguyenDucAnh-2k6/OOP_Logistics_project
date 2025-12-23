package com.oop.logistics.gem_crawler;

import com.oop.logistics.preprocessing.DateExtract;

public class TestCrawler {
    public static void main(String[] args) {
        // REPLACE WITH YOUR ACTUAL COOKIES
        String c_user = "100072127640070"; 
        String xs = "33%3AFPPwpYBhFDPmmA%3A2%3A1766414788%3A-1%3A-1%3A%3AAcxIqzlgc2adaH0j9-jEp0KzNEer7mTFk-ZRXBCcjg";
        String fr = "1mvUL7BdsNoWsORQF.AWeI_cWeyM4kBF_bFjG369nVQSV1eXjb88pZkGt4xsPMOl0laPU.BpSVnH..AAA.0.0.BpSVnH.AWe7mCnBx550GSSGkXfWYnKlqGM"; 

        FacebookCrawler crawler = new FacebookCrawler();

        try {
            // Capture the current date when crawling starts
            String crawlDate = DateExtract.getCurrentDateDDMMYYYY();
            String crawlDateTime = DateExtract.getCurrentDateTime();
            
            System.out.println("🕐 Crawl Date (dd/mm/yyyy): " + crawlDate);
            System.out.println("🕐 Crawl Start Time: " + crawlDateTime);
            
            crawler.loginWithCookies(c_user, xs, fr);
            
            // The specific normal post requested
            String targetUrl = "https://www.facebook.com/share/p/168CfJJwLm/";
            
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