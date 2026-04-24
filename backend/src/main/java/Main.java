import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class Main {

    private static final Gson gson = new Gson();
    private static final Gemini gemini = new Gemini();
    private static final SessionStore sessionStore = new SessionStore();

    public static void main(String[] args) throws Exception {

        System.out.println("Server starting...");

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) port = Integer.parseInt(portEnv);

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/api/health", exchange -> {
            try {
                cors(exchange);

                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                send(exchange, 200, "OK");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        server.createContext("/api/generate-question", exchange -> {
            try {
                cors(exchange);

                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }


                //For testing only
                int unit = Integer.parseInt(getQueryParam(exchange, "unit", "10"));
                int difficulty = Integer.parseInt(getQueryParam(exchange, "difficulty", "5"));

                String unitContext = getUnitContext(unit);

                String prompt = buildPrompt(unitContext, difficulty);

                String raw = gemini.generateReply(prompt);

                GeneratedQuestion question = parseQuestion(raw, difficulty);

                Database.insertQuestion(
                        question.topic(),
                        question.difficulty(),
                        question.text(),
                        gson.toJson(question.choices()),
                        question.correctChoice()
                );

                sendJson(exchange, 200, question);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/next-question", exchange -> {
            try {
                cors(exchange);

                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                SessionRequest req = gson.fromJson(body, SessionRequest.class);

                TestSession session = sessionStore.getSession(String.valueOf(req.sessionId));
                if (session == null) {
                    sendJson(exchange, 404, new ErrorResponse("Session not found"));
                    return;
                }

                int difficulty = session.currentDifficulty();

                String prompt = buildPrompt("Java programming", difficulty);
                String raw = gemini.generateReply(prompt);

                GeneratedQuestion question = parseQuestion(raw, difficulty);
                session.setCurrentQuestion(question);

                sendJson(exchange, 200, question);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
    }

    private static String getUnitContext(int unit) {

        String sql = "SELECT title, content FROM ap_csa_units WHERE unit = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unit);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new RuntimeException("Unit not found: " + unit);
            }

            String title = rs.getString("title");
            String content = rs.getString("content");

            return """
                    AP CSA CURRICULUM CONTEXT

                    UNIT: %d
                    TITLE: %s

                    CONTENT:
                    %s
                    """.formatted(unit, title, content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch unit from Supabase", e);
        }
    }

    private static String buildPrompt(String unitContext, int difficulty) {
        return """
               TASK: JSON_OUTPUT_GENERATION

               MODE: STRICT

               CURRICULUM CONTEXT:
                %s

               HARD RULE:
                - Only generate questions based on the provided unit context
                - Do NOT introduce outside topics
                - Stay strictly within AP CSA material in the context

               QUESTION STYLE RULES:
                - DO NOT overuse \"What is the output of this code?\"
                - AT MOST 25 percent of generated questions may be output-prediction style
                - Prefer conceptual reasoning, logic tracing, and design-based questions
                - Prefer questions involving:
                  - algorithm reasoning
                  - edge cases
                  - debugging incorrect code
                  - comparing multiple implementations
                  - time/space reasoning (conceptual, not Big-O heavy)
                  - object-oriented behavior and interactions

               QUESTION VARIETY REQUIREMENT:
                The question MUST be one of the following types:
                - Conceptual reasoning (no code required)
                - Code comprehension with reasoning (NOT just output)
                - Debugging / error identification
                - Code completion / missing logic
                - Object interaction / method behavior
                - Scenario-based problem solving
                Reject simple recall or trivial output questions.

               ANSWER CHOICE RULES:
                - All incorrect choices must be plausible
                - Avoid obviously wrong answers
                - Distractors should reflect common student mistakes
                - For high difficulty (4–5), choices should be very similar, forcing students to think even harder

               INTERNAL VALIDATION (DO NOT OUTPUT):
                 - Ensure the question cannot be answered in under 10 seconds
                 - Ensure at least one incorrect choice is a common misconception
                 - Ensure the correct answer is unambiguous

               OUTPUT_RULES:
                - Output must be valid JSON
                - Output must contain no text outside JSON
                - Output must not include markdown, comments, or backticks
                - Output must begin with { and end with }

               SCHEMA:
                {
                  "topic": string,
                  "text": string,
                  "choices": [
                    {"id":"A","text":string},
                    {"id":"B","text":string},
                    {"id":"C","text":string},
                    {"id":"D","text":string}
                  ],
                  "correctChoice": "A" | "B" | "C" | "D"
                }

                Question difficulty is based on this proficiency scale:
                DIFFICULTY RULES:
                
               difficulty = 1:
               - single concept
               - no traps
               - direct recall or simple application

               difficulty = 2:
               - 1–2 concepts
               - minimal reasoning

               difficulty = 3:
               - multiple steps
               - requires careful reading
               - may include small traps

               difficulty = 4:
               - multi-step reasoning required
               - involves interaction between multiple concepts
               - includes subtle edge cases or misleading answer choices
               - requires tracing logic across multiple lines or methods
               - incorrect answers must be plausible

               difficulty = 5:
               - highly complex, multi-layer reasoning
               - combines multiple AP CSA topics
               - may include:
                 - nested logic interactions
                 - tricky object behavior
                 - side effects or mutation
                 - non-obvious edge cases
               - answer choices must be VERY close and require deep understanding
               - may go slightly beyond curriculum, but still Java-based
               - MUST take significant time for a student to solve
               
               CREATE THE QUESTION BASED ON DIFFICULTY LEVEL %d.

                RETURN: JSON_ONLY
               """.formatted(unitContext, difficulty);
    }

    private static GeneratedQuestion parseQuestion(String raw, int difficulty) {

        System.out.println("RAW GEMINI RESPONSE");
        System.out.println("Difficulty: "+ difficulty);
        System.out.println(raw);

        String json = extractJson(raw);
        Object obj = gson.fromJson(json, Object.class);

        java.util.Map<?, ?> root = (java.util.Map<?, ?>) obj;

        String topic = require(root, "topic");
        String text = require(root, "text");
        String correct = require(root, "correctChoice");

        Object choicesObj = root.get("choices");
        java.util.List<?> list = (java.util.List<?>) choicesObj;

        if (list == null || list.size() != 4) {
            throw new IllegalArgumentException("Must have 4 choices");
        }

        java.util.List<GeneratedQuestion.Choice> choices = new java.util.ArrayList<>();

        for (Object c : list) {
            java.util.Map<?, ?> cm = (java.util.Map<?, ?>) c;

            choices.add(new GeneratedQuestion.Choice(
                    require(cm, "id"),
                    require(cm, "text")
            ));
        }

        boolean valid = choices.stream().anyMatch(c -> c.id().equals(correct));
        if (!valid) throw new IllegalArgumentException("correctChoice mismatch");

        return new GeneratedQuestion(
                java.util.UUID.randomUUID().toString(),
                topic,
                text,
                choices,
                correct,
                difficulty
        );
    }

    private static String require(java.util.Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return s.trim();
    }

    private static String getQueryParam(HttpExchange ex, String key, String def) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return def;

        for (String part : q.split("&")) {
            if (part.startsWith(key + "=")) {
                return URLDecoder.decode(part.split("=")[1], StandardCharsets.UTF_8);
            }
        }
        return def;
    }

    private static String extractJson(String text) {
        if (text == null) throw new RuntimeException("Empty Gemini response");

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start == -1 || end == -1 || end <= start) {
            System.out.println("RAW GEMINI OUTPUT");
            System.out.println(text);
            throw new RuntimeException("No JSON found in Gemini response");
        }

        return text.substring(start, end + 1);
    }

    private static void cors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    private static void send(HttpExchange ex, int code, String body) throws Exception {
        ex.sendResponseHeaders(code, body.getBytes().length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body.getBytes());
        }
    }

    private static void sendJson(HttpExchange ex, int code, Object obj) throws Exception {
        String json = gson.toJson(obj);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        send(ex, code, json);
    }

    static class SessionRequest {
        long sessionId;
    }

    static class ErrorResponse {
        String error;
        ErrorResponse(String e) { this.error = e; }
    }
}