import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Server starting...");

        // Cloud Run provides PORT, fallback to 8080 for local testing
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 8080;

        // MUST bind to 0.0.0.0 for Cloud Run
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/api/health", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "text/plain");

            String response = "OK";

            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        // ─────────────────────────────────────────────
        // Start session endpoint
        // ─────────────────────────────────────────────
        server.createContext("/api/sessions/start", exchange -> {

            // CORS headers (required for browser frontend)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

            // Handle preflight request
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Only allow POST
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Read request body
            String body = new String(exchange.getRequestBody().readAllBytes());

            // SIMPLE parsing (replace with JSON library later if needed)
            String cleaned = body.replaceAll("[^0-9]", "");
            long studentId = Long.parseLong(cleaned);

            // Create session in DB
            long sessionId = Database.createSession(studentId);

            // Build JSON response
            String response = "{ \"sessionId\": " + sessionId + " }";

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.start();
        System.out.println("Server running on port " + port);

        //Test
        Database.insertQuestion(
                "Recursion",
                100,
                "What is recursion?",
                "{\"A\": \"Answer 1\", \"B\": \"Answer 2\", \"C\": \"Answer 3\", \"D\": \"Answer 4\"}",
                "A"
        );
    }
}