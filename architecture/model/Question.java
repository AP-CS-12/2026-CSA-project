package architecture.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    private final String id;
    private final String text;
    private final List<String> choices;
    private final String correctAnswer;
    private final int difficulty;
    private final String topic;

    public Question(String id, String text, List<String> choices, String correctAnswer,
                    int difficulty, String topic) {
        this.id = id;
        this.text = text;
        this.choices = new ArrayList<>(choices);
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
        this.topic = topic;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public List<String> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    public int getDifficulty() {
        return difficulty;
    }

    boolean matchesAnswer(String answer) {
        return correctAnswer.equals(answer);
    }
}
