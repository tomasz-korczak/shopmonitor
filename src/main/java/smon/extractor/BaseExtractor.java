package smon.extractor;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import smon.downloader.Downloader;
import smon.dto.ShopItemDto;
import smon.exception.DownloadException;
import smon.message.dto.MessageDto;
import smon.model.ElementType;
import smon.model.Selector;
import smon.model.Shop;
import smon.util.Price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseExtractor {

    /** Lazy-load placeholders: an attribute value containing any of these is not a real image. */
    private static final String[] INVALID_ATTRIBUTE_VALUES = {"data:image", "loader"};
    /**
     * In priority order, the first valid one wins: lazy-load attributes come before src,
     * because on lazy-loading pages src holds a placeholder until JavaScript swaps the real URL in.
     */
    private static final String[] ATTRIBUTE_NAMES_IMAGE = {"ci-src", "data-original", "data-src-original", "data-src", "src"};
    private static final String[] ATTRIBUTE_NAMES_ANCHOR = {"href"};

    private Shop shop;
    private final Downloader downloader;
    /** Looks up the "shop closed / maintenance" phrases in a page text (the MESSAGES table). */
    private final Function<String, Optional<MessageDto>> closedShopMessages;
    /** Selectors whose exception was already logged with a stack trace in this run (M8). */
    private final Set<String> reportedSelectors = new HashSet<>();

    /**
     * @param closedShopMessages finds the "shop closed / maintenance" message in a page text, e.g.
     *                           {@code itemsService::noItemsStatusMessage}
     */
    public BaseExtractor(Shop shop, Function<String, Optional<MessageDto>> closedShopMessages) {
        this.shop = shop;
        this.downloader = new Downloader(shop);
        this.closedShopMessages = closedShopMessages;
    }

    public BaseExtractor() {
        this.downloader = new Downloader();
        this.closedShopMessages = text -> Optional.empty();
    }

    /**
     * Downloads the shop page and extracts its items. Never throws for a problem with the shop: the problem is
     * returned in the result, and the caller decides what to record.
     */
    public ExtractionResult extractShopItems() {
        List<ExtractionProblem> problems = new ArrayList<>();
        try {
            Document doc = downloadDocument();
            Elements elements = getLandingPageItemList(doc);
            if (elements == null) {
                Optional<MessageDto> shopMessage = closedShopMessages.apply(doc.wholeText());
                if (shopMessage.isPresent()) {
                    log.warn("Shop {} has no new inventory to sell, maybe is closed or under maintenance", shop.getName());
                    problems.add(new ExtractionProblem(downloadMessage(), shopMessage.get().getReason(), null));
                } else {
                    log.warn("Shop {} has no items, potential change in DOM tree taken place (check list selector)", shop.getName());
                    problems.add(new ExtractionProblem(downloadMessage(), "no items identified", null));
                }
                return ExtractionResult.failed(problems);
            }
            log.debug("Extracted {} elements, selector: {}", elements.size(), shop.getListSelectors());
            return ExtractionResult.parsed(extractItems(elements));
        } catch (DownloadException e) {
            log.error(downloadMessage(), e.getOriginalException());
            problems.add(new ExtractionProblem(String.format("Exception during downloading %s (%s)", shop.getName(), e.getUrl()), null, e));
        } catch (RuntimeException e) {
            // e.g. a stored list selector jsoup cannot parse (M2): this shop fails, the run goes on
            log.error("Shop {} processing failed: {}", shop.getName(), e.toString());
            log.debug("Shop extraction failed", e);
            problems.add(new ExtractionProblem("Exception during processing " + shop.getName(), null, e));
        }
        return ExtractionResult.failed(problems);
    }

    private String downloadMessage() {
        return String.format("Exception during downloading %s", shop.getName());
    }

    protected abstract Document downloadDocument() throws DownloadException;

    /** The whole downloaded page, at TRACE only (it can be megabytes); serialised only when logged. */
    protected static void logPage(Document doc) {
        if (log.isTraceEnabled()) {
            log.trace("Downloaded {}:\n{}", doc.location(), doc.html());
        }
    }

    private List<ShopItemDto> extractItems(Elements elements) {
        List<ShopItemDto> items = elements.stream().map(this::extractItem).collect(Collectors.toList());
        items.removeIf(ShopItemDto::isEmpty);
        return removeDuplicateLinks(items);
    }

    /**
     * A product can be listed twice on one page (e.g. in a featured box and in the list); keeps the first one.
     * Items without a link are kept here; validation rejects them later.
     */
    static List<ShopItemDto> removeDuplicateLinks(List<ShopItemDto> items) {
        Set<String> links = new HashSet<>();
        List<ShopItemDto> result = new ArrayList<>(items.size());
        for (ShopItemDto item : items) {
            if (item.getLink() == null || links.add(item.getLink())) {
                result.add(item);
            } else {
                log.debug("Duplicate item skipped: {}", item.getLink());
            }
        }
        return result;
    }

    private ShopItemDto extractItem(Element element) {
        ShopItemDto item = new ShopItemDto();
        item.setDescription(this.extractResolve(element, shop.getDescriptionSelectors(), ElementType.CONTENT));
        item.setImage(this.extractResolve(element, shop.getImageSelectors(), ElementType.IMAGE));
        item.setLink(this.extractResolve(element, shop.getLinkSelectors(), ElementType.DESTINATION));
        String price = this.extractResolve(element, shop.getPriceSelectors(), ElementType.PRICE);
        item.setPrice(Price.parse(price));
        if (item.getPrice() == null && log.isDebugEnabled()) { // element.html() serialises the element, only do it when it is logged
            log.debug("No price: value extracted:{}, selectors:{}\nElement:\n{}", price, shop.getPriceSelectors(), element.html());
        }
        return item;
    }

    private String extractResolve(Element element, Set<Selector> selectors, ElementType type) {
        for (Selector selector : selectors) {
            String value = extractResolve(element, selector.getSelector(), type);
            if ((value != null && ElementType.PRICE != type) || (ElementType.PRICE == type && Price.parse(value) != null)) {
                return value;
            }
        }
        return null;
    }

    private String extractResolve(Element element, String selector, ElementType type) {
        String value = this.extractValue(element, selector, type);
        if (value != null && !value.isEmpty()) {
            switch (type) {
                case IMAGE:
                case DESTINATION:
                    // already absolute, see extractAttribute
                    value = ValueResolver.encodePath(value);
                    break;
                case PRICE:
                    value = ValueResolver.resolvePrice(value);
                    if (Price.parse(value) == null) {
                        value = null; // not a price (range, decimals or no number): the next selector is tried
                    }
                    break;
            }
        } else {
            value = null;
        }
        return value;
    }

    protected Elements getLandingPageItemList(Document document) {
        Set<Selector> mainItemSelectors = shop.getListSelectors();
        for (Selector mainItemSelector : mainItemSelectors) {
            if (mainItemSelector.getSelector() == null || mainItemSelector.getSelector().isEmpty()) {
                log.warn("Empty main items extraction selector! (id={})", mainItemSelector.getId());
            } else {
                Elements elements = getLandingPageItemList(document, mainItemSelector.getSelector());
                if (!elements.isEmpty()) {
                    return elements;
                }
            }
        }
        return null;
    }

    protected Elements getLandingPageItemList(Document document, String selector) {
        return document.select(selector);
    }

    public Integer countLandingPageItems(Document document, String selector) {
        Elements elements = document.select(selector);
        return elements.size();
    }

    public Long countElementsByType(Document document, String listSelector, String itemSelector, ElementType type) {
        Elements elements = this.getLandingPageItemList(document, listSelector);
        return elements
                .stream()
                .filter(element -> extractResolve(element, itemSelector, type)!= null)
                .count();
    }

    public String extractValue(Element element, String selector, ElementType type) {
        // called for every selector x field x element: the element HTML only at TRACE, and only built when logged
        if (log.isTraceEnabled()) {
            log.trace("Extracting {} with selector {} from:\n{}", type, selector, element.html());
        }
        String result = null;
        if (selector == null || selector.isEmpty()) {
            log.warn("Empty {} selector!", type);
            return null;
        }
        try {
            Elements values = element.select(selector);
            if (values.isEmpty()) {
                return null;
            }
            Element target = values.get(0);
            result = processValue(target, type);
            log.debug("Extracted value: {}, selector: {}, type: {}", result, selector, type);
        } catch (Exception e) {
            // M8: once per selector and run, with the stack trace; the same selector fails on every element
            if (reportedSelectors.add(selector)) {
                log.warn("Exception extracting value, selector: {}", selector, e);
            } else {
                log.debug("Exception extracting value, selector: {}: {}", selector, e.toString());
            }
        }
        reportEmptyValue(result,type,selector);
        return result;
    }

    private void reportEmptyValue(String value, ElementType type, String selector) {
        if (value == null || value.isEmpty()) {
            // DEBUG, not WARN: every fallback selector that misses ends up here, which would flood the console
            log.debug("Extracted invalid value (null); type: {}, selector:{}", type.toString(), selector);
        }
    }


    private String processValue(Element element, ElementType type) {
        switch (type) {
            case CONTENT:
            case PRICE:
                return extractElementContent(element);
            case IMAGE:
                return extractAttribute(element, ATTRIBUTE_NAMES_IMAGE);
            case DESTINATION:
                return extractAttribute(element, ATTRIBUTE_NAMES_ANCHOR);
            default:
                return null;
        }
    }

    /**
     * @return the first valid attribute (in the given priority order) as an absolute URL, or null.
     * Relative values are resolved against the document's URL (the fetched page, or its &lt;base href&gt;),
     * so "../x", "?page=2" and page-relative paths resolve the way a browser resolves them.
     */
    private String extractAttribute(Element element, String[] attributes) {
        for (String attribute : attributes) {
            String value = element.attr(attribute).trim();
            if (isValidAttributeValue(value)) {
                String absolute = element.absUrl(attribute);
                // also rejects javascript:, mailto:, tel: etc., which absUrl returns as they are
                if (absolute.startsWith("https://") || absolute.startsWith("http://")) {
                    return absolute;
                }
                log.debug("Cannot resolve {}=\"{}\" against \"{}\"", attribute, value, element.baseUri());
            }
        }
        return null;
    }

    private String extractElementContent(Element element) {
        return element.ownText();
    }

    private boolean isValidAttributeValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (String invalidMask : INVALID_ATTRIBUTE_VALUES) {
            if (value.contains(invalidMask)) {
                return false;
            }
        }
        return true;
    }

    protected Downloader getDownloader() {
        return downloader;
    }

}
