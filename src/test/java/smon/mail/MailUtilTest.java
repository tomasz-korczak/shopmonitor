package smon.mail;

import org.junit.jupiter.api.Test;
import smon.dto.ShopItemDto;
import smon.model.Shop;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MailUtilTest {

    private static Map<Shop, List<ShopItemDto>> items() {
        Shop shop = new Shop();
        shop.setName("shop <1>");
        shop.setUrl("https://shop.pl/?a=1&b=2");
        ShopItemDto item = new ShopItemDto();
        item.setDescription("Mauser \"K98\" <b>");
        item.setLink("https://shop.pl/k98?x=1&y='2'");
        item.setPrice(new java.math.BigDecimal("1299.00"));
        return Collections.singletonMap(shop, Collections.singletonList(item));
    }

    @Test
    void footerComesFromProperties() {
        String html = new MailUtil(items(), "report", "someone@example.com", "© 2026 Someone")
                .formatHTMLMessage(MailKind.SCAN);
        assertThat(html).contains("href='mailto:someone@example.com?subject=", "<p class='copy'>© 2026 Someone</p>");
    }

    @Test
    void emptyFooterPropertiesLeaveTheLinesOut() {
        String html = new MailUtil(items(), "report", "", null).formatHTMLMessage(MailKind.SCAN);
        assertThat(html).doesNotContain("mailto:", "class='copy'");
    }

    @Test
    void scrapedValuesAreEscaped() {
        String html = new MailUtil(items(), "report", null, null).formatHTMLMessage(MailKind.SCAN);
        assertThat(html)
                .contains("shop &lt;1&gt;", "https://shop.pl/?a=1&amp;b=2", "Mauser &quot;K98&quot; &lt;b&gt;",
                        "https://shop.pl/k98?x=1&amp;y=&#39;2&#39;")
                .doesNotContain("<b>\"");
    }

    @Test
    void itemImageIsARemoteLink() {
        Shop shop = new Shop();
        shop.setName("shop");
        shop.setUrl("https://shop.pl/");
        ShopItemDto item = new ShopItemDto();
        item.setDescription("K98");
        item.setLink("https://shop.pl/k98");
        item.setImage("https://shop.pl/k98.jpg");
        String html = new MailUtil(Collections.singletonMap(shop, Collections.singletonList(item)), "report", null, null)
                .formatHTMLMessage(MailKind.SCAN);
        assertThat(html).contains("<img src='https://shop.pl/k98.jpg'/>").doesNotContain("cid:");
    }
}
