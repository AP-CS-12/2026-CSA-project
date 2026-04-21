public final class TestSession {
    private final String sessionId;
    private final String studentId;
    private final String testId;
    private final int totalQuestions;

    private int currentDifficulty;
    private int questionNumber;
    private GeneratedQuestion currentQuestion;

    public TestSession(String sessionId, String studentId, String testId,
                       int currentDifficulty, int questionNumber, int totalQuestions) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.testId = testId;
        this.currentDifficulty = currentDifficulty;
        this.questionNumber = questionNumber;
        this.totalQuestions = totalQuestions;
    }

    public String sessionId() { return sessionId; }
    public String studentId() { return studentId; }
    public String testId() { return testId; }
    public int currentDifficulty() { return currentDifficulty; }
    public int questionNumber() { return questionNumber; }
    public int totalQuestions() { return totalQuestions; }

    public void setCurrentQuestion(GeneratedQuestion question) {
        this.currentQuestion = question;
    }

    public GeneratedQuestion currentQuestion() {
        return currentQuestion;
    }
}
