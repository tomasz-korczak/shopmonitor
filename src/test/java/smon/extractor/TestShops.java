package smon.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import smon.dto.ShopItemDto;
import smon.exception.DownloadException;
import smon.message.dto.MessageDto;
import smon.model.Selector;
import smon.model.Shop;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Builds shops and runs the extractor on a given document, without a database or network. */
final class TestShops {

    private TestShops() {
    }

    static Shop shop(String url, String list, String description, String image, String link, String price) {
        Shop shop = new Shop();
        shop.setName("test-shop");
        shop.setUrl(url);
        shop.setListSelectors(selectors(list));
        shop.setDescriptionSelectors(selectors(description));
        shop.setImageSelectors(selectors(image));
        shop.setLinkSelectors(selectors(link));
        shop.setPriceSelectors(selectors(price));
        return shop;
    }

    /** Selectors in priority order, like Hibernate loads them (@OrderBy priority). */
    private static Set<Selector> selectors(String... selectors) {
        Set<Selector> result = new LinkedHashSet<>();
        int priority = 1;
        for (String selector : selectors) {
            if (selector != null) {
                result.add(new Selector(null, selector, priority++, null, null));
            }
        }
        return result;
    }

    static Shop withSelectors(Shop shop, String type, String... selectors) {
        Set<Selector> set = selectors(selectors);
        switch (type) {
            case "image": shop.setImageSelectors(set); break;
            case "price": shop.setPriceSelectors(set); break;
            case "description": shop.setDescriptionSelectors(set); break;
            default: throw new IllegalArgumentException(type);
        }
        return shop;
    }

    /** Runs the extractor on an HTML snippet, as if it was downloaded from the shop's URL. */
    static List<ShopItemDto> extract(Shop shop, String html) {
        return extract(shop, Jsoup.parse(html, shop.getUrl()));
    }

    /** Runs the extractor on a saved page from src/test/resources/pages. */
    static List<ShopItemDto> extractPage(Shop shop, String page) {
        try {
            File file = new File(TestShops.class.getResource("/pages/" + page).toURI());
            return extract(shop, Jsoup.parse(file, "UTF-8", shop.getUrl()));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<ShopItemDto> extract(Shop shop, Document document) {
        return result(shop, document, text -> Optional.empty()).getItems().orElse(Collections.<ShopItemDto>emptyList());
    }

    /** The whole result, with the given lookup for the "shop closed" phrases. */
    static ExtractionResult result(Shop shop, Document document, Function<String, Optional<MessageDto>> closedShopMessages) {
        return new BaseExtractor(shop, closedShopMessages) {
            @Override
            protected Document downloadDocument() throws DownloadException {
                return document;
            }
        }.extractShopItems();
    }

    static ShopItemDto item(String link) {
        ShopItemDto item = new ShopItemDto();
        item.setDescription("item " + link);
        item.setLink(link);
        return item;
    }

    static List<ShopItemDto> items(String... links) {
        return Arrays.stream(links).map(TestShops::item).collect(java.util.stream.Collectors.toList());
    }
}
