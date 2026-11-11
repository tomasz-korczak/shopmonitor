package smon.util;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;

import static org.assertj.core.api.Assertions.assertThat;

class SSLUtilsTest {

    @Test
    void downloadsAcceptAnyHostName() {
        SSLUtils.getInsecureSSLSocketFactory();

        assertThat(HttpsURLConnection.getDefaultHostnameVerifier().verify("certificate.for.other.host", null)).isTrue();
    }
}
