package architecture.model;

public class Result {
    private final String examId;
    private final int correctAnswers;
    private final int totalQuestions;
    private final int finalAdaptiveLevel;
    private final int completionTimeSeconds;

    public Result(String examId, int correctAnswers, int totalQuestions,
                  int finalAdaptiveLevel, int completionTimeSeconds) {
        this.examId = examId;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.finalAdaptiveLevel = finalAdaptiveLevel;
        this.completionTimeSeconds = completionTimeSeconds;
    }

    public String getExamId() {
        return examId;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public int getFinalAdaptiveLevel() {
        return finalAdaptiveLevel;
    }

    public int getCompletionTimeSeconds() {
        return completionTimeSeconds;
    }

    public int getPercent() {
        if (totalQuestions == 0) {
            return 0;
        }
        return (int) Math.round((correctAnswers * 100.0) / totalQuestions);
    }
}
