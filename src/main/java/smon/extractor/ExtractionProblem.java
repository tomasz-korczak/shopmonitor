package smon.extractor;

/**
 * Something that went wrong while extracting a shop. The extractor only describes it; the caller records it
 * (ERRORS table) or prints it.
 */
public final class ExtractionProblem {

    private final String message;
    private final String reason;
    private final Throwable cause;

    /**
     * @param message what went wrong, for the error report
     * @param reason  a short explanation, or null
     * @param cause   the exception, or null
     */
    public ExtractionProblem(String message, String reason, Throwable cause) {
        this.message = message;
        this.reason = reason;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public String getReason() {
        return reason;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return message + (reason != null ? " (" + reason + ")" : "") + (cause != null ? ": " + cause : "");
    }
}
