package com.oop.logistics.Facebook;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
public class FacebookClient {

    private final String accessToken;
    private final HttpClient httpClient;

    public FacebookClient(String accessToken) {
        this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getPagePosts(String pageId) throws Exception {
        String url = "https://graph.facebook.com/v19.0/" 
                     + pageId 
                     + "/posts?access_token=" 
                     + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}
