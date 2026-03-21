// File: src/main/java/com/oop/logistics/crawler/SocialCrawler.java
package com.oop.logistics.crawler;
import java.util.List;

public interface SocialCrawler {
    List<SocialResult> crawlComments(String url, int maxComments);
}