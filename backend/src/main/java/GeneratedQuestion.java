import java.util.List;

public final class GeneratedQuestion {
    private final String questionId;
    private final String topic;
    private final String text;
    private final List<Choice> choices;
    private final String correctChoice;
    private final int difficulty;

    public GeneratedQuestion(String questionId, String topic, String text,
                             List<Choice> choices, String correctChoice, int difficulty) {
        this.questionId = questionId;
        this.topic = topic;
        this.text = text;
        this.choices = choices;
        this.correctChoice = correctChoice;
        this.difficulty = difficulty;
    }

    public String questionId() { return questionId; }
    public String topic() { return topic; }
    public String text() { return text; }
    public List<Choice> choices() { return choices; }
    public String correctChoice() { return correctChoice; }
    public int difficulty() { return difficulty; }

    public record Choice(String id, String text) {}
}
