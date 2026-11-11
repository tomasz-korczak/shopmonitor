package smon.exception;

import lombok.Getter;


@Getter
public class DownloadException extends Exception{

    private final String url;
    private final Throwable originalException;
    public DownloadException(String url, Throwable originalException) {
        super(originalException == null ? null : originalException.getMessage(), originalException);
        this.url = url;
        this.originalException = originalException;
    }

}
