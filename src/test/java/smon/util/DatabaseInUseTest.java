package smon.util;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInUseTest {

    @Test
    void bareSqlExceptionWithTheH2CodeIsDetected() {
        assertThat(DatabaseInUse.isDatabaseInUse(new SQLException("in use", "90020", 90020))).isTrue();
    }

    @Test
    void detectedUnderSeveralWrappers() {
        Throwable wrapped = new IllegalStateException("startup",
                new RuntimeException("pool",
                        new SQLException("in use", "90020", 90020)));
        assertThat(DatabaseInUse.isDatabaseInUse(wrapped)).isTrue();
    }

    @Test
    void otherSqlCodeIsNotDetected() {
        assertThat(DatabaseInUse.isDatabaseInUse(new SQLException("syntax", "42000", 42000))).isFalse();
    }

    @Test
    void unrelatedExceptionIsNotDetected() {
        assertThat(DatabaseInUse.isDatabaseInUse(new IllegalStateException("boom"))).isFalse();
        assertThat(DatabaseInUse.isDatabaseInUse(null)).isFalse();
    }

    @Test
    void selfReferencingCauseDoesNotLoopForever() {
        // initCause refuses self-causation, so the cycle is made by overriding getCause
        RuntimeException loop = new RuntimeException("loop") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };
        assertThat(DatabaseInUse.isDatabaseInUse(loop)).isFalse();
    }
}
