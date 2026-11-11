package smon.errors.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import smon.errors.dao.ErrorsRepository;
import smon.exception.DownloadException;
import smon.model.ErrorRecord;
import smon.model.Shop;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ErrorsServiceImplTest {

    private ErrorsRepository repository;
    private ErrorsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ErrorsRepository.class);
        service = new ErrorsServiceImpl();
        ReflectionTestUtils.setField(service, "errorsRepository", repository);
    }

    private ErrorRecord saved() {
        ArgumentCaptor<ErrorRecord> captor = ArgumentCaptor.forClass(ErrorRecord.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static String text(char c, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static Shop shop(String url) {
        Shop shop = new Shop();
        shop.setName("alfa");
        shop.setUrl(url);
        return shop;
    }

    @Test
    void longMessageIsCutToTheColumnSize() {
        service.reportError(text('m', 600), null, "reason");

        ErrorRecord error = saved();
        assertThat(error.getErrorMessage()).hasSize(500).endsWith("...");
    }

    @Test
    void shortReasonIsSavedUnchangedAndNullStaysNull() {
        service.reportError("message", null, (String) null);

        ErrorRecord error = saved();
        assertThat(error.getReason()).isNull();
        assertThat(error.getErrorMessage()).isEqualTo("message");
    }

    @Test
    void longReasonIsCutButKeepsTheShopUrl() {
        service.reportError("message", shop("https://alfa.pl/"), text('r', 600));

        String reason = saved().getReason();
        assertThat(reason.length()).isLessThanOrEqualTo(500);
        assertThat(reason).endsWith("..., URL: https://alfa.pl/");
    }

    @Test
    void downloadExceptionMessageIsCutAndClassPrefixIsKept() {
        DownloadException exception = new DownloadException("https://alfa.pl/", new IOException(text('x', 600)));

        service.reportError("message", null, exception);

        ErrorRecord error = saved();
        assertThat(error.getReason()).hasSize(500).endsWith("...");
        assertThat(error.getSourceClass()).isEqualTo(DownloadException.class.toString());
    }
}
