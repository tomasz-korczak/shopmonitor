package smon.discover;

import lombok.extern.slf4j.Slf4j;
import smon.dto.ShopDefinition;
import smon.extractor.DiscoverExtractor;
import smon.model.ElementType;
import smon.util.SelectorCheck;
import smon.util.URLUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Matches the selectors stored for all shops against a page. Pure logic: it reads the page and the given selectors,
 * and neither prints nor saves anything.
 */
@Slf4j
public final class DiscoverService {

    public static final String LIST = "s_list";
    public static final String DESCRIPTION = "s_desc";
    public static final String IMAGE = "s_image";
    public static final String LINK = "s_link";
    public static final String PRICE = "s_price";
    public static final List<String> ALL_TYPES = Collections.unmodifiableList(
            Arrays.asList(LIST, DESCRIPTION, IMAGE, LINK, PRICE));

    /**
     * @param page   the downloaded page
     * @param stored the stored selectors per type (from the database); malformed ones are skipped and logged once
     */
    public DiscoverResult discover(DiscoverExtractor page, Map<String, List<String>> stored) {
        Map<String, List<String>> usable = withoutMalformed(stored);
        Map<String, List<String>> found = new HashMap<>();
        found.put(LIST, matchingListSelectors(usable.get(LIST), page));
        found.put(DESCRIPTION, matchingItemSelectors(usable.get(DESCRIPTION), found.get(LIST), page, ElementType.CONTENT));
        found.put(IMAGE, matchingItemSelectors(usable.get(IMAGE), found.get(LIST), page, ElementType.IMAGE));
        found.put(LINK, matchingItemSelectors(usable.get(LINK), found.get(LIST), page, ElementType.DESTINATION));
        found.put(PRICE, matchingItemSelectors(usable.get(PRICE), found.get(LIST), page, ElementType.PRICE));
        removeEmpty(found);
        sortLongestFirst(found);
        return new DiscoverResult(found, lacking(found));
    }

    /** The best (longest) discovered selector of each type, as priority 1. ShopsService.addShop validates it. */
    public ShopDefinition toDefinition(String name, String url, DiscoverResult result) {
        ShopDefinition definition = new ShopDefinition(name.trim(), url);
        definition.setBaseUrl(URLUtil.getBaseUrl(url));
        definition.setListSelectors(best(result, LIST));
        definition.setDescriptionSelectors(best(result, DESCRIPTION));
        definition.setLinkSelectors(best(result, LINK));
        definition.setImageSelectors(best(result, IMAGE));
        definition.setPriceSelectors(best(result, PRICE));
        return definition;
    }

    private static Map<String, List<String>> withoutMalformed(Map<String, List<String>> stored) {
        Map<String, List<String>> usable = new HashMap<>();
        for (String type : ALL_TYPES) {
            List<String> selectors = stored.get(type) == null ? Collections.<String>emptyList() : stored.get(type);
            List<String> kept = new ArrayList<>();
            for (String selector : selectors) {
                String error = SelectorCheck.syntaxError(selector);
                if (error == null) {
                    kept.add(selector);
                } else {
                    log.warn("Skipping stored {} selector \"{}\": {}", type, selector, error);
                }
            }
            usable.put(type, kept);
        }
        return usable;
    }

    private static List<String> matchingListSelectors(List<String> allSelectors, DiscoverExtractor page) {
        return allSelectors.stream()
                .filter(selector -> page.countLandingPageItems(page.getDoc(), selector) > 0)
                .collect(Collectors.toList());
    }

    private static List<String> matchingItemSelectors(List<String> allItemSelectors, List<String> matchingListSelectors,
                                                      DiscoverExtractor page, ElementType type) {
        Map<String, Long> result = new HashMap<>();
        for (String listSelector : matchingListSelectors) {
            for (String itemSelector : allItemSelectors) {
                Long count = page.countElementsByType(page.getDoc(), listSelector, itemSelector, type);
                if (count > 0) {
                    result.put(itemSelector, count);
                }
            }
        }
        return new ArrayList<>(result.keySet());
    }

    private static void removeEmpty(Map<String, List<String>> selectors) {
        selectors.values().removeIf(List::isEmpty);
    }

    private static void sortLongestFirst(Map<String, List<String>> selectors) {
        for (List<String> list : selectors.values()) {
            list.sort(Comparator.comparingInt(String::length));
            Collections.reverse(list);
        }
    }

    private static List<String> lacking(Map<String, List<String>> found) {
        List<String> types = new ArrayList<>(ALL_TYPES);
        types.removeAll(found.keySet());
        return types;
    }

    private static List<String> best(DiscoverResult result, String type) {
        List<String> found = result.getSelectors().get(type);
        return found == null || found.isEmpty() ? new ArrayList<>() : new ArrayList<>(found.subList(0, 1));
    }
}
