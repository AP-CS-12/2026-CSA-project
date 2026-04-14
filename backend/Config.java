public final class Config {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_MODEL = "gemini-3-flash-preview";

    private Config() {
    }

    public static int port() {
        String raw = System.getenv("PORT");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("PORT must be a valid integer.", ex);
        }
    }

    public static String geminiApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKey.trim();
    }

    public static String geminiModel() {
        String model = System.getenv("GEMINI_MODEL");
        if (model == null || model.isBlank()) {
            return DEFAULT_MODEL;
        }
        return model.trim();
    }
}
