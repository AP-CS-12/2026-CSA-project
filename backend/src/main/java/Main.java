import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Server starting...");

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) port = Integer.parseInt(portEnv);

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        Gson gson = new Gson();

        // ─────────────────────────────
        // HEALTH
        // ─────────────────────────────
        server.createContext("/api/health", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // ─────────────────────────────
        // START SESSION
        // ─────────────────────────────
        server.createContext("/api/sessions/start", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            long studentId = Long.parseLong(body.replaceAll("[^0-9]", ""));
            long sessionId = Database.createSession(studentId);

            String response = gson.toJson(new SessionResponse(sessionId));

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // ─────────────────────────────
        // CHAT
        // ─────────────────────────────
        server.createContext("/api/chat", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            ChatRequest req = gson.fromJson(body, ChatRequest.class);

            long sessionId = req.sessionId;
            String topic = req.message;

            String prompt =
                    """
                    OUTPUT_FORMAT=JSON
                    STRICT_MODE=true
                    NO_TEXT_ALLOWED=true
                
                    SCHEMA:
                    {
                      "topic": "string",
                      "difficulty": 1,
                      "question": "string",
                      "answers": {
                        "A": "string",
                        "B": "string",
                        "C": "string",
                        "D": "string"
                      },
                      "correct_answer": "A"
                    }
                
                    RULES:
                    - Output must match SCHEMA exactly
                    - No additional keys
                    - No explanation
                    - No markdown
                    - No backticks
                    - Output starts with { and ends with }
                
                    INPUT:
                    topic = %s
                    """.formatted(topic);

            Gemini gemini = new Gemini();
            String rawReply = gemini.generateReply(prompt);

            System.out.println("RAW GEMINI REPLY:\n" + rawReply);

            if (rawReply == null || rawReply.isBlank()) {
                throw new RuntimeException("Empty Gemini response");
            }

            String cleaned = extractJson(rawReply);

            GeminiQuestion q = gson.fromJson(cleaned, GeminiQuestion.class);

            if (q == null || q.answers == null) {
                throw new RuntimeException("Invalid Gemini JSON");
            }

            Database.saveMessage(sessionId, "user", topic);
            Database.saveMessage(sessionId, "assistant", cleaned);

            String response = gson.toJson(new ReplyResponse(cleaned));

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // ─────────────────────────────
        // GENERATE QUESTION
        // ─────────────────────────────
        server.createContext("/api/generate-question", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String topic = "general";

                if (query != null && query.contains("topic=")) {
                    String raw = query.split("topic=")[1].split("&")[0];
                    topic = URLDecoder.decode(raw, StandardCharsets.UTF_8);
                }

                String prompt =
                        """
                        OUTPUT_FORMAT=JSON
                        STRICT_MODE=true
                        NO_TEXT_ALLOWED=true
                    
                        SCHEMA:
                        {
                          "topic": "string",
                          "difficulty": 1,
                          "question": "string",
                          "answers": {
                            "A": "string",
                            "B": "string",
                            "C": "string",
                            "D": "string"
                          },
                          "correct_answer": "A"
                        }
                    
                        RULES:
                        - Output must match SCHEMA exactly
                        - No additional keys
                        - No explanation
                        - No markdown
                        - No backticks
                        - Output starts with { and ends with }
                    
                        INPUT:
                        topic = %s
                        """.formatted(topic);

                Gemini gemini = new Gemini();
                String rawReply = gemini.generateReply(prompt);

                System.out.println("RAW GEMINI REPLY:\n" + rawReply);

                if (rawReply == null || rawReply.isBlank()) {
                    throw new RuntimeException("Empty Gemini response");
                }

                String cleaned = extractJson(rawReply);

                GeminiQuestion q = gson.fromJson(cleaned, GeminiQuestion.class);

                if (q == null || q.answers == null) {
                    throw new RuntimeException("Invalid Gemini JSON");
                }

                String answersJson = gson.toJson(q.answers);

                Database.insertQuestion(
                        q.topic,
                        q.difficulty,
                        q.question,
                        answersJson,
                        q.correct_answer
                );

                String response = gson.toJson(q);

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                String error = gson.toJson(new ReplyResponse("Error: " + e.getMessage()));
                exchange.sendResponseHeaders(500, error.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        });

        // ─────────────────────────────
        // DEMO QUESTION
        // ─────────────────────────────
        server.createContext("/api/demo/question", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (var conn = Database.getConnection();
                 var stmt = conn.prepareStatement(
                         "SELECT topic, question FROM questions ORDER BY RANDOM() LIMIT 1"
                 );
                 var rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    String empty = gson.toJson(new ReplyResponse("No questions found"));
                    exchange.sendResponseHeaders(200, empty.getBytes().length);
                    exchange.getResponseBody().write(empty.getBytes());
                    return;
                }

                String prompt =
                        "Explain this clearly:\nTopic: " +
                                rs.getString("topic") +
                                "\nQuestion: " +
                                rs.getString("question");

                Gemini gemini = new Gemini();
                String reply = gemini.generateReply(prompt);

                String response = gson.toJson(new ReplyResponse(reply));

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                String error = gson.toJson(new ReplyResponse("Error: " + e.getMessage()));
                exchange.sendResponseHeaders(500, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────

    static String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start == -1 || end == -1 || end <= start) {
            System.out.println("BAD RESPONSE:\n" + text);
            throw new RuntimeException("No JSON found");
        }

        return text.substring(start, end + 1);
    }

    // ─────────────────────────────
    // DTOs
    // ─────────────────────────────
    static class ChatRequest {
        long sessionId;
        String message;
    }

    static class SessionResponse {
        long sessionId;
        SessionResponse(long id) { this.sessionId = id; }
    }

    static class ReplyResponse {
        String reply;
        ReplyResponse(String r) { this.reply = r; }
    }

    static class GeminiQuestion {
        String topic;
        int difficulty;
        String question;
        Answers answers;
        String correct_answer;

        static class Answers {
            String A;
            String B;
            String C;
            String D;
        }
    }
}