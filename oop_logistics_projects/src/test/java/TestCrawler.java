import com.oop.logistics.crawler.FacebookCrawler;
import java.util.*;
import java.io.FileWriter;


public class TestCrawler {
    public static void saveCSV(List<Map<String,String>> rows, String filename) throws Exception {

        FileWriter csv = new FileWriter(filename);

    csv.write("date,text\n");
    for (Map<String,String> r : rows) {
        csv.write("\"" + r.get("date") + "\",\"" + r.get("text").replace("\"","'") + "\"\n");
    }
    csv.close();

    System.out.println(">>> Saved to " + filename);
    }

    public static void main(String[] args) throws Exception {

        FacebookCrawler crawler = new FacebookCrawler(
                "D:\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe"
        );

        Map<String, String> cookies = new HashMap<>();
        cookies.put("c_user", "100072127640070");
        cookies.put("xs", "40%3A1RcoACc-sNkDqA%3A2%3A1765676730%3A-1%3A-1%3A%3AAczc9I6PftucoqxcJMLLBGFNhPJmMD6aa746IjgVFcs");
        cookies.put("fr", "1f5Tq9DicHsIY9xn3.AWeyxdnhXJ9x1RNTzBfnwiCJaiw1uC6Y_Azf4KEEKOAzwsx1mRE.BpQDBR..AAA.0.0.BpQDBR.AWfN9YMTY_unfmXa5ul7CJrB670");

        crawler.loginWithCookies(cookies);

        String url = "https://www.facebook.com/share/v/1ADfAWWtPn/";

        List<Map<String,String>> rows = crawler.crawlComments(url);

        saveCSV(rows, "D:\\JAVAProjects\\OOP_Logistics_project\\comments.csv");

        crawler.close();
    }
}
