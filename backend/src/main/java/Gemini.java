import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.List;

public class Gemini {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public Gemini() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
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
        """.formatted(message.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return extractReply(response.body());

        } catch (Exception e) {
            return "Gemini error: " + e.getMessage();
        }
    }

    private String extractReply(String json) {
        int index = json.indexOf("\"text\":");
        if (index == -1) return "No response";

        int start = json.indexOf("\"", index + 7) + 1;
        int end = json.indexOf("\"", start);

        if (start == -1 || end == -1) return "No response";

        return json.substring(start, end);
    }

    private String urlEncode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
}