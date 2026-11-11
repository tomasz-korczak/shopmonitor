package smon.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

public class URLUtil {

    /** Replaced with the whole search text, spaces as %20. */
    private final static String SEARCH_STRING_SUBSTITUTE = "{search}";
    /** Replaced with the search words joined with '+'. */
    private final static String SEARCH_STRING_SUBSTITUTE_PLUS = "{search+}";
    /** Replaced with the search words joined with ','. */
    private final static String SEARCH_STRING_SUBSTITUTE_COMMA = "{search,}";

    /**
     * Fills the search text into a shop's searchUrl. The text is URL-encoded as UTF-8,
     * so '&amp;', '#', '$', '\' and Polish letters are safe.
     */
    public static String prepareSearchUrl(String url, String searchString) {
        String[] words = searchString.trim().split("\\s+");
        return url
                .replace(SEARCH_STRING_SUBSTITUTE, encode(searchString.trim()))
                .replace(SEARCH_STRING_SUBSTITUTE_PLUS, join(words, "+"))
                .replace(SEARCH_STRING_SUBSTITUTE_COMMA, join(words, ","));
    }

    private static String join(String[] words, String separator) {
        return Arrays.stream(words).map(URLUtil::encode).collect(Collectors.joining(separator));
    }

    /** Encodes for both the path and the query: a space becomes %20, not '+'. */
    private static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e); // UTF-8 is always supported
        }
    }

    public static String getBaseUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String port = url.getPort() == -1 ? "" : ":" + url.getPort();
            return url.getProtocol()+"://"+url.getHost()+port;
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
