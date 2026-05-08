import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class TestSession {

    private final String sessionId;
    private final String studentId;
    private final String testId;
    private final transient PerformanceTracker performance;
    private final transient AtomicReference<CompletableFuture<GeneratedQuestion>> nextQuestion =
            new AtomicReference<>();
    private SessionData data;

    public TestSession(String sessionId, String studentId, String testId, SessionData data) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.testId = testId;
        this.performance = new PerformanceTracker();
        this.data = data;
    }

    public String sessionId() { return sessionId; }
    public String studentId() { return studentId; }
    public String testId() { return testId; }

    public SessionData data() { return data; }

    public PerformanceTracker performance() { return performance; }

    public void setData(SessionData data) {
        this.data = data;
    }

    public CompletableFuture<GeneratedQuestion> takeNextQuestion() {
        return nextQuestion.getAndSet(null);
    }

    public void setNextQuestion(CompletableFuture<GeneratedQuestion> future) {
        nextQuestion.set(future);
    }
}