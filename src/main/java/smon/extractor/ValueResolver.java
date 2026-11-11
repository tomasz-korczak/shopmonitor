package smon.extractor;

public class ValueResolver {

    public static String resolvePrice(String price) {
        PriceResolver resolver = new PriceResolver(price);
        return resolver.resolve();
    }

    /** Spaces are not valid in a URL; jsoup's absUrl keeps them as they are. */
    public static String encodePath(String path) {
        return path == null ? null : path.replace(" ", "%20");
    }

}
