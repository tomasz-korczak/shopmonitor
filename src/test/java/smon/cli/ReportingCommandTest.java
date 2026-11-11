package smon.cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportingCommandTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    private static LocalDate since(Integer days, String since) throws Exception {
        ReportingCommand command = new ReportingCommand();
        set(command, "days", days);
        set(command, "since", since);
        return command.sinceDate(TODAY);
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void defaultIsToday() throws Exception {
        assertThat(since(null, null)).isEqualTo(TODAY);
    }

    @Test
    void daysIncludeToday() throws Exception {
        assertThat(since(1, null)).isEqualTo(TODAY);
        assertThat(since(2, null)).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(since(30, null)).isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    void sinceDate() throws Exception {
        assertThat(since(null, "2026-11-01")).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void invalidOptionsAreUsageErrors() {
        assertThatThrownBy(() -> since(0, null)).isInstanceOf(UsageException.class);
        assertThatThrownBy(() -> since(2, "2026-11-01")).isInstanceOf(UsageException.class);
        assertThatThrownBy(() -> since(null, "1.11.2026")).isInstanceOf(UsageException.class);
    }
}
