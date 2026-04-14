import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws IOException {
        int port = Config.port();
        Gemini gemini = new Gemini();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/chat", new ChatHandler(gemini));
        server.createContext("/health", exchange -> HttpResponses.sendJson(exchange, 200, "{\"status\":\"ok\"}"));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Backend listening on http://localhost:" + port);
    }
}
