package smon.reporting.service;

import smon.reporting.dto.ErrorReportDto;

import java.time.LocalDate;
import java.util.List;

public interface ReportingService {

    /** Errors recorded on the given day or later, oldest first. */
    List<ErrorReportDto> getErrorsSince(LocalDate since);
}
