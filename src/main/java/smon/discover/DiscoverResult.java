package smon.discover;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The selectors that matched a page, per selector type, best (longest) first, and the types that matched nothing. */
public final class DiscoverResult {

    private final Map<String, List<String>> selectors;
    private final List<String> lacking;

    public DiscoverResult(Map<String, List<String>> selectors, List<String> lacking) {
        this.selectors = Collections.unmodifiableMap(selectors);
        this.lacking = Collections.unmodifiableList(lacking);
    }

    public Map<String, List<String>> getSelectors() {
        return selectors;
    }

    public List<String> getLacking() {
        return lacking;
    }
}
