import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class Gemini {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public Gemini() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.apiKey = Config.geminiApiKey();
        this.model = Config.geminiModel();
    }

    public String generateReply(String message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("Missing GEMINI_API_KEY environment variable.", 500);
        }

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + urlEncode(model)
                + ":generateContent";

        String requestBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(Json.escape(message));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new GeminiException("Gemini API network error.", 504, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Gemini API request interrupted.", 504, ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new GeminiException(extractGeminiError(response.body()), mapStatus(response.statusCode()));
        }

        String reply = extractReply(response.body());
        if (reply == null || reply.isBlank()) {
            throw new GeminiException("Gemini returned an empty response.", 502);
        }

        return reply;
    }

    private String extractReply(String json) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            return null;
        }

        Object candidatesValue = root.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            return null;
        }

        Object contentValue = candidate.get("content");
        if (!(contentValue instanceof Map<?, ?> content)) {
            return null;
        }

        Object partsValue = content.get("parts");
        if (!(partsValue instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }

        StringBuilder reply = new StringBuilder();
        for (Object partValue : parts) {
            if (!(partValue instanceof Map<?, ?> part)) {
                continue;
            }
            Object textValue = part.get("text");
            if (textValue instanceof String text && !text.isBlank()) {
                if (!reply.isEmpty()) {
                    reply.append('\n');
                }
                reply.append(text);
            }
        }
        return reply.toString();
    }

    private String extractGeminiError(String json) {
        try {
            Object parsed = Json.parse(json);
            if (!(parsed instanceof Map<?, ?> root)) {
                return "Gemini API call failed.";
            }
            Object errorValue = root.get("error");
            if (!(errorValue instanceof Map<?, ?> error)) {
                return "Gemini API call failed.";
            }
            Object messageValue = error.get("message");
            if (messageValue instanceof String message && !message.isBlank()) {
                return "Gemini API call failed: " + message;
            }
            return "Gemini API call failed.";
        } catch (Exception ignored) {
            return "Gemini API call failed.";
        }
    }

    private int mapStatus(int upstreamStatus) {
        if (upstreamStatus == 400) {
            return 400;
        }
        if (upstreamStatus == 401 || upstreamStatus == 403) {
            return 500;
        }
        if (upstreamStatus == 408 || upstreamStatus == 504) {
            return 504;
        }
        return 502;
    }

    private String urlEncode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
}
