package smon.util;

import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class SSLUtils {

    private static SSLSocketFactory insecureSocketFactory;

    /**
     * A socket factory that accepts any server certificate, used for every shop download (many small shops have
     * broken certificates). Built once and reused. Never use it for anything that sends credentials.
     * <p>
     * jsoup 1.17.2 has no option for the host-name check (it only exposes {@code sslSocketFactory}), so the first call
     * also sets the JVM-wide default hostname verifier to accept any name. Only {@code HttpsURLConnection} uses it;
     * JavaMail's SMTP connection does not, so mail keeps its host-name check (see {@code application.properties}).
     */
    public static synchronized SSLSocketFactory getInsecureSSLSocketFactory() {
        if (insecureSocketFactory == null) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                        public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                insecureSocketFactory = sslContext.getSocketFactory();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Cannot create SSL context", e);
            }
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        }
        return insecureSocketFactory;
    }
}
