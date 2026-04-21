import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NextQuestionHandler implements HttpHandler {
    private final SessionStore sessionStore;
    private final Gemini gemini;

    public NextQuestionHandler(SessionStore sessionStore, Gemini gemini) {
        this.sessionStore = sessionStore;
        this.gemini = gemini;
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

            Object sessionIdValue = payload.get("sessionId");
            if (!(sessionIdValue instanceof String sessionId) || sessionId.isBlank()) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("`sessionId` is required."));
                return;
            }

            TestSession session = sessionStore.getSession(sessionId.trim());
            if (session == null) {
                HttpResponses.sendJson(exchange, 404, HttpResponses.error("Session not found."));
                return;
            }

            String prompt = buildPrompt(session.currentDifficulty());
            String reply = gemini.generateReply(prompt);
            GeneratedQuestion question = parseQuestion(reply, session.currentDifficulty());

            session.setCurrentQuestion(question);

            String json = toFrontendJson(session, question);
            HttpResponses.sendJson(exchange, 200, json);
        } catch (IllegalArgumentException ex) {
            HttpResponses.sendJson(exchange, 400, HttpResponses.error(ex.getMessage()));
        } catch (GeminiException ex) {
            int statusCode = ex.statusCode() > 0 ? ex.statusCode() : 502;
            HttpResponses.sendJson(exchange, statusCode, HttpResponses.error(ex.getMessage()));
        } catch (Exception ex) {
            HttpResponses.sendJson(exchange, 500, HttpResponses.error("Internal server error."));
        } finally {
            exchange.close();
        }
    }

    private String buildPrompt(int difficulty) {
        return """
                Generate exactly 1 AP Computer Science A multiple-choice question.

                Requirements:
                - Topic: Java programming
                - Difficulty: %d on a scale of 1 to 3
                - 4 answer choices labeled A, B, C, D
                - Exactly 1 correct answer
                - Appropriate for a high school AP CSA student
                - No markdown
                - Return valid JSON only

                JSON format:
                {
                  "topic": "string",
                  "text": "string",
                  "choices": [
                    {"id":"A","text":"string"},
                    {"id":"B","text":"string"},
                    {"id":"C","text":"string"},
                    {"id":"D","text":"string"}
                  ],
                  "correctChoice": "A"
                }
                """.formatted(difficulty);
    }

    private GeneratedQuestion parseQuestion(String rawReply, int difficulty) {
        String cleaned = stripCodeFences(rawReply);
        Object parsed = Json.parse(cleaned);

        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Gemini returned an invalid question object.");
        }

        String topic = requireString(root, "topic");
        String text = requireString(root, "text");
        String correctChoice = requireString(root, "correctChoice");

        Object choicesValue = root.get("choices");
        if (!(choicesValue instanceof List<?> rawChoices) || rawChoices.size() != 4) {
            throw new IllegalArgumentException("Question must include exactly 4 choices.");
        }

        List<GeneratedQuestion.Choice> choices = new ArrayList<>();
        for (Object item : rawChoices) {
            if (!(item instanceof Map<?, ?> choiceMap)) {
                throw new IllegalArgumentException("Each choice must be an object.");
            }
            String id = requireString(choiceMap, "id");
            String choiceText = requireString(choiceMap, "text");
            choices.add(new GeneratedQuestion.Choice(id, choiceText));
        }

        boolean hasCorrect = choices.stream().anyMatch(choice -> choice.id().equals(correctChoice));
        if (!hasCorrect) {
            throw new IllegalArgumentException("Correct answer must match one of the choice IDs.");
        }

        return new GeneratedQuestion(
                UUID.randomUUID().toString(),
                topic,
                text,
                choices,
                correctChoice,
                difficulty
        );
    }

    private String requireString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing or invalid `" + key + "`.");
        }
        return text.trim();
    }

    private String stripCodeFences(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return cleaned;
    }

    private String toFrontendJson(TestSession session, GeneratedQuestion question) {
        StringBuilder choicesJson = new StringBuilder("[");
        for (int i = 0; i < question.choices().size(); i++) {
            GeneratedQuestion.Choice choice = question.choices().get(i);
            if (i > 0) choicesJson.append(',');
            choicesJson.append("""
                    {"id":"%s","text":"%s"}"""
                    .formatted(Json.escape(choice.id()), Json.escape(choice.text())));
        }
        choicesJson.append(']');

        return """
                {
                  "questionId":"%s",
                  "questionNumber":%d,
                  "difficulty":%d,
                  "topic":"%s",
                  "text":"%s",
                  "choices":%s
                }
                """.formatted(
                Json.escape(question.questionId()),
                session.questionNumber(),
                question.difficulty(),
                Json.escape(question.topic()),
                Json.escape(question.text()),
                choicesJson
        );
    }
}
