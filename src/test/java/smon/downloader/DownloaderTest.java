package smon.downloader;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smon.exception.DownloadException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownloaderTest {

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    /** Status codes to answer with, in order; the last one repeats. */
    private final Deque<Integer> statuses = new ArrayDeque<>();
    private String body = "<html><body><p>ok</p></body></html>";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            int status = statuses.size() > 1 ? statuses.poll() : statuses.peek();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/page";
    }

    private Document download(Integer... statusSequence) throws DownloadException {
        statuses.addAll(Arrays.asList(statusSequence));
        Downloader downloader = new Downloader();
        downloader.retryDelayMs = 0;
        return downloader.getDocument(url());
    }

    @Test
    void downloadsAndParses() throws Exception {
        Document doc = download(200);
        assertThat(doc.select("p").text()).isEqualTo("ok");
        assertThat(doc.location()).isEqualTo(url());
        assertThat(requests).hasValue(1);
    }

    @Test
    void serverErrorIsRetriedOnce() throws Exception {
        assertThat(download(503, 200).select("p").text()).isEqualTo("ok");
        assertThat(requests).hasValue(2);
    }

    @Test
    void secondFailureGivesUp() {
        assertThatThrownBy(() -> download(500, 502, 200)).isInstanceOf(DownloadException.class);
        assertThat(requests).hasValue(2);
    }

    @Test
    void clientErrorIsNotRetried() {
        assertThatThrownBy(() -> download(404)).isInstanceOf(DownloadException.class);
        assertThat(requests).hasValue(1);
    }

    @Test
    void pagesLargerThanJsoupsDefaultLimitAreNotCut() throws Exception {
        StringBuilder html = new StringBuilder("<html><body>");
        while (html.length() < 3 * 1024 * 1024) {
            html.append("<p>filler filler filler filler filler filler</p>");
        }
        body = html.append("<p id='last'>end</p></body></html>").toString();
        assertThat(download(200).select("#last").text()).isEqualTo("end");
    }

    @Test
    void retryable() {
        assertThat(Downloader.isRetryable(new SocketTimeoutException())).isTrue();
        assertThat(Downloader.isRetryable(new HttpStatusException("x", 503, "u"))).isTrue();
        assertThat(Downloader.isRetryable(new HttpStatusException("x", 429, "u"))).isTrue();
        assertThat(Downloader.isRetryable(new HttpStatusException("x", 403, "u"))).isFalse();
        assertThat(Downloader.isRetryable(new UnsupportedMimeTypeException("x", "image/png", "u"))).isFalse();
    }
}
