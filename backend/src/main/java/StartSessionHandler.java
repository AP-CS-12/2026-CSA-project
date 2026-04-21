import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class StartSessionHandler implements HttpHandler {
    private final SessionStore sessionStore;

    public StartSessionHandler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpResponses.sendJson(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpResponses.sendJson(exchange, 405, HttpResponses.error("Method not allowed."));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Object parsed = Json.parse(body);
            if (!(parsed instanceof Map<?, ?> payload)) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("Request body must be a JSON object."));
                return;
            }

            Object studentIdValue = payload.get("studentId");
            Object testIdValue = payload.get("testId");

            if (!(studentIdValue instanceof String studentId) || studentId.isBlank()) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("`studentId` is required."));
                return;
            }
            if (!(testIdValue instanceof String testId) || testId.isBlank()) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("`testId` is required."));
                return;
            }

            TestSession session = sessionStore.createSession(studentId.trim(), testId.trim());

            String json = """
                    {
                      "sessionId":"%s",
                      "studentId":"%s",
                      "testId":"%s",
                      "currentDifficulty":%d,
                      "questionNumber":%d,
                      "totalQuestions":%d
                    }
                    """.formatted(
                    Json.escape(session.sessionId()),
                    Json.escape(session.studentId()),
                    Json.escape(session.testId()),
                    session.currentDifficulty(),
                    session.questionNumber(),
                    session.totalQuestions()
            );

            HttpResponses.sendJson(exchange, 200, json);
        } catch (IllegalArgumentException ex) {
            HttpResponses.sendJson(exchange, 400, HttpResponses.error(ex.getMessage()));
        } finally {
            exchange.close();
        }
    }
}
