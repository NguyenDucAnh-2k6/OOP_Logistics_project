package com.oop.logistics.Facebook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class FacebookClient {

    private final String accessToken = "EAAdllrHFvAYBP1mZAlNTIYHoUpWGckMwmY07S8fxYBaPYyLL01k4tNJ7j1ox9gvWTu5HTZCMB7t0d7ozWSNZBr3Jnq665L9JZBoovdl73PmMR8vGiZAxUKFyV2wmHTlONsZCMObfAqQlrwFTfeny25dt9xY5AsTbZA0aFpGc9NgquVOQAikgwLo6DM3UvUZCLceAUZBp7EP0P0R8osDdu6lSP2ySSoCYWQt9mmmsZAVzgXOf2TRAwQ6G8WXtCxhbOMo36t0iCb7kZCzwSqicR9nvSCP";
    private final HttpClient httpClient;
    private final Gson gson;

    public FacebookClient(String accessToken) {
        //this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Get raw JSON response from Facebook API
     */
    public String getPagePostsRaw(String pageId) throws Exception {
        String url = "https://graph.facebook.com/v19.0/" 
                     + pageId 
                     + "/posts?fields=id,message,created_time,likes.summary(true),comments.summary(true),shares"
                     + "&access_token=" 
                     + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Facebook API error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    /**
     * Get parsed list of FacebookPost objects
     */
    public List<FacebookPost> getPagePosts(String pageId) throws Exception {
        String jsonResponse = getPagePostsRaw(pageId);
        return parsePostsFromJson(jsonResponse);
    }

    /**
     * Parse JSON response into FacebookPost objects
     */
    private List<FacebookPost> parsePostsFromJson(String jsonResponse) {
        List<FacebookPost> posts = new ArrayList<>();
        
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonArray dataArray = jsonObject.getAsJsonArray("data");

        if (dataArray != null) {
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject postJson = dataArray.get(i).getAsJsonObject();
                FacebookPost post = gson.fromJson(postJson, FacebookPost.class);
                
                // Parse engagement metrics if available
                if (postJson.has("likes")) {
                    JsonObject likes = postJson.getAsJsonObject("likes");
                    if (likes.has("summary")) {
                        post.setLikes(likes.getAsJsonObject("summary").get("total_count").getAsInt());
                    }
                }
                
                if (postJson.has("comments")) {
                    JsonObject comments = postJson.getAsJsonObject("comments");
                    if (comments.has("summary")) {
                        post.setComments(comments.getAsJsonObject("summary").get("total_count").getAsInt());
                    }
                }
                
                if (postJson.has("shares")) {
                    post.setShares(postJson.getAsJsonObject("shares").get("count").getAsInt());
                }
                
                posts.add(post);
            }
        }

        return posts;
    }

    /**
     * Search posts by keyword
     */
    public List<FacebookPost> searchPosts(String pageId, String keyword) throws Exception {
        List<FacebookPost> allPosts = getPagePosts(pageId);
        List<FacebookPost> filteredPosts = new ArrayList<>();

        for (FacebookPost post : allPosts) {
            if (post.getMessage() != null && 
                post.getMessage().toLowerCase().contains(keyword.toLowerCase())) {
                filteredPosts.add(post);
            }
        }

        return filteredPosts;
    }
    // In FacebookClient.java, add this method
public List<FacebookComment> getPostComments(String postId) throws Exception {
    String url = "https://graph.facebook.com/v19.0/" 
                 + postId 
                 + "/comments?fields=id,message,created_time,from&summary=true"
                 + "&access_token=" 
                 + accessToken;

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
        throw new RuntimeException("Facebook API error: " + response.statusCode() + " - " + response.body());
    }

    return parseCommentsFromJson(response.body());
}

// Add a private parser method
private List<FacebookComment> parseCommentsFromJson(String jsonResponse) {
    List<FacebookComment> comments = new ArrayList<>();
    JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
    JsonArray dataArray = jsonObject.getAsJsonArray("data");

    if (dataArray != null) {
        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject commentJson = dataArray.get(i).getAsJsonObject();
            FacebookComment comment = new FacebookComment();
            comment.setId(commentJson.get("id").getAsString());
            if (commentJson.has("message")) comment.setMessage(commentJson.get("message").getAsString());
            if (commentJson.has("created_time")) comment.setCreatedTime(commentJson.get("created_time").getAsString());
            if (commentJson.has("from")) {
                JsonObject from = commentJson.getAsJsonObject("from");
                comment.setFromName(from.get("name").getAsString());
            }
            comments.add(comment);
        }
    }
    return comments;
}
}