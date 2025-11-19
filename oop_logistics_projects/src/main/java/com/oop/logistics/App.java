package com.oop.logistics;
import com.oop.logistics.Facebook.FacebookClient;
public class App {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("FB_TOKEN");

        FacebookClient fb = new FacebookClient(token);

        String json = fb.getPagePosts("CNN");  // example page
        System.out.println(json);
    }
}
