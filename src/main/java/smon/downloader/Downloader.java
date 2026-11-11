package smon.downloader;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import smon.exception.DownloadException;
import smon.model.Shop;
import smon.util.SSLUtils;
import smon.util.URLUtil;

import java.io.IOException;

/**
 * Downloads shop pages with jsoup: a browser User-Agent, an explicit timeout and body size cap,
 * one retry for transient failures, and SSL verification disabled (see {@link SSLUtils}).
 */
@Slf4j
public class Downloader {

    /**
     * Connect and read timeout. jsoup's only public option is one value for both (checked in the 1.17.2 jar), so this
     * is 20 s: a dead shop costs one timeout plus one retry, not a whole scheduled run (M6).
     */
    static final int TIMEOUT_MS = 20_000;
    /**
     * jsoup cuts bodies at 2 MB by default, silently, which would drop the end of long listing pages.
     * The saved Wix page in the tests is 1.7 MB.
     */
    static final int MAX_BODY_BYTES = 20 * 1024 * 1024;
    static final long RETRY_DELAY_MS = 3_000;

    private Shop shop;
    long retryDelayMs = RETRY_DELAY_MS;

    public Downloader(Shop shop) {
        this.shop = shop;
    }

    public Downloader() {
    }

    public Document getDocument() throws DownloadException {
        String url = shop.getUrl();
        return this.getDocument(url);
    }

    public Document getSearchDocument(String searchString) throws DownloadException {
        String searchUrl = URLUtil.prepareSearchUrl(shop.getSearchUrl(), searchString);
        return getDocument(searchUrl);
    }

    /** Downloads and parses the page; a transient failure (timeout, connection reset, HTTP 5xx/429) is retried once. */
    public Document getDocument(String url) throws DownloadException {
        try {
            return fetch(url);
        } catch (IOException first) {
            if (!isRetryable(first)) {
                throw new DownloadException(url, first);
            }
            log.info("Downloading {} failed ({}), retrying once", url, first.toString());
            pause();
            try {
                return fetch(url);
            } catch (IOException second) {
                throw new DownloadException(url, second);
            }
        }
    }

    private Document fetch(String url) throws IOException {
        Connection.Response response = makeConnection(url).execute();
        if (response.bodyAsBytes().length >= MAX_BODY_BYTES) {
            log.warn("Page {} reached the {} MB size limit and was cut off", url, MAX_BODY_BYTES / 1024 / 1024);
        }
        return response.parse();
    }

    /** Worth a second try: network problems and server-side errors, not 4xx or a non-HTML response. */
    static boolean isRetryable(IOException e) {
        if (e instanceof HttpStatusException) {
            int status = ((HttpStatusException) e).getStatusCode();
            return status >= 500 || status == 429;
        }
        return !(e instanceof UnsupportedMimeTypeException);
    }

    private void pause() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Connection makeConnection(String url) {
        Connection connection = Jsoup.connect(url)
                .sslSocketFactory(SSLUtils.getInsecureSSLSocketFactory())
                .timeout(TIMEOUT_MS)
                .maxBodySize(MAX_BODY_BYTES);
        connection.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0");
        connection.header("accept-language","pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7");
        connection.header("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        connection.header("priority","u=0, i");
        return connection;
    }

}
