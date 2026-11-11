package smon.scan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smon.dto.ShopItemDto;
import smon.errors.service.ErrorsService;
import smon.extractor.ExtractionProblem;
import smon.extractor.ExtractionResult;
import smon.items.service.ItemsService;
import smon.mail.MailKind;
import smon.mail.MailService;
import smon.model.Item;
import smon.model.Shop;
import smon.shops.service.ShopsService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The scan rules: what is saved, mailed and reported in each mode. Plain Mockito fakes, no Spring. */
class ScanServiceTest {

    private ItemsService items;
    private ErrorsService errors;
    private MailService mail;
    private ShopsService shops;
    private ScanServiceImpl service;

    @BeforeEach
    void setUp() {
        items = mock(ItemsService.class);
        errors = mock(ErrorsService.class);
        mail = mock(MailService.class);
        shops = mock(ShopsService.class);
        service = new ScanServiceImpl(items, errors, mail, shops);
    }

    private static Shop shop(long id, String name) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(name);
        shop.setUrl("https://" + name + ".pl/new");
        shop.setSearchUrl("https://" + name + ".pl/search?q={search}");
        return shop;
    }

    private static ShopItemDto item(String link, String price) {
        ShopItemDto dto = new ShopItemDto();
        dto.setDescription("item " + link);
        dto.setLink(link);
        dto.setPrice(price == null ? null : new BigDecimal(price));
        return dto;
    }

    private static ExtractionResult found(ShopItemDto... items) {
        return ExtractionResult.parsed(Arrays.asList(items));
    }

    /** Each shop gets the given result; a shop not in the map gets an empty page. */
    private static Function<Shop, ExtractionResult> extractor(Map<Shop, ExtractionResult> results) {
        return shop -> results.containsKey(shop) ? results.get(shop) : found();
    }

    private static ScanRequest request(ScanMode mode, Function<Shop, ExtractionResult> extractor) {
        return new ScanRequest(null, mode, extractor, shop -> true,
                mode == ScanMode.SEARCH || mode == ScanMode.SEARCH_PRINT ? MailKind.SEARCH : MailKind.SCAN,
                mode == ScanMode.SEARCH || mode == ScanMode.SEARCH_PRINT ? new String[]{"mauser"} : new String[0]);
    }

    private static Item pending(long id, Shop shop) {
        Item item = new Item();
        item.setId(id);
        item.setShop(shop);
        item.setLink("https://alfa.pl/" + id);
        item.setPrice(new BigDecimal("100.00"));
        item.setShouldNotify(true);
        return item;
    }

    @Test
    void saveAndMailSavesNewItemsAndClearsOnlyTheMailedIds() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        when(items.getItemsToNotify(null)).thenReturn(Collections.singletonList(pending(7L, alfa)));
        when(mail.sendEmail(eq(MailKind.SCAN), anyMap())).thenReturn(true);
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));

        service.scan(request(ScanMode.SAVE_AND_MAIL, extractor(pages)));

        verify(items).saveItems(argThat(list -> list.size() == 1), eq(alfa), eq(true));
        verify(items).markNotified(Collections.singletonList(7L));
    }

    @Test
    void saveAndMailKeepsPendingItemsWhenNobodyReceivedTheMail() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        when(items.getItemsToNotify(null)).thenReturn(Collections.singletonList(pending(7L, alfa)));
        when(mail.sendEmail(eq(MailKind.SCAN), anyMap())).thenReturn(false);

        assertThatThrownBy(() -> service.scan(request(ScanMode.SAVE_AND_MAIL, extractor(Collections.<Shop, ExtractionResult>emptyMap()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be sent");
        verify(items, never()).markNotified(anyCollection());
    }

    @Test
    void readOnlyMailsWhatWasFoundAndSavesNothing() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        when(mail.sendEmail(eq(MailKind.SCAN), anyMap())).thenReturn(true);
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));

        ScanOutcome outcome = service.scan(request(ScanMode.READ_ONLY_MAIL, extractor(pages)));

        verify(items, never()).saveItems(anyList(), any(), anyBoolean());
        verify(mail).sendEmail(eq(MailKind.SCAN), anyMap());
        assertThat(outcome.getFound()).containsOnlyKeys(alfa);
    }

    @Test
    void saveNoMailSavesWithNotifyFalseAndSendsNothing() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));

        service.scan(request(ScanMode.SAVE_NO_MAIL, extractor(pages)));

        verify(items).saveItems(argThat(list -> list.size() == 1), eq(alfa), eq(false));
        verify(mail, never()).sendEmail(any(), anyMap());
    }

    @Test
    void dryRunSavesAndMailsNothingButReturnsTheItems() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));

        ScanOutcome outcome = service.scan(request(ScanMode.DRY_RUN, extractor(pages)));

        verify(items, never()).saveItems(anyList(), any(), anyBoolean());
        verify(mail, never()).sendEmail(any(), anyMap());
        assertThat(outcome.getFound().get(alfa)).hasSize(1);
    }

    @Test
    void searchMailsItemsAlreadyStoredAndSavesNothing() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        // the item is already in ITEMS: a scan would skip it, a search must still report it (H1)
        when(items.knownKeys(anyCollection())).thenReturn(Collections.singleton("https://alfa.pl/1|100.00"));
        when(mail.sendEmail(eq(MailKind.SEARCH), anyMap(), eq("mauser"))).thenReturn(true);
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));

        service.scan(request(ScanMode.SEARCH, extractor(pages)));

        verify(items, never()).knownKeys(anyCollection());
        verify(items, never()).saveItems(anyList(), any(), anyBoolean());
        verify(mail).sendEmail(eq(MailKind.SEARCH), anyMap(), eq("mauser"));
    }

    @Test
    void scanSkipsItemsAlreadyStored() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        when(items.knownKeys(anyCollection())).thenReturn(Collections.singleton("https://alfa.pl/1|100.00"));
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(
                item("https://alfa.pl/1", "100"),      // known: same link and price
                item("https://alfa.pl/1", "120.00"),   // same link, new price: a new item (production prices have scale 2)
                item("https://alfa.pl/2", null)));     // new, without a price

        ScanOutcome outcome = service.scan(request(ScanMode.DRY_RUN, extractor(pages)));

        assertThat(outcome.getFound().get(alfa)).extracting(ShopItemDto::getPrice)
                .containsExactly(new BigDecimal("120.00"), null);
    }

    @Test
    void aFailingShopIsRecordedAndTheOtherShopsStillRun() {
        Shop alfa = shop(1, "alfa");
        Shop bravo = shop(2, "bravo");
        when(shops.listShops()).thenReturn(Arrays.asList(alfa, bravo));
        Function<Shop, ExtractionResult> failsForAlfa = shop -> {
            if (shop == alfa) {
                throw new IllegalStateException("bad page");
            }
            return found(item("https://bravo.pl/1", "50"));
        };

        service.scan(request(ScanMode.SAVE_NO_MAIL, failsForAlfa));

        verify(errors).reportError(eq("Exception during processing alfa"), eq(alfa), any(Throwable.class));
        verify(items).saveItems(argThat(list -> list.size() == 1), eq(bravo), eq(false));
    }

    @Test
    void problemsOfAShopAreRecordedWithTheirReason() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        Function<Shop, ExtractionResult> noList = shop -> ExtractionResult.failed(Collections.singletonList(
                new ExtractionProblem("Exception during downloading alfa", "no items identified", null)));

        service.scan(request(ScanMode.SAVE_AND_MAIL, noList));

        verify(errors).reportError("Exception during downloading alfa", alfa, "no items identified");
        verify(items, never()).saveItems(anyList(), any(), anyBoolean());
    }

    @Test
    void itemWithoutLinkIsDroppedAndRecorded() {
        Shop alfa = shop(1, "alfa");
        when(shops.listShops()).thenReturn(Collections.singletonList(alfa));
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(
                item("https://alfa.pl/1", "100"),
                item(null, "200")));

        service.scan(request(ScanMode.SAVE_NO_MAIL, extractor(pages)));

        verify(errors).reportError(eq("Incomplete items in alfa"), eq(alfa), anyString());
        verify(items).saveItems(argThat(list -> list.size() == 1), eq(alfa), eq(false));
    }

    @Test
    void searchSkipsShopsThatAreNotApplicable() {
        Shop alfa = shop(1, "alfa");
        Shop bravo = shop(2, "bravo");
        bravo.setSearchUrl(null);
        when(shops.listShops()).thenReturn(Arrays.asList(alfa, bravo));
        Predicate<Shop> hasSearchUrl = shop -> shop.getSearchUrl() != null;
        Map<Shop, ExtractionResult> pages = Collections.singletonMap(alfa, found(item("https://alfa.pl/1", "100")));
        ScanRequest request = new ScanRequest(null, ScanMode.SEARCH_PRINT, extractor(pages), hasSearchUrl,
                MailKind.SEARCH, "mauser");

        ScanOutcome outcome = service.scan(request);

        assertThat(outcome.getFound()).containsOnlyKeys(alfa);
    }

    @Test
    void unknownShopNameIsAnError() {
        when(shops.getShopDetails("nope")).thenReturn(null);
        assertThatThrownBy(() -> service.scan(new ScanRequest("nope", ScanMode.DRY_RUN, extractor(Collections.<Shop, ExtractionResult>emptyMap()),
                shop -> true, MailKind.SCAN)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found or disabled");
    }
}
