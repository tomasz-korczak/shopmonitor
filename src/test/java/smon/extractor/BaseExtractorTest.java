package smon.extractor;

import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import smon.dto.ShopItemDto;
import smon.exception.DownloadException;
import smon.message.dto.MessageDto;
import smon.model.Shop;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static smon.extractor.TestShops.extract;
import static smon.extractor.TestShops.items;
import static smon.extractor.TestShops.result;
import static smon.extractor.TestShops.shop;
import static smon.extractor.TestShops.withSelectors;

class BaseExtractorTest {

    private static final String PAGE_URL = "https://shop.pl/kategoria/bron/nowosci.html?page=1";

    private static Shop testShop() {
        return shop(PAGE_URL, "div.product", "h3", "img", "a", "span.price");
    }

    private static String product(String inner) {
        return "<div class='product'><h3>Mauser K98</h3><span class='price'>1 299,00 zł</span>" + inner + "</div>";
    }

    private static ShopItemDto single(Shop shop, String html) {
        List<ShopItemDto> items = extract(shop, "<html><body>" + html + "</body></html>");
        assertThat(items).hasSize(1);
        return items.get(0);
    }

    @Test
    void extractsAllFields() {
        ShopItemDto item = single(testShop(), product("<a href='https://shop.pl/k98'><img src='https://shop.pl/k98.jpg'></a>"));
        assertThat(item.getDescription()).isEqualTo("Mauser K98");
        assertThat(item.getPrice()).isEqualTo(new BigDecimal("1299.00"));
        assertThat(item.getLink()).isEqualTo("https://shop.pl/k98");
        assertThat(item.getImage()).isEqualTo("https://shop.pl/k98.jpg");
    }

    // ---- M1: image placeholders

    @Test
    void dataUriPlaceholderInSrcIsSkippedForLazyAttribute() {
        ShopItemDto item = single(testShop(), product(
                "<a href='/k98'><img src='data:image/gif;base64,R0lGODlhAQABAAAAACw=' data-src='/img/k98.jpg'></a>"));
        assertThat(item.getImage()).isEqualTo("https://shop.pl/img/k98.jpg");
    }

    @Test
    void placeholderAloneGivesNoImage() {
        // used to return the placeholder: the filter compared the wrong way round
        assertThat(single(testShop(), product("<a href='/k98'><img src='/img/loader.gif' data-lazy='/img/k98.jpg'></a>")).getImage())
                .isNull();
        assertThat(single(testShop(), product("<a href='/k98'><img src='data:image/svg+xml,%3Csvg%3E'></a>")).getImage())
                .isNull();
    }

    @Test
    void placeholderInLazyAttributeFallsBackToSrc() {
        // the real URL is in src, the lazy attribute holds the spinner
        ShopItemDto item = single(testShop(), product("<a href='/k98'><img src='/img/k98.jpg' data-src='/img/loader.gif'></a>"));
        assertThat(item.getImage()).isEqualTo("https://shop.pl/img/k98.jpg");
    }

    @Test
    void lazyAttributesHavePriorityOverSrc() {
        ShopItemDto item = single(testShop(), product(
                "<a href='/k98'><img src='/thumb.jpg' data-src='/data-src.jpg' data-original='/data-original.jpg'></a>"));
        assertThat(item.getImage()).isEqualTo("https://shop.pl/data-original.jpg");
    }

    // ---- M2: relative URLs resolve against the page, like in a browser

    @Test
    void resolvesUrlsRelativeToThePage() {
        Shop shop = testShop();
        assertThat(single(shop, product("<a href='/k98'></a>")).getLink()).isEqualTo("https://shop.pl/k98");
        assertThat(single(shop, product("<a href='k98.html'></a>")).getLink()).isEqualTo("https://shop.pl/kategoria/bron/k98.html");
        assertThat(single(shop, product("<a href='../k98.html'></a>")).getLink()).isEqualTo("https://shop.pl/kategoria/k98.html");
        assertThat(single(shop, product("<a href='?page=2'></a>")).getLink()).isEqualTo("https://shop.pl/kategoria/bron/nowosci.html?page=2");
        assertThat(single(shop, product("<a href='//cdn.shop.pl/k98'></a>")).getLink()).isEqualTo("https://cdn.shop.pl/k98");
        assertThat(single(shop, product("<a href='http-guns/k98'></a>")).getLink()).isEqualTo("https://shop.pl/kategoria/bron/http-guns/k98");
        assertThat(single(shop, product("<a href='http://other.pl/k98'></a>")).getLink()).isEqualTo("http://other.pl/k98");
    }

    @Test
    void baseHrefIsRespected() {
        String html = "<html><head><base href='https://static.shop.pl/pl/'></head><body>"
                + product("<a href='k98.html'><img src='img/k98.jpg'></a>") + "</body></html>";
        List<ShopItemDto> items = extract(testShop(), html);
        assertThat(items).extracting(ShopItemDto::getLink, ShopItemDto::getImage)
                .containsExactly(tuple("https://static.shop.pl/pl/k98.html", "https://static.shop.pl/pl/img/k98.jpg"));
    }

