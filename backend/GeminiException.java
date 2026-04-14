public final class GeminiException extends RuntimeException {
    private final int statusCode;

    public GeminiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GeminiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
