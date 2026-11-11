package smon.exception;

import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadExceptionTest {

    @Test
    void keepsTheMessageOfTheCause() {
        DownloadException e = new DownloadException("https://shop.pl/", new SocketTimeoutException("Read timed out"));

        assertThat(e.getMessage()).isEqualTo("Read timed out");
        assertThat(e.getUrl()).isEqualTo("https://shop.pl/");
    }

    @Test
    void nullCauseDoesNotThrow() {
        DownloadException e = new DownloadException("https://shop.pl/", null);

        assertThat(e.getMessage()).isNull();
        assertThat(e.getOriginalException()).isNull();
    }
}
