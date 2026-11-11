package smon.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import smon.model.Selector;
import smon.model.Shop;
import smon.util.SelectorCheck;
import smon.util.URLUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A shop with its selectors, as written by {@code shop export} and read by {@code shop import} / built by
 * {@code shop discover}. Each selector type is a list in priority order (first = priority 1); a single
 * {@code <listSelector>} per type, as in the old test.xml, is still valid.
 */
@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"name", "url", "baseUrl", "searchUrl", "disabled",
        "listSelectors", "descriptionSelectors", "linkSelectors", "imageSelectors", "priceSelectors"})
public class ShopDefinition {

    public static final String LIST = "s_list";
    public static final String DESCRIPTION = "s_desc";
    public static final String LINK = "s_link";
    public static final String IMAGE = "s_image";
    public static final String PRICE = "s_price";

    private String name;
    private String url;
    /** Optional, derived from url when missing. */
    private String baseUrl;
    private String searchUrl;
    private Boolean disabled;

    @XmlElement(name = "listSelector")
    private List<String> listSelectors = new ArrayList<>();
    @XmlElement(name = "descriptionSelector")
    private List<String> descriptionSelectors = new ArrayList<>();
    @XmlElement(name = "linkSelector")
    private List<String> linkSelectors = new ArrayList<>();
    @XmlElement(name = "imageSelector")
    private List<String> imageSelectors = new ArrayList<>();
    @XmlElement(name = "priceSelector")
    private List<String> priceSelectors = new ArrayList<>();

    public ShopDefinition(String name, String url) {
        this.name = name;
        this.url = url;
    }

    /**
     * @return what prevents this shop from working; empty when it can be saved.
     * List, description and link selectors are required: without them no item can be saved (see ShopItemDto.isComplete).
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (isBlank(name)) {
            problems.add("name is missing");
        }
        if (isBlank(url) || URLUtil.getBaseUrl(url) == null) {
            problems.add("url is missing or not a valid URL: " + url);
        }
        if (isBlank(baseUrl) && URLUtil.getBaseUrl(url) == null) {
            problems.add("baseUrl is missing and cannot be derived from url");
        }
        requireSelector(problems, listSelectors, "list");
        requireSelector(problems, descriptionSelectors, "description");
        requireSelector(problems, linkSelectors, "link");
        checkSyntax(problems, listSelectors, "list");
        checkSyntax(problems, descriptionSelectors, "description");
        checkSyntax(problems, linkSelectors, "link");
        checkSyntax(problems, imageSelectors, "image");
        checkSyntax(problems, priceSelectors, "price");
        return problems;
    }

    private static void requireSelector(List<String> problems, List<String> selectors, String type) {
        if (selectors == null || selectors.stream().allMatch(ShopDefinition::isBlank)) {
            problems.add("no " + type + " selector");
        }
    }

    /** A selector jsoup cannot parse would fail the shop on every scan, so it is refused here (M2). */
    private static void checkSyntax(List<String> problems, List<String> selectors, String type) {
        if (selectors == null) {
            return;
        }
        for (String selector : selectors) {
            if (!isBlank(selector)) {
                String error = SelectorCheck.syntaxError(selector);
                if (error != null) {
                    problems.add(type + " selector \"" + selector.trim() + "\": " + error);
                }
            }
        }
    }

    /** From a shop loaded with all its selectors. */
    public static ShopDefinition of(Shop shop) {
        ShopDefinition definition = new ShopDefinition(shop.getName(), shop.getUrl());
        definition.setBaseUrl(shop.getBaseUrl());
        definition.setSearchUrl(shop.getSearchUrl());
        definition.setDisabled(Boolean.TRUE.equals(shop.getDisabled()));
        definition.setListSelectors(texts(shop.getListSelectors()));
        definition.setDescriptionSelectors(texts(shop.getDescriptionSelectors()));
        definition.setLinkSelectors(texts(shop.getLinkSelectors()));
        definition.setImageSelectors(texts(shop.getImageSelectors()));
        definition.setPriceSelectors(texts(shop.getPriceSelectors()));
        return definition;
    }

    /** A new, not yet saved shop; selectors get priorities 1..n in list order and point back to the shop. */
    public Shop toShop() {
        Shop shop = new Shop();
        shop.setName(trim(name));
        shop.setUrl(trim(url));
        shop.setBaseUrl(isBlank(baseUrl) ? URLUtil.getBaseUrl(trim(url)) : trim(baseUrl));
        shop.setSearchUrl(isBlank(searchUrl) ? null : trim(searchUrl));
        shop.setDisabled(Boolean.TRUE.equals(disabled));
        shop.setListSelectors(selectors(shop, LIST, listSelectors));
        shop.setDescriptionSelectors(selectors(shop, DESCRIPTION, descriptionSelectors));
        shop.setLinkSelectors(selectors(shop, LINK, linkSelectors));
        shop.setImageSelectors(selectors(shop, IMAGE, imageSelectors));
        shop.setPriceSelectors(selectors(shop, PRICE, priceSelectors));
        return shop;
    }

    /** All selectors of a shop built by {@link #toShop()}, for saving. */
    public static List<Selector> allSelectors(Shop shop) {
        List<Selector> all = new ArrayList<>();
        all.addAll(shop.getListSelectors());
        all.addAll(shop.getDescriptionSelectors());
        all.addAll(shop.getLinkSelectors());
        all.addAll(shop.getImageSelectors());
        all.addAll(shop.getPriceSelectors());
        return all;
    }

    private static Set<Selector> selectors(Shop shop, String type, List<String> texts) {
        Set<Selector> result = new LinkedHashSet<>();
        if (texts != null) {
            int priority = 1;
            for (String text : texts) {
                if (!isBlank(text)) {
                    result.add(new Selector(null, text.trim(), priority++, shop, type));
                }
            }
        }
        return result;
    }

    private static List<String> texts(Collection<Selector> selectors) {
        return selectors == null ? new ArrayList<>()
                : selectors.stream().map(Selector::getSelector).collect(Collectors.toList());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
