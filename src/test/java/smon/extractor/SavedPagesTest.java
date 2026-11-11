package smon.extractor;

import org.junit.jupiter.api.Test;
import smon.dto.ShopItemDto;
import smon.model.Shop;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static smon.extractor.TestShops.extractPage;
import static smon.extractor.TestShops.shop;

/**
 * Real pages saved from the shops (src/test/resources/pages), with the selectors stored for that shop in data/smon.
 * They pin down how extraction behaves on real markup. When a shop changes its layout, the live page stops matching
 * the stored selectors, not these tests; to cover the new layout, save the new page next to the old one.
 */
class SavedPagesTest {

    /** Shop 80 "bazooka-demobil" (Wix), page saved 2025-12-27. */
    private static Shop bazookaShop() {
        return shop("https://www.bazooka.sklep.pl/bron-z-demobilu?sort=newest",
                "#TPASection_ke02p96z > div > div > div > div > div > div:nth-child(2) > section > div > ul:nth-child(1) > li",
                "div > div > div > div.Jzh04F > a > div > div > div.EiRAO9 > div:nth-child(1) > p",
                "div > div > a > div > div > div.Wo6AuW.vALCqq > div > wow-image > img",
                "div > div > div > div.Jzh04F > a",
                "div > div > div > div.Jzh04F > a > div > div > div.EiRAO9 > div.ntj2AV > div > div > span.cfpn1d");
    }

    @Test
    void bazooka() {
        List<ShopItemDto> items = extractPage(bazookaShop(), "bazooka-demobil.html");

        // sold-out items ("Wyprzedane") have no price
        assertThat(items).extracting(ShopItemDto::getDescription, ShopItemDto::getPrice, ShopItemDto::getLink).containsExactly(
                tuple("MOSIN wz. 44 kal. 7,62x54R", null, "https://www.bazooka.sklep.pl/product-page/mosin-wz-44-kal-7-62x54r"),
                tuple("FN L1A1 .308WIN", new BigDecimal("9300.00"), "https://www.bazooka.sklep.pl/product-page/fn-l1a1-308win"),
                tuple("Karabin samopowtarzalny AWT-40 kal. 7,62x54R", null, "https://www.bazooka.sklep.pl/product-page/karabin-samoczynny-awt-40-kal-7-62x54r"),
                tuple("Karabin samopowtarzalny M57/STGW57", new BigDecimal("3850.00"), "https://www.bazooka.sklep.pl/product-page/karabin-samopowtarzalny-m57-stgw57"),
                tuple("P-64 Czak", new BigDecimal("1590.00"), "https://www.bazooka.sklep.pl/product-page/p-64-czak"),
                tuple("RAK PM-63", new BigDecimal("1350.00"), "https://www.bazooka.sklep.pl/product-page/rak-63"));
        assertThat(items).allSatisfy(item -> {
            assertThat(item.isComplete()).isTrue();
            assertThat(item.getImage()).startsWith("https://static.wixstatic.com/media/");
        });
    }
}
