package com.oop.logistics.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for HTTP operations
 * Provides common HTTP client functionality for all data sources
 */
public class HttpClientUtil {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final HttpClient sharedClient;
    private static final Gson gson;

    static {
        sharedClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    /**
     * Get shared HTTP client instance
     */
    public static HttpClient getSharedClient() {
        return sharedClient;
    }

    /**
     * Create custom HTTP client with specific timeout
     */
    public static HttpClient createClient(int timeoutSeconds) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Perform GET request
     */
    public static HttpResponse<String> get(String url) throws Exception {
        return get(url, null);
    }

    /**
     * Perform GET request with headers
     */
    public static HttpResponse<String> get(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .GET();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        return sharedClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Perform async GET request
     */
    public static CompletableFuture<HttpResponse<String>> getAsync(String url) {
        return getAsync(url, null);
    }

    /**
     * Perform async GET request with headers
     */
    public static CompletableFuture<HttpResponse<String>> getAsync(String url, Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .GET();

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            return sharedClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Perform POST request with JSON body
     */
    public static HttpResponse<String> post(String url, Object body) throws Exception {
        return post(url, body, null);
    }

    /**
     * Perform POST request with JSON body and headers
     */
    public static HttpResponse<String> post(String url, Object body, Map<String, String> headers) 
            throws Exception {
        String jsonBody = gson.toJson(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        return sharedClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Perform async POST request
     */
    public static CompletableFuture<HttpResponse<String>> postAsync(String url, Object body) {
        return postAsync(url, body, null);
    }

    /**
     * Perform async POST request with headers
     */
    public static CompletableFuture<HttpResponse<String>> postAsync(
            String url, Object body, Map<String, String> headers) {
        try {
            String jsonBody = gson.toJson(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            return sharedClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Perform PUT request
     */
    public static HttpResponse<String> put(String url, Object body, Map<String, String> headers) 
            throws Exception {
        String jsonBody = gson.toJson(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        return sharedClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Perform DELETE request
     */
    public static HttpResponse<String> delete(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .DELETE();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        return sharedClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Parse JSON response to object
     */
    public static <T> T parseJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Check if URL is accessible
     */
    public static boolean isAccessible(String url) {
        try {
            HttpResponse<String> response = get(url);
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if URL is accessible (async)
     */
    public static CompletableFuture<Boolean> isAccessibleAsync(String url) {
        return getAsync(url)
            .thenApply(response -> response.statusCode() >= 200 && response.statusCode() < 400)
            .exceptionally(e -> false);
    }

    /**
     * Build URL with query parameters
     */
    public static String buildUrl(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(encodeParam(entry.getKey()))
                      .append("=")
                      .append(encodeParam(entry.getValue()));
            first = false;
        }

        return urlBuilder.toString();
    }

    /**
     * URL encode a parameter
     */
    public static String encodeParam(String param) {
        try {
            return java.net.URLEncoder.encode(param, "UTF-8");
        } catch (Exception e) {
            return param;
        }
    }

    /**
     * Create Bearer auth header map
     */
    public static Map<String, String> bearerAuthHeader(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    /**
     * Create Basic auth header map
     */
    public static Map<String, String> basicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
        return Map.of("Authorization", "Basic " + encoded);
    }

    /**
     * Response wrapper for easier handling
     */
    public static class Response<T> {
        private final int statusCode;
        private final T body;
        private final String rawBody;
        private final boolean success;
        private final String error;

        public Response(int statusCode, T body, String rawBody, boolean success, String error) {
            this.statusCode = statusCode;
            this.body = body;
            this.rawBody = rawBody;
            this.success = success;
            this.error = error;
        }

        public int getStatusCode() { return statusCode; }
        public T getBody() { return body; }
        public String getRawBody() { return rawBody; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }

    /**
     * Execute GET and parse response
     */
    public static <T> Response<T> getAndParse(String url, Class<T> responseClass) {
        try {
            HttpResponse<String> response = get(url);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            
            T body = null;
            if (success) {
                body = parseJson(response.body(), responseClass);
            }
            
            return new Response<>(
                response.statusCode(), 
                body, 
                response.body(), 
                success, 
                success ? null : response.body()
            );
        } catch (Exception e) {
            return new Response<>(0, null, null, false, e.getMessage());
        }
    }
}