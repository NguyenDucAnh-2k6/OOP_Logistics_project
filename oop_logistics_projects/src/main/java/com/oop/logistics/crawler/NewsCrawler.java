package com.oop.logistics.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public abstract class NewsCrawler {

    // Now returns a NewsResult instead of void
    public abstract NewsResult crawl(String url);

    protected String getMetaContent(Document doc, String propertyName) {
        Element meta = doc.selectFirst("meta[property='" + propertyName + "']");
        if (meta == null) {
            meta = doc.selectFirst("meta[name='" + propertyName + "']");
        }
        if (meta == null) {
            meta = doc.selectFirst("meta[itemprop='" + propertyName + "']");
        }
        return meta != null ? meta.attr("content") : null;
    }
}