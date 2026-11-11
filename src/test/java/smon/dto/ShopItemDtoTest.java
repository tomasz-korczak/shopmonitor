package smon.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShopItemDtoTest {

    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private static ShopItemDto item(String description, String link, BigDecimal price) {
        ShopItemDto item = new ShopItemDto();
        item.setDescription(description);
        item.setLink(link);
        item.setPrice(price);
        return item;
    }

    @Test
    void completeNeedsDescriptionAndLink() {
        assertThat(item("Mauser K98", "https://shop.pl/k98", PRICE).isComplete()).isTrue();
        assertThat(item("Mauser K98", "https://shop.pl/k98", null).isComplete()).as("price is optional").isTrue();
        assertThat(item(null, "https://shop.pl/k98", PRICE).isComplete()).isFalse();
        assertThat(item("  ", "https://shop.pl/k98", PRICE).isComplete()).isFalse();
        assertThat(item("Mauser K98", null, PRICE).isComplete()).isFalse();
        assertThat(item("Mauser K98", "", PRICE).isComplete()).isFalse();
    }

    @Test
    void emptyMeansNeitherDescriptionNorPrice() {
        assertThat(item(null, "https://shop.pl/banner", null).isEmpty()).isTrue();
        assertThat(item("x", null, null).isEmpty()).isTrue();
        assertThat(item(null, null, PRICE).isEmpty()).isFalse();
        assertThat(item("Mauser K98", null, null).isEmpty()).isFalse();
    }
}
