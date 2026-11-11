package smon.scan;

/** Scans shops for items: what is saved, mailed and reported is decided here, not in the commands. */
public interface ScanService {

    /**
     * Runs one scan. A failure in one shop is recorded in ERRORS and the other shops still run.
     *
     * @throws IllegalStateException when the mail could not be sent to any recipient, or the shop name is unknown
     */
    ScanOutcome scan(ScanRequest request);
}
