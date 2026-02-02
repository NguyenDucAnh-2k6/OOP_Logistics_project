package com.oop.logistics.search;

import java.util.Map;

public interface SearchStrategy {
    void search(String domain, String keyword, Map<String, UrlWithDate> results);
}