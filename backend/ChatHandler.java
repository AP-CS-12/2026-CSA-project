import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ChatHandler implements HttpHandler {
    private final Gemini gemini;

    public ChatHandler(Gemini gemini) {
        this.gemini = gemini;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpResponses.sendJson(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(method)) {
                HttpResponses.sendJson(exchange, 405, HttpResponses.error("Method not allowed."));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Object parsed = Json.parse(body);
            if (!(parsed instanceof Map<?, ?> payload)) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("Request body must be a JSON object."));
                return;
            }

            Object messageValue = payload.get("message");
            if (!(messageValue instanceof String message) || message.isBlank()) {
                HttpResponses.sendJson(exchange, 400, HttpResponses.error("`message` is required."));
                return;
            }

            String reply = gemini.generateReply(message.trim());
            HttpResponses.sendJson(exchange, 200, HttpResponses.successReply(reply));
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
}
