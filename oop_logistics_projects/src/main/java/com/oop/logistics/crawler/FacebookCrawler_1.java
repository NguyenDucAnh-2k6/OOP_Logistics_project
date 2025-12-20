package com.oop.logistics.crawler;

import org.openqa.selenium.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FacebookCrawler_1 {

    private final ChromeDriver driver;

    public FacebookCrawler_1(String chromeDriverPath) {

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
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
            List<WebElement> els = driver.findElements(selector);
            if (els.isEmpty()) {
                System.out.println("[safeClick] no element for: " + selector);
                return;
            }

            WebElement btn = els.get(0);
            try {
                btn.click();
            } catch (Exception e) {
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                } catch (Exception jsEx) {
                    System.out.println("[safeClick] click failed for selector: " + selector + " -> " + jsEx.getMessage());
                }
            }

            Thread.sleep(1200);
        } catch (Exception e) {
            System.out.println("[safeClick] unexpected: " + e.getMessage());
        }
    }


    // -------------------------------------------------------------------------
    //  MỞ TẤT CẢ COMMENT (ALL COMMENTS + SEE MORE + SEE MORE COMMENTS)
    // -------------------------------------------------------------------------
    private void expandAllComments() throws InterruptedException {

        String[] commentButtons = new String[]{"Bình luận","Comments","Comment","Coment"};
        for (String t : commentButtons) {
            safeClick(By.xpath("//div[@role='button']//span[contains(text('" + t + "'))]"));
        }

        String[] allComments = new String[]{"Tất cả bình luận","All comments","See all comments"};
        for (String t : allComments) {
            safeClick(By.xpath("//span[contains(text(),'" + t + "')]") );
        }

        // Loop clicking various "see more comments" labels
        String[] moreLabels = new String[]{"Xem thêm bình luận","See more comments","Load more comments","Xem thêm" , "See more"};
        for (int i = 0; i < 20; i++) {
            boolean clickedAny = false;
            for (String lbl : moreLabels) {
                List<WebElement> more = driver.findElements(By.xpath("//div[@role='button']//span[contains(text(),'" + lbl + "')]") );
                if (!more.isEmpty()) {
                    clickedAny = true;
                    for (WebElement m : more) {
                        try { m.click(); Thread.sleep(800); } catch (Exception ignored) {}
                    }
                }
            }

            // try a generic "load more" button by data-testid
            List<WebElement> generic = driver.findElements(By.xpath("//div[@role='button' and (contains(@aria-label,'more') or contains(@data-testid,'more_comments'))]"));
            for (WebElement g : generic) {
                try { g.click(); clickedAny = true; Thread.sleep(800); } catch (Exception ignored) {}
            }

            // scroll to bottom to load more
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 800);");
            Thread.sleep(1000);

            if (!clickedAny) break;
        }

        // Mở 'See more' inside individual comments
        List<WebElement> more2 = driver.findElements(By.xpath("//div[@role='button']//span[text()='Xem thêm' or text()='See more']"));
        for (WebElement m : more2) {
            try { m.click(); Thread.sleep(600); } catch (Exception ignored) {}
        }
    }


    // -------------------------------------------------------------------------
    //  CRAWL COMMENT + TIME
    // -------------------------------------------------------------------------
    public List<Map<String,String>> crawlComments(String postUrl) throws InterruptedException {

        driver.get(postUrl);
        Thread.sleep(5000);

        expandAllComments();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        // try several strategies to locate comment blocks
        String[] commentXPaths = new String[]{
                "//div[@aria-label='Bình luận']",
                "//div[contains(@aria-label,'Comment') or contains(@aria-label,'Bình luận')]",
                "//div[contains(@data-testid,'UFI2Comment/root_depth')]",
                "//div[contains(@data-testid,'ufi_comment')]",
                "//div[@role='article' and .//span[contains(@class,'_72vr')]]"
        };

        List<WebElement> blocks = new ArrayList<>();
        for (String xp : commentXPaths) {
            try {
                List<WebElement> found = driver.findElements(By.xpath(xp));
                if (!found.isEmpty()) {
                    System.out.println("[crawlComments] found " + found.size() + " blocks using xpath: " + xp);
                    blocks.addAll(found);
                }
            } catch (Exception e) {
                System.out.println("[crawlComments] xpath error: " + xp + " -> " + e.getMessage());
            }
        }

        // fallback: find elements that look like comments by role/article
        if (blocks.isEmpty()) {
            List<WebElement> fallback = driver.findElements(By.xpath("//div[@role='article']"));
            System.out.println("[crawlComments] fallback article count: " + fallback.size());
            blocks.addAll(fallback);
        }

        List<Map<String,String>> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (WebElement block : blocks) {
            try {
                String text = "";
                try {
                    WebElement txtEl = block.findElement(By.xpath(".//div[@dir='auto' or contains(@class,'_aok') or .//span]") );
                    text = txtEl.getText().trim();
                } catch (Exception e) {
                    try { text = block.getText().trim(); } catch (Exception ignored) {}
                }

                String time = "";
                try {
                    WebElement timeEl = block.findElement(By.xpath(".//abbr | .//a//span[contains(@class,'timestamp') or contains(@data-tooltip-content,'ago')]") );
                    time = timeEl.getText().trim();
                } catch (Exception e) {
                    try {
                        List<WebElement> spans = block.findElements(By.xpath(".//a//span"));
                        for (WebElement s : spans) {
                            String t = s.getText();
                            if (t != null && (t.contains("ago") || t.matches(".*\\d+ (m|h|d|w|mo|y).*") || t.contains("giờ") || t.contains("ngày"))) {
                                time = t; break;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (text.isEmpty()) continue;
                String key = (time + "|" + text).replaceAll("\\s+"," ");
                if (seen.contains(key)) continue;
                seen.add(key);

                Map<String,String> row = new HashMap<>();
                row.put("date", time.isEmpty() ? "" : time);
                row.put("text", text);
                results.add(row);

            } catch (Exception e) {
                System.out.println("[crawlComments] block parse error: " + e.getMessage());
            }
        }

        System.out.println(">>> TOTAL COMMENTS: " + results.size());

        if (results.isEmpty()) {
            try {
                savePageSource("debug_facebook_page_source.html");
                System.out.println("[crawlComments] saved page source to debug_facebook_page_source.html");
            } catch (Exception e) {
                System.out.println("[crawlComments] saving page source failed: " + e.getMessage());
            }
        }

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

    private void savePageSource(String filename) {
        try {
            String src = driver.getPageSource();
            Files.write(Paths.get(filename), src.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("[savePageSource] failed: " + e.getMessage());
        }
    }


    public void close() {
        driver.quit();
    }
}
