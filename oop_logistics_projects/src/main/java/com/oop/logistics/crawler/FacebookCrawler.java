package com.oop.logistics.crawler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.*;

public class FacebookCrawler {

    private final ChromeDriver driver;

    public FacebookCrawler(String chromeDriverPath) {

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    // -------------------------------------------------------
    // ĐĂNG NHẬP BẰNG COOKIE, KHÔNG DÙNG DEVTOOLS
    // -------------------------------------------------------
    public void loginWithCookies(Map<String, String> cookies) throws InterruptedException {

        driver.get("https://facebook.com");
        Thread.sleep(2000);

        System.out.println(">>> Adding cookies...");

        for (Map.Entry<String, String> entry : cookies.entrySet()) {

            Cookie ck = new Cookie.Builder(entry.getKey(), entry.getValue())
                    .domain(".facebook.com")
                    .path("/")
                    .isSecure(true)
                    .isHttpOnly(true)
                    .build();

            try {
                driver.manage().addCookie(ck);
            }
            catch (Exception ex) {
                System.out.println("Failed to add cookie: " + entry.getKey());
            }
        }

        System.out.println(">>> Cookies injected, refreshing...");
        driver.navigate().refresh();
        Thread.sleep(3000);

        System.out.println(">>> Login OK");
        System.out.println("URL after login: " + driver.getCurrentUrl());
    }


    // -------------------------------------------------------
    // SCROLL CHẬM, KHÔNG BỎ LỠ COMMENTS
    // -------------------------------------------------------
    private void slowScroll(int times) throws InterruptedException {
        JavascriptExecutor js = driver;

        for (int i = 0; i < times; i++) {
            js.executeScript("window.scrollBy(0, 500);");
            System.out.println("Scrolling... step " + (i + 1));
            Thread.sleep(1200);
        }
    }


    // -------------------------------------------------------
    // CLICK TẤT CẢ LOẠI “Xem thêm bình luận”
    // -------------------------------------------------------
    private void expandAllComments() throws InterruptedException {

        String[] selectors = new String[]{
                "//span[text()='Xem thêm bình luận']",
                "//span[contains(text(),'Xem thêm câu trả lời')]",
                "//span[contains(text(),'Xem thêm')]",
                "//div[@role='button']//span[contains(text(),'thêm')]",
                "//div[@role='button']//span[contains(text(),'Bình luận')]",
                "//div[@role='button']//span[contains(text(),'Xem')]"
        };

        for (int round = 0; round < 6; round++) {

            System.out.println(">>> Clicking buttons round " + (round + 1));

            for (String xp : selectors) {
                List<WebElement> btns = driver.findElements(By.xpath(xp));

                for (WebElement b : btns) {
                    try {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
                        Thread.sleep(800);
                    }
                    catch (Exception ignored) {}
                }
            }

            Thread.sleep(1200);
        }
    }


    // -------------------------------------------------------
    // CRAWL COMMENTS + DATE
    // -------------------------------------------------------
    public List<Map<String, String>> crawlComments(String postUrl) throws InterruptedException {

        driver.get(postUrl);
        Thread.sleep(4000);

        System.out.println("Loaded URL = " + driver.getCurrentUrl());

        // Scroll xuống từ từ
        slowScroll(12);

        // Click toàn bộ xem thêm
        expandAllComments();

        Thread.sleep(2000);

        System.out.println(">>> Collecting comments...");

        List<WebElement> commentBlocks = driver.findElements(By.xpath("//div[@aria-label='Bình luận']"));

        List<Map<String, String>> results = new ArrayList<>();

        for (WebElement block : commentBlocks) {

            try {
                String text = block.findElement(By.xpath(".//div[@dir='auto']")).getText();
                String time = block.findElement(By.xpath(".//a[contains(@href,'comment_id')]/span")).getText();

                Map<String, String> row = new HashMap<>();
                row.put("date", time);
                row.put("text", text);

                results.add(row);
            }
            catch (Exception ignored) {}
        }

        System.out.println(">>> TOTAL COMMENTS FOUND = " + results.size());
        return results;
    }


    public void close() {
        driver.quit();
    }
}
