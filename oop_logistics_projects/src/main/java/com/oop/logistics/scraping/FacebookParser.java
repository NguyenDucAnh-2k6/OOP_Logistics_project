package com.oop.logistics.scraping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FacebookParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Output structure
    public static class FbComment {
        public String text;
        public long timestamp;
        public String author;

        public FbComment(String text, long timestamp, String author) {
            this.text = text;
            this.timestamp = timestamp;
            this.author = author;
        }
    }

    public static class FbPostData {
        public String title;
        public String description;
        public List<FbComment> comments = new ArrayList<>();
    }

    /**
     * Extract all JSON-like blocks from <script> tags
     */
    private static List<String> extractJsonBlocks(Document doc) {
        List<String> results = new ArrayList<>();

        Pattern jsonPattern = Pattern.compile("\\{.+}", Pattern.DOTALL);

        for (Element script : doc.select("script")) {
            String data = script.data().trim();
            if (data.contains("{") && data.contains("}")) {
                Matcher m = jsonPattern.matcher(data);
                while (m.find()) {
                    String json = m.group();
                    // avoid extremely short fragments
                    if (json.length() > 50)
                        results.add(json);
                }
            }
        }
        return results;
    }

    /**
     * Main parser: extracts title, description, comments and timestamps.
     */
    public static FbPostData parseFacebookHtml(Document doc) {
        FbPostData post = new FbPostData();

        // Metadata via OpenGraph
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) post.title = ogTitle.attr("content");

        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null) post.description = ogDesc.attr("content");

        // Extract raw JSON blocks
        List<String> jsonBlocks = extractJsonBlocks(doc);

        for (String json : jsonBlocks) {
            try {
                JsonNode root = mapper.readTree(json);

                // Try to find comments in deeply nested JSON structures
                findCommentsRecursive(root, post.comments);

            } catch (Exception ignored) {
                // Many blocks are NOT valid JSON → skip silently
            }
        }

        return post;
    }

    /**
     * Recursively scan all JSON nodes to find comment-like structures
     */
    private static void findCommentsRecursive(JsonNode node, List<FbComment> out) {
        if (node == null) return;

        // Look for known FB fields
        if (node.has("comment") || node.has("comment_text") || node.has("body")) {

            String text = null;
            long ts = 0;
            String author = "unknown";

            if (node.has("body")) {
                JsonNode t = node.get("body").get("text");
                if (t != null) text = t.asText();
            } else if (node.has("comment_text")) {
                text = node.get("comment_text").asText();
            }

            if (node.has("timestamp")) {
                ts = node.get("timestamp").asLong();
            } else if (node.has("created_time")) {
                ts = node.get("created_time").asLong();
            }

            if (node.has("author")) {
                JsonNode name = node.get("author").get("name");
                if (name != null) author = name.asText();
            }

            if (text != null && !text.isBlank()) {
                out.add(new FbComment(text, ts, author));
            }
        }

        // recursive scan all children
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                findCommentsRecursive(child, out);
            }
        }
    }
}

