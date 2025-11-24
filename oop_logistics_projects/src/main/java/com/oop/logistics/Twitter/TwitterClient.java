package com.oop.logistics.Twitter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for Twitter/X API v2
 * Handles authentication and API requests
 */
public class TwitterClient {

    private static final String BASE_URL = "https://api.twitter.com/2";
    private final String bearerToken;
    private final HttpClient httpClient;
    private final Gson gson;

    public TwitterClient(String bearerToken) {
        this.bearerToken = bearerToken;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
    }

    /**
     * Search recent tweets by query
     * @param query Search query (keywords, hashtags)
     * @param maxResults Maximum results (10-100)
     * @return List of TwitterPost objects
     */
    public List<TwitterPost> searchRecentTweets(String query, int maxResults) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        
        String url = BASE_URL + "/tweets/search/recent"
            + "?query=" + encodedQuery
            + "&max_results=" + Math.min(Math.max(maxResults, 10), 100)
            + "&tweet.fields=id,text,created_at,public_metrics,geo,lang"
            + "&expansions=author_id,geo.place_id"
            + "&user.fields=name,username,location"
            + "&place.fields=full_name,country";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("Authorization", "Bearer " + bearerToken)
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "Twitter API error: " + response.statusCode() + " - " + response.body()
            );
        }

        return parseSearchResponse(response.body());
    }

    /**
     * Search tweets with multiple keywords
     */
    public List<TwitterPost> searchMultipleKeywords(List<String> keywords, int maxResultsPerKeyword) 
            throws Exception {
        List<TwitterPost> allTweets = new ArrayList<>();
        
        for (String keyword : keywords) {
            try {
                List<TwitterPost> tweets = searchRecentTweets(keyword, maxResultsPerKeyword);
                allTweets.addAll(tweets);
                
                // Rate limiting: wait between requests
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("Error searching for keyword '" + keyword + "': " + e.getMessage());
            }
        }
        
        return allTweets;
    }

    /**
     * Get tweets from a specific user timeline
     */
    public List<TwitterPost> getUserTweets(String userId, int maxResults) throws Exception {
        String url = BASE_URL + "/users/" + userId + "/tweets"
            + "?max_results=" + Math.min(Math.max(maxResults, 5), 100)
            + "&tweet.fields=id,text,created_at,public_metrics,geo"
            + "&expansions=geo.place_id"
            + "&place.fields=full_name,country";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("Authorization", "Bearer " + bearerToken)
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "Twitter API error: " + response.statusCode() + " - " + response.body()
            );
        }

        return parseSearchResponse(response.body());
    }

    /**
     * Get user ID from username
     */
    public String getUserIdByUsername(String username) throws Exception {
        String url = BASE_URL + "/users/by/username/" + username;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("Authorization", "Bearer " + bearerToken)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get user ID: " + response.body());
        }

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        JsonObject data = jsonResponse.getAsJsonObject("data");
        
        return data != null ? data.get("id").getAsString() : null;
    }

    /**
     * Parse API response into TwitterPost objects
     */
    private List<TwitterPost> parseSearchResponse(String jsonResponse) {
        List<TwitterPost> posts = new ArrayList<>();
        
        JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
        JsonArray data = response.getAsJsonArray("data");
        
        if (data == null) {
            return posts;
        }

        // Parse includes for user and place data
        JsonObject includes = response.getAsJsonObject("includes");
        java.util.Map<String, JsonObject> users = new java.util.HashMap<>();
        java.util.Map<String, JsonObject> places = new java.util.HashMap<>();
        
        if (includes != null) {
            JsonArray usersArray = includes.getAsJsonArray("users");
            if (usersArray != null) {
                for (JsonElement userEl : usersArray) {
                    JsonObject user = userEl.getAsJsonObject();
                    users.put(user.get("id").getAsString(), user);
                }
            }
            
            JsonArray placesArray = includes.getAsJsonArray("places");
            if (placesArray != null) {
                for (JsonElement placeEl : placesArray) {
                    JsonObject place = placeEl.getAsJsonObject();
                    places.put(place.get("id").getAsString(), place);
                }
            }
        }

        // Parse tweets
        for (JsonElement element : data) {
            JsonObject tweetJson = element.getAsJsonObject();
            TwitterPost post = new TwitterPost();
            
            post.setId(getStringOrNull(tweetJson, "id"));
            post.setText(getStringOrNull(tweetJson, "text"));
            post.setCreatedAt(getStringOrNull(tweetJson, "created_at"));
            post.setLang(getStringOrNull(tweetJson, "lang"));
            
            // Parse metrics
            if (tweetJson.has("public_metrics")) {
                JsonObject metrics = tweetJson.getAsJsonObject("public_metrics");
                post.setRetweetCount(getIntOrZero(metrics, "retweet_count"));
                post.setReplyCount(getIntOrZero(metrics, "reply_count"));
                post.setLikeCount(getIntOrZero(metrics, "like_count"));
                post.setQuoteCount(getIntOrZero(metrics, "quote_count"));
            }
            
            // Parse author info
            String authorId = getStringOrNull(tweetJson, "author_id");
            if (authorId != null && users.containsKey(authorId)) {
                JsonObject author = users.get(authorId);
                post.setAuthorName(getStringOrNull(author, "name"));
                post.setAuthorUsername(getStringOrNull(author, "username"));
                post.setAuthorLocation(getStringOrNull(author, "location"));
            }
            
            // Parse geo/place info
            if (tweetJson.has("geo")) {
                JsonObject geo = tweetJson.getAsJsonObject("geo");
                String placeId = getStringOrNull(geo, "place_id");
                if (placeId != null && places.containsKey(placeId)) {
                    JsonObject place = places.get(placeId);
                    post.setLocation(getStringOrNull(place, "full_name"));
                    post.setCountry(getStringOrNull(place, "country"));
                }
            }
            
            posts.add(post);
        }

        return posts;
    }

    /**
     * Check if API is accessible
     */
    public boolean isApiAccessible() {
        try {
            String url = BASE_URL + "/tweets/search/recent?query=test&max_results=10";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200 || response.statusCode() == 429;
        } catch (Exception e) {
            return false;
        }
    }

    // Helper methods
    private String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() 
            ? obj.get(key).getAsString() 
            : null;
    }

    private int getIntOrZero(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() 
            ? obj.get(key).getAsInt() 
            : 0;
    }
}