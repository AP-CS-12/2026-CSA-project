import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionStore {
    private final Map<String, TestSession> sessions = new ConcurrentHashMap<>();

    public TestSession createSession(String studentId, String testId) {
        String sessionId = UUID.randomUUID().toString();
        TestSession session = new TestSession(sessionId, studentId, testId, 2, 1, 5);
        sessions.put(sessionId, session);
        return session;
    }

    public TestSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
}


