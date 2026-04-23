import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Gemini {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public Gemini() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        this.apiKey = Config.geminiApiKey();
        this.model = Config.geminiModel();
    }

    public String generateReply(String message) {

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + urlEncode(model)
                + ":generateContent";

        String requestBody = """
        {
          "contents": [
            {
              "parts": [
                { "text": "%s" }
              ]
            }
          ]
        }
        """.formatted(escapeJson(message));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        int attempts = 0;
        int maxAttempts = 3;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                String body = response.body();

                if (response.statusCode() != 200) {
                    System.out.println("Gemini HTTP error: " + response.statusCode());
                    System.out.println(body);
                    continue;
                }

                // 🔑 FIXED: real JSON parsing
                String extracted = extractReply(body);

                if (extracted == null || extracted.isBlank()) {
                    System.out.println("Empty Gemini response, retrying...");
                    continue;
                }

                return extracted;

            } catch (Exception e) {
                System.out.println("Gemini attempt " + attempts + " failed: " + e.getMessage());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException(
                "Gemini error: request failed after " + maxAttempts + " attempts"
        );
    }

    // ─────────────────────────────
    // FIXED JSON EXTRACTION
    // ─────────────────────────────
    private String extractReply(String json) {

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                System.out.println("No candidates in response:\n" + json);
                return null;
            }

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts == null || parts.size() == 0) {
                System.out.println("No parts in response:\n" + json);
                return null;
            }

            String text = parts.get(0).getAsJsonObject().get("text").getAsString();

            return text;

        } catch (Exception e) {
            System.out.println("Failed to parse Gemini JSON:");
            System.out.println(json);
            return null;
        }
    }

    // ─────────────────────────────
    // ESCAPE INPUT SAFELY
    // ─────────────────────────────
    private String escapeJson(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String urlEncode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
}