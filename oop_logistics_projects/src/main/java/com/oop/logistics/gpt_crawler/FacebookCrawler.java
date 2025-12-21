package com.oop.logistics.gpt_crawler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class FacebookCrawler {

    private WebDriver driver;
    private WebDriverWait wait;
    private final List<Comment> extractedComments = new ArrayList<>();

    public FacebookCrawler(String cUser, String xs, String fr) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--disable-notifications",
                "--disable-infobars",
                "--disable-extensions",
                "--start-maximized"
        );

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        loginByCookies(cUser, xs, fr);
    }

    /* =========================
       LOGIN USING COOKIES
       ========================= */
    private void loginByCookies(String cUser, String xs, String fr) {
        driver.get("https://www.facebook.com/");

        driver.manage().addCookie(new Cookie("c_user", cUser, ".facebook.com", "/", null));
        driver.manage().addCookie(new Cookie("xs", xs, ".facebook.com", "/", null));
        driver.manage().addCookie(new Cookie("fr", fr, ".facebook.com", "/", null));

        driver.navigate().refresh();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /* =========================
       ENTRY POINT
       ========================= */
    public void crawlPost(String url) {
    driver.get(url);
    sleep(3000);

    if (isReel()) {
        openReelComments();
    }

    expandSortMenu();
    expandAllComments();

    extractComments();
    }
    private void extractComments() {

    List<WebElement> commentBlocks = driver.findElements(By.xpath(
            "//div[@role='article']"
    ));

    for (WebElement block : commentBlocks) {
        try {
            // AUTHOR
            WebElement authorEl = block.findElement(By.xpath(
                    ".//span[contains(@class,'xt0psk2')]//a"
            ));
            String author = authorEl.getText();

            // COMMENT TEXT
            WebElement textEl = block.findElement(By.xpath(
                    ".//div[@data-ad-preview='message']"
            ));
            String text = textEl.getText();

            int level = isReply(block) ? 1 : 0;

            extractedComments.add(new Comment(author, text, level));

        } catch (NoSuchElementException ignored) {
        }
    }
}
    private boolean isReply(WebElement commentBlock) {
    try {
        commentBlock.findElement(By.xpath(".//ul"));
        return true;
    } catch (NoSuchElementException e) {
        return false;
    }
}
    public void writeCsv(String outputPath) {

    try (Writer writer = new OutputStreamWriter(
            new FileOutputStream(outputPath),
            StandardCharsets.UTF_8
    )) {

        // UTF-8 BOM for Excel compatibility
        writer.write("\uFEFFauthor,text,level\n");

        for (Comment c : extractedComments) {
            writer.write(escape(c.author) + ","
                    + escape(c.text) + ","
                    + c.level + "\n");
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
}
    private String escape(String s) {
    if (s == null) return "";
    return "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\"";
}


    /* =========================
       REEL DETECTION
       ========================= */
    private boolean isReel() {
        return !driver.findElements(By.xpath(
                "//div[@aria-label='Bình luận' or @aria-label='Comment']"
        )).isEmpty();
    }

    private void openReelComments() {
        try {
            WebElement commentBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@aria-label='Bình luận' or @aria-label='Comment']")
            ));
            commentBtn.click();
            sleep(2000);
        } catch (Exception ignored) {}
    }

    /* =========================
       SORT: MOST RELEVANT
       ========================= */
    private void expandSortMenu() {
        try {
            WebElement sortBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[contains(text(),'Most relevant') or contains(text(),'Phù hợp nhất')]")
            ));
            sortBtn.click();
            sleep(1000);
        } catch (Exception ignored) {}
    }

    /* =========================
       CORE CRAWLING LOOP
       ========================= */
    private void expandAllComments() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
            clickAllReplies();
            clickLoadMoreComments();

            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(2000);

            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) {
                // no more scrolling possible
                break;
            }
            lastHeight = newHeight;
        }
    }

    /* =========================
       CLICK "VIEW ALL REPLIES"
       ========================= */
    private void clickAllReplies() {
        List<WebElement> replyButtons = driver.findElements(By.xpath(
                "//span[contains(text(),'Xem tất cả') and contains(text(),'phản hồi')] | " +
                "//span[contains(text(),'View') and contains(text(),'repl')]"
        ));

        for (WebElement btn : replyButtons) {
            try {
                scrollIntoView(btn);
                btn.click();
                sleep(800);
            } catch (Exception ignored) {}
        }
    }

    /* =========================
       CLICK "LOAD MORE COMMENTS"
       ========================= */
    private void clickLoadMoreComments() {
        List<WebElement> loadMore = driver.findElements(By.xpath(
                "//span[contains(text(),'Xem thêm bình luận')] | " +
                "//span[contains(text(),'View more comments')]"
        ));

        for (WebElement btn : loadMore) {
            try {
                scrollIntoView(btn);
                btn.click();
                sleep(1500);
            } catch (Exception ignored) {}
        }
    }

    /* =========================
       UTILITIES
       ========================= */
    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element
        );
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
    
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}

