package smon.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import org.springframework.beans.factory.annotation.Autowired;
import smon.reporting.dto.ErrorReportDto;
import smon.reporting.service.ReportingService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Command(name = "failures", description = "List recorded errors (default: today's)")
public class ReportingCommand extends BaseCommand {

    @Autowired
    private ReportingService reportingService;

    @Option(name = {"-d", "--days"}, description = "errors from the last N days, today included (1 = today, 2 = since yesterday, ...)")
    private Integer days;

    @Option(name = {"-s", "--since"}, description = "errors recorded on this date (yyyy-MM-dd) or later")
    private String since;

    @Override
    public void run() {
        LocalDate from = sinceDate(LocalDate.now());
        List<ErrorReportDto> errors = reportingService.getErrorsSince(from);
        System.out.printf("%d errors since %s%n", errors.size(), from);
        for (ErrorReportDto error : errors) {
            System.out.printf("%s  %-30s  %s%n", error.getRegistrationDate(),
                    error.getShopName() == null ? "-" : error.getShopName(), error.getErrorMessage());
            if (error.getReason() != null) {
                System.out.printf("%12s  %-30s  %s%n", "", "", error.getReason());
            }
        }
    }

    LocalDate sinceDate(LocalDate today) {
        if (days != null && since != null) {
            throw new UsageException("use either --days or --since, not both");
        }
        if (since != null) {
            try {
                return LocalDate.parse(since);
            } catch (DateTimeParseException e) {
                throw new UsageException("--since needs a date like 2026-11-01, got: " + since);
            }
        }
        if (days != null) {
            if (days < 1) {
                throw new UsageException("--days must be 1 or more");
            }
            return today.minusDays(days - 1);
        }
        return today;
    }

}
