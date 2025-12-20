package com.oop.logistics.crawler;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FacebookCrawler {

    private final ChromeDriver driver;

    public FacebookCrawler(String chromeDriverPath) {

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }


    // -------------------------------------------------------------------------
    //  LOGIN BẰNG COOKIE – CÁCH ỔN ĐỊNH NHẤT
    // -------------------------------------------------------------------------
    public void loginWithCookies(Map<String, String> cookies) throws InterruptedException {

        driver.get("https://facebook.com");
        Thread.sleep(3000);

        System.out.println(">>> Adding cookies...");

        for (Map.Entry<String, String> e : cookies.entrySet()) {
            Cookie ck = new Cookie.Builder(e.getKey(), e.getValue())
                    .domain(".facebook.com")
                    .path("/")
                    .isSecure(true)
                    .build();

            driver.manage().addCookie(ck);
        }

        driver.navigate().refresh();
        Thread.sleep(3000);

        System.out.println(">>> Login OK");
    }


    // -------------------------------------------------------------------------
    //  HÀM CLICK MỘT CÁCH AN TOÀN
    // -------------------------------------------------------------------------
    private void safeClick(By selector) {
        try {
            WebElement btn = driver.findElement(selector);
            btn.click();
            Thread.sleep(1200);
        } catch (Exception ignored) {}
    }


    // -------------------------------------------------------------------------
    //  MỞ TẤT CẢ COMMENT (ALL COMMENTS + SEE MORE + SEE MORE COMMENTS)
    // -------------------------------------------------------------------------
    private void expandAllComments() throws InterruptedException {

        // Bấm nút "Bình luận"
        safeClick(By.xpath("//div[@role='button']//span[contains(text(),'Bình luận')]"));

        // Bấm nút "Tất cả bình luận"
        safeClick(By.xpath("//span[contains(text(),'Tất cả bình luận')]"));

        // Loop bấm “Xem thêm bình luận”
        for (int i = 0; i < 15; i++) {
            List<WebElement> more = driver.findElements(
                    By.xpath("//div[@role='button']//span[contains(text(),'Xem thêm bình luận')]")
            );

            if (more.isEmpty()) break;

            for (WebElement m : more) {
                try { m.click(); Thread.sleep(1300); }
                catch (Exception ignored) {}
            }

            Thread.sleep(1200);
        }

        // Mở “Xem thêm” trong từng comment
        List<WebElement> more2 = driver.findElements(By.xpath("//div[@role='button']//span[text()='Xem thêm']"));
        for (WebElement m : more2) {
            try { m.click(); Thread.sleep(1000); }
            catch (Exception ignored) {}
        }
    }


    // -------------------------------------------------------------------------
    //  CRAWL COMMENT + TIME
    // -------------------------------------------------------------------------
    public List<Map<String,String>> crawlComments(String postUrl) throws InterruptedException {

        driver.get(postUrl);
        Thread.sleep(5000);

        expandAllComments();

        // Tìm từng comment + time
        List<WebElement> blocks = driver.findElements(By.xpath("//div[@aria-label='Bình luận']"));

        List<Map<String,String>> results = new ArrayList<>();

        for (WebElement block : blocks) {
            try {
                String text = block.findElement(By.xpath(".//div[@dir='auto']")).getText().trim();
                String time = block.findElement(By.xpath(".//a[contains(@href,'/posts/')]//span")).getText();

                Map<String,String> row = new HashMap<>();
                row.put("date", time);
                row.put("text", text);

                results.add(row);

            } catch (Exception ignored) {}
        }

        System.out.println(">>> TOTAL COMMENTS: " + results.size());
        return results;
    }


    // -------------------------------------------------------------------------
    //  LƯU CSV
    // -------------------------------------------------------------------------
    public void saveCSV(List<Map<String,String>> data, String path) {
        try (FileWriter fw = new FileWriter(path)) {

            fw.write("date,text\n");

            for (Map<String,String> row : data) {
                String date = row.get("date").replace(",", " ");
                String text = row.get("text").replace("\"", "\"\"");
                fw.write(date + ",\"" + text + "\"\n");
            }

            fw.flush();
            System.out.println(">>> CSV saved to: " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void close() {
        driver.quit();
    }
}
