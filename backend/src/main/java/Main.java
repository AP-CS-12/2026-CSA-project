import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Server starting...");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // health check
        server.createContext("/api/health", exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        // sessions endpoint (your frontend needs this)
        server.createContext("/api/sessions/start", exchange -> {

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            long studentId = Long.parseLong(
                    body.split("\"studentId\":")[1].replaceAll("[^0-9]", "")
            );

            long sessionId = Database.createSession(studentId);

            String response = "{ \"sessionId\": " + sessionId + " }";

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });

        server.start();

        System.out.println("Server running on http://localhost:8080");
    }
}