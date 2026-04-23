import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class NextQuestionHandler implements HttpHandler {

    private final SessionStore sessionStore;
    private final Gemini gemini;

    public NextQuestionHandler(SessionStore sessionStore, Gemini gemini) {
        this.sessionStore = sessionStore;
        this.gemini = gemini;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            throw new UnsupportedOperationException("Move logic to Main");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}