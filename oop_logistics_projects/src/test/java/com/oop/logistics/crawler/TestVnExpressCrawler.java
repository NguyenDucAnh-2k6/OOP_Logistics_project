package com.oop.logistics.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("VnExpressCrawler Tests")
class TestVnExpressCrawler {

    @Test
    @DisplayName("Should parse article correctly and return NewsResult")
    void testCrawlSuccess() throws Exception {
        // 1. Arrange: Create a real JSoup Document from a fake HTML string
        String fakeHtml = """
            <html>
                <head>
                    <title>Bão Yagi tàn phá</title>
                    <meta name="pubdate" content="2024-09-07T15:23:00Z">
                </head>
                <body>
                    <article class="fck_detail">
                        <p>Đoạn văn 1 dài hơn 50 ký tự để vượt qua điều kiện kiểm tra của crawler.</p>
                        <p>Đoạn văn 2 cung cấp thêm thông tin về cơn bão tại Hà Nội.</p>
                    </article>
                </body>
            </html>
            """;
        Document fakeDoc = Jsoup.parse(fakeHtml);

        // 2. Arrange: Mock the Jsoup Connection chain
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
        when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
        when(mockConnection.get()).thenReturn(fakeDoc); // Return our fake document!

        // 3. Act: Intercept static Jsoup.connect()
        try (MockedStatic<Jsoup> mockedJsoup = mockStatic(Jsoup.class)) {
            mockedJsoup.when(() -> Jsoup.connect("https://vnexpress.net/fake-news")).thenReturn(mockConnection);

            VnExpressCrawler crawler = new VnExpressCrawler();
            NewsResult result = crawler.crawl("https://vnexpress.net/fake-news");

            // 4. Assert - Accessing the public final fields directly
            assertNotNull(result, "Crawler should return a valid NewsResult");
            assertEquals("Bão Yagi tàn phá", result.title, "Title should match");
            assertEquals("2024-09-07T15:23:00Z", result.date, "Date should match");
            assertTrue(result.text.contains("Đoạn văn 1 dài hơn 50 ký tự"), "Text should contain the paragraph content");
        }
    }
}