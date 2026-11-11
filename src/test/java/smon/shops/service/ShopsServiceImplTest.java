package smon.shops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import smon.dto.ShopDefinition;
import smon.model.Selector;
import smon.selectors.service.SelectorsService;
import smon.shops.dao.ShopsRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopsServiceImplTest {

    private ShopsRepository repository;
    private SelectorsService selectors;
    private ShopsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ShopsRepository.class);
        selectors = mock(SelectorsService.class);
        service = new ShopsServiceImpl();
        ReflectionTestUtils.setField(service, "shopsRepository", repository);
        ReflectionTestUtils.setField(service, "selectorService", selectors);
    }

    private static ShopDefinition valid() {
        ShopDefinition definition = new ShopDefinition("walkiria", "https://walkiria.sklep.pl/shop");
        definition.setListSelectors(Arrays.asList("div.products > div", "ul.list > li"));
        definition.setDescriptionSelectors(Collections.singletonList("h3 > a"));
        definition.setLinkSelectors(Collections.singletonList("h3 > a"));
        return definition;
    }

    @Test
    void invalidDefinitionIsRefusedAndNothingIsSaved() {
        ShopDefinition definition = valid();
        definition.setListSelectors(Collections.singletonList("div[class"));

        assertThatThrownBy(() -> service.addShop(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not valid");
        verify(repository, never()).save(any());
        verify(selectors, never()).saveSelectors(any());
    }

    @Test
    void takenNameIsRefusedAndNothingIsSaved() {
        when(repository.existsByName("walkiria")).thenReturn(true);

        assertThatThrownBy(() -> service.addShop(valid()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(repository, never()).save(any());
    }

    /** Two imports of the same name can both pass existsByName; the unique index refuses the second one. */
    @Test
    void nameTakenByAConcurrentImportIsRefusedWithTheSameMessage() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("unique index SHOPS.NAME"));

        assertThatThrownBy(() -> service.addShop(valid()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(selectors, never()).saveSelectors(any());
    }

    @Test
    void validDefinitionSavesTheShopAndItsSelectorsWithPriorities() {
        service.addShop(valid());

        verify(repository).save(any());
        verify(selectors).saveSelectors(argThat((Collection<Selector> saved) -> priorities(saved, ShopDefinition.LIST)
                .equals(Arrays.asList(1, 2))));
        verify(selectors).saveSelectors(argThat((Collection<Selector> saved) -> priorities(saved, ShopDefinition.LINK)
                .equals(Collections.singletonList(1))));
    }

    private static List<Integer> priorities(Collection<Selector> selectors, String type) {
        return selectors.stream()
                .filter(selector -> type.equals(selector.getSelectorTypeCode()))
                .map(Selector::getPriority)
                .collect(Collectors.toList());
    }
}