    @Test
    void spacesInUrlsAreEncoded() {
        assertThat(single(testShop(), product("<a href='/bron/mauser k98.html'></a>")).getLink())
                .isEqualTo("https://shop.pl/bron/mauser%20k98.html");
    }

    @Test
    void unresolvableLinkGivesNull() {
        assertThat(single(testShop(), product("<a href='javascript:void(0)'></a>")).getLink()).isNull();
    }

    // ---- selectors

    @Test
    void fallsBackToNextSelectorInPriorityOrder() {
        Shop shop = withSelectors(testShop(), "price", "span.special-price", "span.price");
        assertThat(single(shop, product("<a href='/k98'></a>")).getPrice()).isEqualTo(new BigDecimal("1299.00"));
    }

    @Test
    void priceSelectorWithoutNumberFallsBack() {
        Shop shop = withSelectors(testShop(), "price", "span.note", "span.price");
        assertThat(single(shop, product("<span class='note'>Zapytaj o cenę</span><a href='/k98'></a>")).getPrice())
                .isEqualTo(new BigDecimal("1299.00"));
    }

    // ---- M12: a value the column cannot store is not a price (message boards can post anything)

    @Test
    void priceOutOfRangeFallsBackToNextSelector() {
        Shop shop = withSelectors(testShop(), "price", "span.phone", "span.price");
        assertThat(single(shop, product("<span class='phone'>123456789012</span><a href='/k98'></a>")).getPrice())
                .isEqualTo(new BigDecimal("1299.00"));
    }

    @Test
    void priceWithNoValidSelectorIsNull() {
        Shop shop = withSelectors(testShop(), "price", "span.phone");
        assertThat(single(shop, product("<span class='phone'>123456789012</span><a href='/k98'></a>")).getPrice()).isNull();
    }

    @Test
    void elementsWithoutDescriptionAndPriceAreDropped() {
        String html = product("<a href='/k98'></a>") + "<div class='product'><a href='/banner'>Promocja</a></div>";
        assertThat(extract(testShop(), html)).extracting(ShopItemDto::getLink).containsExactly("https://shop.pl/k98");
    }

    // ---- M7: duplicates within a page

    @Test
    void duplicateLinksOnOnePageAreExtractedOnce() {
        String html = product("<a href='/k98'></a>") + product("<a href='/k98'></a>") + product("<a href='/vz24'></a>");
        assertThat(extract(testShop(), html)).extracting(ShopItemDto::getLink)
                .containsExactly("https://shop.pl/k98", "https://shop.pl/vz24");
    }

    @Test
    void removeDuplicateLinksKeepsFirstAndItemsWithoutLink() {
        List<ShopItemDto> items = items("a", "b", "a", null, null, "b", "c");
        assertThat(BaseExtractor.removeDuplicateLinks(items)).extracting(ShopItemDto::getLink)
                .containsExactly("a", "b", null, null, "c");
    }

    // ---- M9/M10: the result says what went wrong; the extractor records nothing itself

    @Test
    void noListMatchGivesNoItemsAndAProblem() {
        Shop shop = shop(PAGE_URL, "div.nothing", "h3", "img", "a", "span.price");
        ExtractionResult result = result(shop, Jsoup.parse(product("<a href='/k98'></a>"), PAGE_URL), text -> Optional.empty());
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getProblems()).extracting(ExtractionProblem::getReason).containsExactly("no items identified");
    }

    @Test
    void closedShopMessageIsTheReason() {
        Shop shop = shop(PAGE_URL, "div.nothing", "h3", "img", "a", "span.price");
        MessageDto closed = new MessageDto("przerwa techniczna", "shop closed");
        ExtractionResult result = result(shop, Jsoup.parse("<p>przerwa techniczna</p>", PAGE_URL),
                text -> text.contains("przerwa") ? Optional.of(closed) : Optional.empty());
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getProblems()).extracting(ExtractionProblem::getReason).containsExactly("shop closed");
    }

    @Test
    void parsedPageWithoutProductsHasEmptyItemsAndNoProblem() {
        ExtractionResult result = result(testShop(), Jsoup.parse("<div class='product'></div>", PAGE_URL), text -> Optional.empty());
        assertThat(result.getItems()).isPresent().hasValueSatisfying(items -> assertThat(items).isEmpty());
        assertThat(result.getProblems()).isEmpty();
    }

    @Test
    void invalidListSelectorIsAProblemNotAnException() {
        Shop shop = shop(PAGE_URL, "div[class", "h3", "img", "a", "span.price");
        ExtractionResult result = result(shop, Jsoup.parse(product("<a href='/k98'></a>"), PAGE_URL), text -> Optional.empty());
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getProblems()).singleElement()
                .satisfies(problem -> assertThat(problem.getCause()).isNotNull());
    }

    @Test
    void downloadFailureIsAProblem() {
        ExtractionResult result = new BaseExtractor(testShop(), text -> Optional.empty()) {
            @Override
            protected Document downloadDocument() throws DownloadException {
                throw new DownloadException(PAGE_URL, new IOException("connection reset"));
            }
        }.extractShopItems();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getProblems()).singleElement().satisfies(problem -> {
            assertThat(problem.getMessage()).startsWith("Exception during downloading test-shop");
            assertThat(problem.getCause()).isInstanceOf(DownloadException.class);
        });
    }
}
