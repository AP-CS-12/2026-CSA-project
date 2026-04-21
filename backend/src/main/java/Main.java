import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;
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
        // CHAT (FIXED + GENERALIZED PROMPT)
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
                    "You are a strict JSON generator.\n" +
                            "You MUST output ONLY valid JSON. No markdown. No explanation. No extra text.\n\n" +
                            "Schema:\n" +
                            "{\n" +
                            "  \"topic\": string,\n" +
                            "  \"difficulty\": integer (1-5),\n" +
                            "  \"question\": string,\n" +
                            "  \"answers\": {\"A\": string, \"B\": string, \"C\": string, \"D\": string},\n" +
                            "  \"correct_answer\": \"A\" | \"B\" | \"C\" | \"D\"\n" +
                            "}\n\n" +
                            "Generate a question about: " + topic;

            Gemini gemini = new Gemini();
            String rawReply = gemini.generateReply(prompt);

            String cleaned = extractJson(rawReply);

            GeminiQuestion q = gson.fromJson(cleaned, GeminiQuestion.class);

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
        // GENERATE QUESTION (FIXED + SAFE INSERT)
        // ─────────────────────────────
        server.createContext("/api/generate-question", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String prompt =
                        "Return ONLY valid JSON.\n" +
                                "Schema:\n" +
                                "{ \"topic\": string, \"difficulty\": 1-5, \"question\": string, " +
                                "\"answers\": {\"A\":string,\"B\":string,\"C\":string,\"D\":string}, " +
                                "\"correct_answer\": \"A\" }\n\n" +
                                "No markdown. No explanation. Only JSON.";

                Gemini gemini = new Gemini();
                String rawReply = gemini.generateReply(prompt);

                String cleaned = extractJson(stripMarkdown(rawReply));

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
                exchange.getResponseBody().write(error.getBytes());
                exchange.close();
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
            throw new RuntimeException("No JSON found");
        }
        return text.substring(start, end + 1);
    }

    static String stripMarkdown(String text) {
        return text.replace("```json", "")
                .replace("```", "")
                .trim();
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