package com.oop.logistics.gem_crawler;
public class TestCrawler {
    public static void main(String[] args) {
        // REPLACE WITH YOUR ACTUAL COOKIES
        String c_user = "100072127640070"; 
        String xs = "3%3ANyr3fedOJFY6tQ%3A2%3A1766323433%3A-1%3A-1%3A%3AAcwH2O-vuYhtrAATJjz7i_dFS6If_EfpsgRJtOGsTg";
        String fr = "1JCfftbq4kJFOYDjE.AWd4ZlxGlecX8zB04s3KLJ1Ace2H09gXahR64cvnHBqZt_OIJFI.BpR_Ts..AAA.0.0.BpR_Ts.AWdnlh-GI4n2CMqV_b-3SaNpGN8"; 

        FacebookCrawler crawler = new FacebookCrawler();

        try {
            crawler.loginWithCookies(c_user, xs, fr);
            
            // The specific normal post requested
            String targetUrl = "https://www.facebook.com/share/r/17yUsfcYXA/";
            
            crawler.crawl(targetUrl);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            crawler.tearDown();
        }
    }
}