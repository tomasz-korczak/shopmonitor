package smon.reporting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import smon.errors.dao.ErrorsRepository;
import smon.model.ErrorRecord;
import smon.reporting.dto.ErrorReportDto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final ErrorsRepository errorsRepository;

    @Override
    public List<ErrorReportDto> getErrorsSince(LocalDate since) {
        List<ErrorRecord> errors = errorsRepository.findAllByRegistrationDateGreaterThanEqualOrderByIdAsc(since);
        return errors.stream().map(this::map2Dto).collect(Collectors.toList());
    }

    private ErrorReportDto map2Dto(ErrorRecord error) {
        ErrorReportDto errorDto = new ErrorReportDto();
        errorDto.setErrorMessage(error.getErrorMessage());
        errorDto.setReason(error.getReason());
        errorDto.setRegistrationDate(error.getRegistrationDate());
        errorDto.setShopName(error.getShop() != null ? error.getShop().getName() : null);
        errorDto.setSourceClass(error.getSourceClass());
        return errorDto;
    }
}
