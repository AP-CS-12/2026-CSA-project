import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PerformanceTracker {

    public record Outcome(String topic, int difficulty, boolean correct, long at) {}

    private final List<Outcome> history = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, int[]> topicStats = new HashMap<>();

    public synchronized void record(String topic, int difficulty, boolean correct) {
        String key = (topic == null || topic.isBlank()) ? "(unspecified)" : topic.trim();
        history.add(new Outcome(key, difficulty, correct, System.currentTimeMillis()));
        int[] s = topicStats.computeIfAbsent(key, k -> new int[2]);
        s[1]++;
        if (correct) s[0]++;
    }

    public synchronized int totalAnswered() {
        return history.size();
    }

    public synchronized int totalCorrect() {
        int n = 0;
        for (Outcome o : history) if (o.correct()) n++;
        return n;
    }

    public synchronized String summaryForPrompt() {
        if (history.isEmpty()) {
            return "No prior answers in this session yet. Choose any topic from the unit context.";
        }

        int total = history.size();
        int correct = totalCorrect();
        double pct = 100.0 * correct / total;

        boolean lastCorrect = history.get(history.size() - 1).correct();
        int streakLen = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).correct() == lastCorrect) streakLen++;
            else break;
        }

        List<String> weak = new ArrayList<>();
        List<String> strong = new ArrayList<>();
        for (Map.Entry<String, int[]> e : topicStats.entrySet()) {
            int c = e.getValue()[0];
            int t = e.getValue()[1];
            if (t >= 2 && c * 2 < t) {
                weak.add(e.getKey() + " (" + c + "/" + t + ")");
            } else if (t >= 2 && c == t) {
                strong.add(e.getKey() + " (" + c + "/" + t + ")");
            }
        }

        LinkedHashSet<String> recentTopics = new LinkedHashSet<>();
        for (int i = history.size() - 1; i >= 0 && recentTopics.size() < 3; i--) {
            recentTopics.add(history.get(i).topic());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("STUDENT PERFORMANCE SO FAR THIS SESSION:\n");
        sb.append(" - Answered ").append(total).append(" question(s); ")
          .append(correct).append(" correct (")
          .append(String.format("%.0f", pct)).append("%).\n");
        sb.append(" - Last ").append(streakLen).append(" answer(s) ")
          .append(lastCorrect ? "correct in a row." : "incorrect in a row.").append("\n");

        if (!weak.isEmpty()) {
            sb.append(" - Struggling on: ").append(String.join(", ", weak)).append(".\n");
            sb.append(" - TARGET one of these weak topics for the next question.\n");
        }
        if (!strong.isEmpty()) {
            sb.append(" - Confident on: ").append(String.join(", ", strong)).append(".\n");
            sb.append(" - Avoid re-testing these unless the difficulty is higher than what they have already solved.\n");
        }
        sb.append(" - Recent topics shown: ").append(String.join(", ", recentTopics)).append(".\n");
        sb.append(" - DO NOT repeat any of those recent topics as the focus of the next question.\n");

        if (lastCorrect && streakLen >= 2) {
            sb.append(" - Student is on a correct streak; lean toward the harder end of the requested difficulty band.\n");
        } else if (!lastCorrect && streakLen >= 2) {
            sb.append(" - Student is missing repeatedly; keep wording crisp and avoid stacking traps on top of each other.\n");
        }

        return sb.toString();
    }
}
