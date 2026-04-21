package architecture.model;

import architecture.adaptive.AdaptiveEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestSession {
    private final String id;
    private final StudentProfile student;
    private final Exam exam;
    private final AdaptiveEngine adaptiveEngine;
    private final List<AnswerRecord> answers;

    private Question currentQuestion;
    private int currentQuestionNumber;
    private int currentDifficulty;
    private boolean started;
    private boolean finished;

    public TestSession(String id, StudentProfile student, Exam exam,
                       AdaptiveEngine adaptiveEngine, int startingDifficulty) {
        this.id = id;
        this.student = student;
        this.exam = exam;
        this.adaptiveEngine = adaptiveEngine;
        this.currentDifficulty = startingDifficulty;
        this.answers = new ArrayList<>();
    }

    public void start(Question firstQuestion) {
        started = true;
        currentQuestionNumber = 1;
        currentQuestion = firstQuestion;
    }

    public AnswerRecord submitAnswer(String selectedAnswer, int timeSpentSeconds) {
        if (!started || finished || currentQuestion == null) {
            throw new IllegalStateException("The session is not ready to accept answers.");
        }

        AnswerRecord record = new AnswerRecord(currentQuestion, selectedAnswer, timeSpentSeconds);
        answers.add(record);
        currentDifficulty = adaptiveEngine.getNextDifficulty(currentDifficulty, record.wasCorrect());

        if (currentQuestionNumber >= exam.getTotalQuestions()) {
            finished = true;
            currentQuestion = null;
        }

        return record;
    }

    public void submitAnswerAndMoveToNextQuestion(String selectedAnswer, int timeSpentSeconds, Question nextQuestion) {
        submitAnswer(selectedAnswer, timeSpentSeconds);
        if (!finished) {
            currentQuestionNumber++;
            currentQuestion = nextQuestion;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCurrentDifficulty() {
        return currentDifficulty;
    }

    public int getCurrentQuestionNumber() {
        return currentQuestionNumber;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public int getScore() {
        int score = 0;
        for (AnswerRecord answer : answers) {
            if (answer.wasCorrect()) {
                score++;
            }
        }
        return score;
    }

    public int getPercent() {
        if (answers.isEmpty()) {
            return 0;
        }
        return (int) Math.round((getScore() * 100.0) / exam.getTotalQuestions());
    }

    public Result finish() {
        finished = true;

        int totalTimeSeconds = 0;
        for (AnswerRecord answer : answers) {
            totalTimeSeconds += answer.getTimeSpentSeconds();
        }

        return new Result(
                exam.getId(),
                getScore(),
                exam.getTotalQuestions(),
                currentDifficulty,
                totalTimeSeconds
        );
    }

    public String getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public Exam getExam() {
        return exam;
    }

    public List<AnswerRecord> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}
