import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final Gson gson = new Gson();
    private static final Gemini gemini = new Gemini();
    private static final SessionStore sessionStore = new SessionStore();
    private static final ExecutorService prefetchExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) throws Exception {

        System.out.println("Server starting...");

        int port = Config.port();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // FIX 1: Don't block all requests on a single thread
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // requires Java 21+

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

        server.createContext("/api/gemini", exchange -> {
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
                ChatRequest request = gson.fromJson(body, ChatRequest.class);

                if (request == null || request.message == null || request.message.isBlank()) {
                    sendJson(exchange, 400, new ErrorResponse("Request body must include a non-empty message."));
                    return;
                }

                String reply = gemini.generateReply(request.message.trim());
                sendJson(exchange, 200, new ChatResponse(reply));

            } catch (com.google.gson.JsonSyntaxException e) {
                try {
                    sendJson(exchange, 400, new ErrorResponse("Request body must be valid JSON."));
                } catch (Exception ignored) {}
            } catch (IllegalStateException e) {
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 502, new ErrorResponse("Gemini request failed."));
                } catch (Exception ignored) {}
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
                /*int unit = Integer.parseInt(getQueryParam(exchange, "unit", "3"));
                int difficulty = Integer.parseInt(getQueryParam(exchange, "difficulty", "3"));

                String unitContext = getUnitContext(unit);
                String prompt = buildPrompt(unitContext, difficulty);
                String raw = gemini.generateReply(prompt);

                GeneratedQuestion question = shuffleChoices(parseQuestion(raw, difficulty));

                Database.insertQuestion(
                        question.topic(),
                        question.difficulty(),
                        question.text(),
                        gson.toJson(question.choices()),
                        question.correctChoice()
                );

                sendJson(exchange, 200, question);*/

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/join-class", exchange -> {
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
                java.util.Map<?, ?> req = gson.fromJson(body, java.util.Map.class);

                String classId = (String) req.get("classId");
                String userId = (String) req.get("userId");

                Database.joinClass(classId, userId);

                send(exchange, 200, "{\"status\":\"joined\"}");

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/my-classes", exchange -> {
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

                String userId = getQueryParam(exchange, "userId", null);

                if (userId == null) {
                    sendJson(exchange, 400, new ErrorResponse("userId required"));
                    return;
                }

                String result = Database.getUserClasses(userId);

                send(exchange, 200, result);

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

                TestSession session = sessionStore.getSession(req.sessionId);
                if (session == null) {
                    sendJson(exchange, 404, new ErrorResponse("Session not found"));
                    return;
                }

                int correctCount = session.data().correctCount();
                int incorrectCount = session.data().incorrectCount();

                // Record the previous answer's outcome against the question we last
                // sent to this session, so the Gemini prompt can adapt to performance.
                if (req.wasCorrect != null) {
                    GeneratedQuestion prev = session.data().currentQuestion();
                    if (prev != null
                            && (req.previousQuestionId == null
                                || req.previousQuestionId.equals(prev.questionId()))) {
                        session.performance().record(prev.topic(), prev.difficulty(), req.wasCorrect);
                    }
                    if (req.wasCorrect) correctCount++;
                    else incorrectCount++;
                }

                int difficulty = req.difficulty != null
                        ? req.difficulty
                        : session.data().currentDifficulty();
                int unit = req.unit != null
                        ? req.unit
                        : session.data().currentUnit();

                // Try to consume a pre-generated question from the buffer.
                // The buffered question's difficulty may be off by ±1 from the
                // requested difficulty (it was generated before this answer was
                // known) — accepted tradeoff for hiding generation latency.
                GeneratedQuestion question = null;
                CompletableFuture<GeneratedQuestion> buffered = session.takeNextQuestion();
                if (buffered != null) {
                    try {
                        question = buffered.join();
                    } catch (Exception bufferErr) {
                        bufferErr.printStackTrace();
                        question = null;
                    }
                }

                if (question == null) {
                    String performanceSummary = session.performance().summaryForPrompt();
                    question = generateAndPersistQuestion(unit, difficulty, performanceSummary);
                }

                SessionData updated = new SessionData(
                        unit,
                        question.difficulty(),
                        question,
                        System.currentTimeMillis(),
                        correctCount,
                        incorrectCount,
                        session.data().status()
                );

                session.setData(updated);

                // Refill the buffer asynchronously so the NEXT call returns instantly.
                final int prefetchUnit = unit;
                final int prefetchDifficulty = question.difficulty();
                final String prefetchSummary = session.performance().summaryForPrompt();
                CompletableFuture<GeneratedQuestion> nextFuture = CompletableFuture.supplyAsync(
                        () -> generateAndPersistQuestion(prefetchUnit, prefetchDifficulty, prefetchSummary),
                        prefetchExecutor);
                session.setNextQuestion(nextFuture);

                sessionStore.saveSession(session.sessionId(), session);

                sendJson(exchange, 200, question);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/test-create-classroom", exchange -> {
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

                //Testing
                String result = Database.insertClassroom(
                        "Test Physics Class",
                        "Physics",
                        "Automated backend test class",
                        "System Test"
                );

                send(exchange, 200, result);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/start-session", exchange -> {
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
                java.util.Map<?, ?> req = gson.fromJson(body, java.util.Map.class);

                String studentId = (String) req.get("studentId");
                String testId = (String) req.get("testId");

                TestSession session = sessionStore.createSession(studentId, testId);

                sendJson(exchange, 200, session);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendJson(exchange, 500, new ErrorResponse(e.getMessage()));
                } catch (Exception ignored) {}
            }
        });

        server.createContext("/api/resume-session", exchange -> {
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
                java.util.Map<?, ?> req = gson.fromJson(body, java.util.Map.class);

                String sessionId = (String) req.get("sessionId");

                TestSession session = sessionStore.getSession(sessionId);

                if (session == null) {
                    sendJson(exchange, 404, new ErrorResponse("Session not found"));
                    return;
                }

                sendJson(exchange, 200, session);

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

    private static GeneratedQuestion generateAndPersistQuestion(int unit, int difficulty, String performanceSummary) {
        String unitContext = getUnitContext(unit);
        String prompt = buildPrompt(unitContext, difficulty, performanceSummary);
        String raw = gemini.generateReply(prompt);

        GeneratedQuestion question = shuffleChoices(parseQuestion(raw, difficulty));

        Database.insertQuestion(
                question.topic(),
                question.difficulty(),
                question.text(),
                gson.toJson(question.choices()),
                question.correctChoice()
        );

        return question;
    }

    private static String getUnitContext(int unit) {

        String sql = "SELECT title, content FROM ap_csa_units WHERE unit = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unit);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");

                return """
                        AP CSA CURRICULUM CONTEXT

                        UNIT: %d
                        TITLE: %s

                        CONTENT:
                        %s
                        """.formatted(unit, title, content);
            }

            System.err.println("Unit " + unit + " not found in ap_csa_units; using fallback context.");

        } catch (Exception e) {
            System.err.println("Failed to fetch unit " + unit + " from Supabase: " + e.getMessage());
        }

        // Fallback so question generation still proceeds when the units table is unseeded or unreachable.
        return """
                AP CSA CURRICULUM CONTEXT

                UNIT: %d (fallback — unit data unavailable)
                TITLE: General AP Computer Science A

                CONTENT:
                Standard AP Computer Science A topics:
                - Primitive types and using objects
                - Boolean expressions and if statements
                - Iteration (for, while)
                - Writing classes (instance variables, constructors, methods)
                - Array and ArrayList
                - 2D Array
                - Inheritance
                - Recursion
                """.formatted(unit);
    }

    private static String buildPrompt(String unitContext, int difficulty, String performanceSummary) {
        return """
               TASK: JSON_OUTPUT_GENERATION

               MODE: STRICT

               CURRICULUM CONTEXT:
                %s

               PERFORMANCE-DRIVEN ADAPTATION (use this to choose the topic):
                %s
                - The student's pattern above is the PRIMARY signal for which topic to test next.
                - The requested difficulty level (below) is the secondary signal — honor it exactly.
                - Do NOT mention this performance data to the student in the question text.

               HARD RULE:
                - Only generate questions based on the provided unit context
                - Do NOT introduce outside topics
                - Stay strictly within AP CSA material in the context

               QUESTION STYLE RULES:
                - DO NOT overuse "What is the output of this code?"-style questions
                - AT MOST 25 percent of generated questions may be output-prediction style
                - Prefer conceptual reasoning, logic tracing, and design-based questions
                - Preferred questions involve:
                  - algorithm reasoning
                  - edge cases
                  - debugging incorrect code
                  - comparing multiple implementations
                  - time/space reasoning (conceptual, not Big-O heavy)
                  - object-oriented behavior and interactions
               - The question must test understanding of a specific concept from the unit
               - The concept being tested should be inferable but not explicitly stated anywhere

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
               - The problem MUST include at least one non-obvious flaw, edge case, or interaction
               - The correct answer MUST require identifying a subtle detail (not surface-level reading)
               - The question MUST NOT be solvable by quick inspection
               - The reasoning path should involve at least 2–3 logical steps

               difficulty = 5:
               - REQUIRES highly complex, multi-layer reasoning that goes well beyond AP CSA content while remaining Java-based
               - combines multiple AP CSA topics
               - may include:
                 - nested logic interactions
                 - tricky object behavior
                 - side effects or mutation
                 - non-obvious edge cases
               - answer choices must be VERY close and require deep understanding
               - MUST take significant time for a student to solve
               - The problem MUST involve a hidden pitfall (e.g., off-by-one, mutation side effects, recursion boundary error, object aliasing)

               CREATE THE QUESTION BASED ON DIFFICULTY LEVEL %d.

               RETURN: JSON_ONLY
               """.formatted(unitContext, performanceSummary, difficulty);
    }

    private static GeneratedQuestion shuffleChoices(GeneratedQuestion q) {

        java.util.List<GeneratedQuestion.Choice> shuffled =
                new java.util.ArrayList<>(q.choices());
        java.util.Collections.shuffle(shuffled);

        // FIX 3: removed dead first newCorrect assignment; only one pass needed
        String[] labels = {"A", "B", "C", "D"};
        java.util.List<GeneratedQuestion.Choice> relabeled = new java.util.ArrayList<>();
        String newCorrect = null;

        for (int i = 0; i < shuffled.size(); i++) {
            GeneratedQuestion.Choice c = shuffled.get(i);
            String newId = labels[i];
            if (c.id().equals(q.correctChoice())) {
                newCorrect = newId;
            }
            relabeled.add(new GeneratedQuestion.Choice(newId, c.text()));
        }

        return new GeneratedQuestion(
                q.questionId(),
                q.topic(),
                q.text(),
                relabeled,
                newCorrect,
                q.difficulty()
        );
    }

    private static GeneratedQuestion parseQuestion(String raw, int difficulty) {

        System.out.println("RAW GEMINI RESPONSE");
        System.out.println("Difficulty: " + difficulty);
        System.out.println(raw);

        String json = extractJson(raw);
        Object obj = gson.fromJson(json, Object.class);
        java.util.Map<?, ?> root = (java.util.Map<?, ?>) obj;

        String topic = require(root, "topic");
        String text = require(root, "text");
        String correct = require(root, "correctChoice");

        java.util.List<?> list = (java.util.List<?>) root.get("choices");
        if (list == null || list.size() != 4) {
            throw new IllegalArgumentException("Must have 4 choices");
        }

        java.util.List<GeneratedQuestion.Choice> choices = new java.util.ArrayList<>();
        for (Object c : list) {
            java.util.Map<?, ?> cm = (java.util.Map<?, ?>) c;
            choices.add(new GeneratedQuestion.Choice(require(cm, "id"), require(cm, "text")));
        }

        boolean valid = choices.stream().anyMatch(c -> c.id().equals(correct));
        if (!valid) throw new IllegalArgumentException("correctChoice mismatch");

        return new GeneratedQuestion(
                java.util.UUID.randomUUID().toString(),
                topic, text, choices, correct, difficulty
        );
    }

    private static String require(java.util.Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return s.trim();
    }

    // FIX 4: was splitting on "=" which breaks values containing "="
    private static String getQueryParam(HttpExchange ex, String key, String def) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return def;
        for (String part : q.split("&")) {
            if (part.startsWith(key + "=")) {
                return URLDecoder.decode(part.substring(key.length() + 1), StandardCharsets.UTF_8);
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
        String sessionId;
        Integer difficulty;
        Integer unit;
        // Performance feedback for adaptive prompting. Sent by test.html
        // alongside the next-question request after each committed answer.
        String previousQuestionId;
        String selectedChoice;
        Boolean wasCorrect;
    }

    static class ChatRequest {
        String message;
    }

    static class ChatResponse {
        String reply;
        ChatResponse(String reply) { this.reply = reply; }
    }

    static class ErrorResponse {
        String error;
        ErrorResponse(String e) { this.error = e; }
    }
}
