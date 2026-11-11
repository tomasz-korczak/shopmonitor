package smon.util;

import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;

/** Recognises "the H2 database file is already open in another process" (M7). */
public final class DatabaseInUse {

    /** H2 error code: "Database may be already in use" (checked with H2 1.4.200). */
    static final int H2_DATABASE_IN_USE = 90020;

    private DatabaseInUse() {
    }

    /**
     * @return true when the throwable or one of its causes is an {@link SQLException} with H2's "in use" code.
     *         Walks the whole cause chain, since Spring wraps the SQLException in several layers.
     */
    public static boolean isDatabaseInUse(Throwable throwable) {
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();
        for (Throwable t = throwable; t != null && seen.put(t, Boolean.TRUE) == null; t = t.getCause()) {
            if (t instanceof SQLException && ((SQLException) t).getErrorCode() == H2_DATABASE_IN_USE) {
                return true;
            }
        }
        return false;
    }
}
