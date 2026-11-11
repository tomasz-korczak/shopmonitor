package smon.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Entity identity is the id (M13): stable when the id is assigned on save, and no lazy association is touched. */
class EntityEqualityTest {

    @Test
    void unsavedShopsAreEqualOnlyToThemselves() {
        Shop first = new Shop();
        Shop second = new Shop();
        first.setName("alfa");
        second.setName("alfa");
        assertThat(first).isEqualTo(first).isNotEqualTo(second);
    }

    @Test
    void savedShopsAreEqualById() {
        Shop first = new Shop();
        Shop second = new Shop();
        first.setId(5L);
        second.setId(5L);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void hashCodeDoesNotChangeWhenTheIdIsAssigned() {
        Shop shop = new Shop();
        int before = shop.hashCode();
        shop.setId(9L);
        assertThat(shop.hashCode()).isEqualTo(before);
    }

    @Test
    void selectorStaysInAHashSetWhenTheShopGetsAnIdAndTheSelectorGetsOne() {
        Shop shop = new Shop();
        Selector selector = new Selector(null, "h3 > a", 1, shop, "s_link");
        Set<Selector> selectors = new LinkedHashSet<>();
        selectors.add(selector);

        shop.setId(3L);
        selector.setId(30L);

        assertThat(selectors).contains(selector);
    }

    @Test
    void selectorEqualityDoesNotLookAtTheShop() {
        Selector first = new Selector(7L, "h3", 1, null, "s_desc");
        Selector second = new Selector(7L, "other", 2, new Shop(), "s_link");
        assertThat(first).isEqualTo(second);
        assertThat(new HashSet<>(Collections.singleton(first))).contains(second);
    }

    @Test
    void shopToStringDoesNotPrintSelectors() {
        Shop shop = new Shop();
        shop.setName("alfa");
        shop.setListSelectors(new HashSet<>(Collections.singleton(new Selector(null, "unique-selector-text", 1, shop, "s_list"))));

        assertThat(shop.toString()).contains("alfa").doesNotContain("unique-selector-text");
    }
}
