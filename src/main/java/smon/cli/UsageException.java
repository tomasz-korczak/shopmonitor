package smon.cli;

/** Invalid combination of command line options; the process exits with code 2 (Application.EXIT_USAGE). */
public class UsageException extends RuntimeException {
    public UsageException(String message) {
        super(message);
    }
}
