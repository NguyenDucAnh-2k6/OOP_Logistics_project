import java.util.*;
import com.oop.logistics.crawler.FacebookCrawler;

public class TestCrawler {

    public static void main(String[] args) throws Exception {

        FacebookCrawler crawler = new FacebookCrawler(
                "D:\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe"
        );

        Map<String, String> cookies = new HashMap<>();
        cookies.put("c_user", "100072127640070");
        cookies.put("xs", "14%3A_3tWopl3qts-2g%3A2%3A1765898701%3A-1%3A-1%3A%3AAcxoaG_DEIT2YU-dAm--C8TVP9BBcqcCWanCHhnpUw");
        cookies.put("fr", "1hqFmtFI3VKuxGTMG.AWeQkm8Oj4TiKT3IfOH2wcLWI4CWfTkZbG7QwZGbxn3Nokp3orA.BpQq_0..AAA.0.0.BpQq_0.AWczvMVsV2yYbsB7kkvkAGk6ois");

        crawler.loginWithCookies(cookies);

        String url = "https://www.facebook.com/share/p/1HHfSVAp5g/";

        List<Map<String,String>> comments = crawler.crawlComments(url);

        crawler.saveCSV(comments, "D:\\JAVAProjects\\OOP_Logistics_project\\comments.csv");

        crawler.close();
    }
}
