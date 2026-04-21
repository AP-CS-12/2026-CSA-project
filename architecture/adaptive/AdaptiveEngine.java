package architecture.adaptive;

public class AdaptiveEngine {
    private final int minimumDifficulty;
    private final int maximumDifficulty;

    public AdaptiveEngine(int minimumDifficulty, int maximumDifficulty) {
        this.minimumDifficulty = minimumDifficulty;
        this.maximumDifficulty = maximumDifficulty;
    }

    public int getNextDifficulty(int currentDifficulty, boolean correct) {
        if (correct) {
            return Math.min(maximumDifficulty, currentDifficulty + 1);
        }
        return Math.max(minimumDifficulty, currentDifficulty - 1);
    }
}
