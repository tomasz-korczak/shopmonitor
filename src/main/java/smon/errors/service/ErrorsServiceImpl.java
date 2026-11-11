package smon.errors.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smon.errors.dao.ErrorsRepository;
import smon.errors.dto.ErrorDto;
import smon.model.ErrorRecord;
import smon.model.Shop;

import java.time.LocalDate;

@Service
@Slf4j
public class ErrorsServiceImpl implements ErrorsService {

    @Autowired
    private ErrorsRepository errorsRepository;
    @Override
    public void reportError(String message, Shop shop,  Throwable originalException) {
        ErrorRecord error = prepareError(message, shop, originalException);
        errorsRepository.save(error);
    }
    @Override
    public void reportError(String message, Shop shop, String reason) {
        ErrorRecord error = prepareError(message, shop, reason);
        errorsRepository.save(error);
    }

    private ErrorRecord dto2entity(ErrorDto errorDto) {
        ErrorRecord error = new ErrorRecord();
        error.setErrorMessage(fit(errorDto.getMessage()));
        error.setShop(errorDto.getShop());
        error.setReason(fit(errorDto.getReason()));
        error.setSourceClass(fit(errorDto.getSourceClass()));
        error.setRegistrationDate(LocalDate.now());
        return error;
    }

    /** Column size of ERRORS.ERROR_MESSAGE, REASON and SOURCE_CLASS in schema.sql. A longer value fails the insert. */
    static final int MAX_LENGTH = 500;
    private static final String ELLIPSIS = "...";

    /** Cuts a text to the column size, marking the cut with an ellipsis; null and short texts are returned as they are. */
    static String fit(String value) {
        return fit(value, MAX_LENGTH);
    }

    static String fit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        if (max <= ELLIPSIS.length()) {
            return value.substring(0, Math.max(0, max));
        }
        return value.substring(0, max - ELLIPSIS.length()) + ELLIPSIS;
    }

    private ErrorRecord prepareError(String message, Shop shop, Throwable originalException) {
        ErrorDto errorDto = new ErrorDto(originalException != null ? originalException.getClass().toString() : null,
                message,
                originalException != null ? originalException.getMessage() : null,
                shop);
        return dto2entity(errorDto);
    }

    private ErrorRecord prepareError(String message, Shop shop, String reason) {
        String text = reason;
        if (shop != null) {
            // the URL is kept: the reason is cut first, so that the whole text still fits
            String urlPart = ", URL: " + shop.getUrl();
            text = fit(reason, Math.max(0, MAX_LENGTH - urlPart.length())) + urlPart;
        }
        ErrorDto errorDto = new ErrorDto(null, message, text, shop);
        return dto2entity(errorDto);
    }
}
