package architecture.model;

public class AnswerRecord {
    private final Question question;
    private final String selectedAnswer;
    private final int timeSpentSeconds;

    public AnswerRecord(Question question, String selectedAnswer, int timeSpentSeconds) {
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public Question getQuestion() {
        return question;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public boolean wasCorrect() {
        return question.matchesAnswer(selectedAnswer);
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }
}
