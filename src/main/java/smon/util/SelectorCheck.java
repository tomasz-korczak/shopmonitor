package smon.util;

import org.jsoup.select.QueryParser;

/**
 * Checks CSS selectors stored with a shop. Uses the same parser as {@code Element.select}, so a selector that passes
 * here is one the scanner can run. It does not check whether a selector matches anything on a page.
 */
public final class SelectorCheck {

    private SelectorCheck() {
    }

    /** @return null when the selector is valid, otherwise the parser's message */
    public static String syntaxError(String selector) {
        try {
            QueryParser.parse(selector.trim());
            return null;
        } catch (RuntimeException e) {
            return e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }
}
