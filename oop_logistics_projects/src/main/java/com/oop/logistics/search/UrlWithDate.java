package com.oop.logistics.search;

import java.time.LocalDate;
import java.util.Objects;

public class UrlWithDate {
    private final String url;
    private final LocalDate date;

    public UrlWithDate(String url, LocalDate date) {
        this.url = url;
        this.date = date;
    }

    public String getUrl() { return url; }
    public LocalDate getDate() { return date; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlWithDate that = (UrlWithDate) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}