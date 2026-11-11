package gdziestrzelac;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class SslUtil {

    private SslUtil() {
    }

    public static void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(
                    sslContext.getSocketFactory()
            );

            HttpsURLConnection.setDefaultHostnameVerifier(
                    (hostname, session) -> true
            );

        } catch (Exception e) {
            throw new RuntimeException("Nie można zainicjalizować SSL", e);
        }
    }
}
