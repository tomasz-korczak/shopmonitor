package smon.items.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import smon.dto.ShopItemDto;
import smon.items.dao.ItemsRepository;
import smon.message.dto.MessageDto;
import smon.message.service.MessageService;
import smon.model.Item;
import smon.model.Shop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemsServiceImplTest {

    private ItemsRepository repository;
    private ItemsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ItemsRepository.class);
        service = new ItemsServiceImpl();
        ReflectionTestUtils.setField(service, "itemsRepository", repository);
        ReflectionTestUtils.setField(service, "messageService", mock(MessageService.class));
    }

    @Test
    void knownKeysAreLookedUpInChunksOf500() {
        List<String> links = IntStream.range(0, 1200).mapToObj(i -> "https://x.pl/" + i).collect(Collectors.toList());
        List<Integer> sizes = new ArrayList<>();
        when(repository.findByLinkIn(anyCollection())).thenAnswer(invocation -> {
            sizes.add(((Collection<?>) invocation.getArgument(0)).size());
            return Collections.emptyList();
        });

        service.knownKeys(links);

        assertThat(sizes).containsExactly(500, 500, 200);
    }

    @Test
    void knownKeyIsLinkAndPriceWithTwoDecimals() {
        Item stored = new Item();
        stored.setLink("https://x.pl/1");
        stored.setPrice(new BigDecimal("1299"));
        when(repository.findByLinkIn(anyCollection())).thenReturn(Collections.singletonList(stored));

        assertThat(service.knownKeys(Collections.singletonList("https://x.pl/1"))).containsExactly("https://x.pl/1|1299.00");
    }

    @Test
    void knownKeyOfAStoredItemWithoutPriceIsNull() {
        Item stored = new Item();
        stored.setLink("https://x.pl/1");
        when(repository.findByLinkIn(anyCollection())).thenReturn(Collections.singletonList(stored));

        assertThat(service.knownKeys(Collections.singletonList("https://x.pl/1"))).containsExactly("https://x.pl/1|null");
    }

    @Test
    void closedShopPhraseIsFoundInThePage() {
        ReflectionTestUtils.setField(service, "messageService", messagesWith(
                new MessageDto(null, "no phrase"),
                new MessageDto("  ", "blank phrase"),
                new MessageDto("przerwa techniczna", "shop closed")));

        Optional<MessageDto> found = service.noItemsStatusMessage("<p>Trwa przerwa techniczna</p>");

        assertThat(found).isPresent();
        assertThat(found.get().getReason()).isEqualTo("shop closed");
    }

    @Test
    void nullAndBlankPhrasesNeverMatch() {
        ReflectionTestUtils.setField(service, "messageService", messagesWith(
                new MessageDto(null, "no phrase"),
                new MessageDto("", "empty phrase")));

        assertThat(service.noItemsStatusMessage("<p>Any page at all</p>")).isNotPresent();
    }

    private static MessageService messagesWith(MessageDto... messages) {
        MessageService messageService = mock(MessageService.class);
        when(messageService.getAllMessages()).thenReturn(Arrays.asList(messages));
        return messageService;
    }

    @Test
    void saveItemsSetsShopAndNotifyFlag() {
        Shop shop = new Shop();
        shop.setName("alfa");
        List<Item> saved = new ArrayList<>();
        doAnswer(invocation -> {
            for (Object entity : (Iterable<?>) invocation.getArgument(0)) {
                saved.add((Item) entity);
            }
            return null;
        }).when(repository).saveAll(anyIterable());
        ShopItemDto dto = new ShopItemDto();
        dto.setDescription("Mauser");
        dto.setLink("https://alfa.pl/1");
        dto.setPrice(new BigDecimal("100.00"));

        service.saveItems(Collections.singletonList(dto), shop, false);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getShop()).isSameAs(shop);
        assertThat(saved.get(0).getShouldNotify()).isFalse();
        assertThat(saved.get(0).getPrice()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void markNotifiedWithNoIdsMakesNoCall() {
        service.markNotified(Collections.<Long>emptyList());
        verify(repository, never()).clearShouldNotify(anyCollection());
    }

    @Test
    void markNotifiedClearsTheGivenIds() {
        service.markNotified(Arrays.asList(1L, 2L));
        verify(repository).clearShouldNotify(Arrays.asList(1L, 2L));
    }

    @Test
    void itemsToNotifyForAllShopsOrForOneShop() {
        Shop shop = new Shop();
        shop.setId(5L);
        service.getItemsToNotify(null);
        service.getItemsToNotify(shop);
        verify(repository).findAllToNotify();
        verify(repository).findToNotifyByShop(5L);
    }
}
