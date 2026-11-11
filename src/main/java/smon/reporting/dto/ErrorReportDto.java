package smon.reporting.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ErrorReportDto {

    private String errorMessage;
    private String reason;
    private String sourceClass;
    private LocalDate registrationDate;
    private String shopName;

}
