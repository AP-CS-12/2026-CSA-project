import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponses {

    private static final Gson gson = new Gson();

    private HttpResponses() {
    }

    // ─────────────────────────────
    // CORE JSON SENDER
    // ─────────────────────────────
    public static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ─────────────────────────────
    // SUCCESS WRAPPER
    // ─────────────────────────────
    public static String successReply(String reply) {
        return gson.toJson(new ReplyResponse(reply));
    }

    // ─────────────────────────────
    // ERROR WRAPPER
    // ─────────────────────────────
    public static String error(String message) {
        return gson.toJson(new ErrorResponse(message));
    }

    // ─────────────────────────────
    // DTOs
    // ─────────────────────────────
    static class ReplyResponse {
        String reply;
        ReplyResponse(String reply) {
            this.reply = reply;
        }
    }

    static class ErrorResponse {
        String error;
        ErrorResponse(String error) {
            this.error = error;
        }
    }
}