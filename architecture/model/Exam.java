package architecture.model;

public class Exam {
    private final String id;
    private final String subject;
    private final int totalQuestions;
    private final boolean adaptive;

    public Exam(String id, String subject, int totalQuestions, boolean adaptive) {
        this.id = id;
        this.subject = subject;
        this.totalQuestions = totalQuestions;
        this.adaptive = adaptive;
    }

    public String getId() {
        return id;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }
}
