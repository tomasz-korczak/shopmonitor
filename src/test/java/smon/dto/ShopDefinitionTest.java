package smon.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import smon.model.Selector;
import smon.model.Shop;
import smon.util.MarshallerUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ShopDefinitionTest {

    private static ShopDefinition valid() {
        ShopDefinition definition = new ShopDefinition("walkiria-broń", "https://walkiria.sklep.pl/shop");
        definition.setListSelectors(Arrays.asList("div.products > div", "ul.list > li"));
        definition.setDescriptionSelectors(Collections.singletonList("h3 > a"));
        definition.setLinkSelectors(Collections.singletonList("h3 > a"));
        definition.setImageSelectors(Collections.singletonList("img"));
        definition.setPriceSelectors(Collections.singletonList("span.price"));
        return definition;
    }

    @Test
    void validDefinitionHasNoProblems() {
        assertThat(valid().validate()).isEmpty();
    }

    @Test
    void listDescriptionAndLinkSelectorsAreRequired() {
        ShopDefinition definition = new ShopDefinition("x", "https://x.pl/");
        definition.setDescriptionSelectors(Collections.singletonList("  "));
        assertThat(definition.validate())
                .containsExactly("no list selector", "no description selector", "no link selector");
    }

    @Test
    void nameAndValidUrlAreRequired() {
        ShopDefinition definition = valid();
        definition.setName(" ");
        definition.setUrl("walkiria.sklep.pl/shop");
        assertThat(definition.validate()).contains("name is missing")
                .anyMatch(p -> p.startsWith("url is missing or not a valid URL"));
    }

    @Test
    void toShopAssignsPrioritiesTypesAndBaseUrl() {
        Shop shop = valid().toShop();
        assertThat(shop.getBaseUrl()).isEqualTo("https://walkiria.sklep.pl");
        assertThat(shop.getDisabled()).isFalse();
        assertThat(shop.getListSelectors())
                .extracting(Selector::getSelector, Selector::getPriority, Selector::getSelectorTypeCode, Selector::getShop)
                .containsExactly(tuple("div.products > div", 1, "s_list", shop), tuple("ul.list > li", 2, "s_list", shop));
        assertThat(ShopDefinition.allSelectors(shop)).extracting(Selector::getSelectorTypeCode)
                .containsExactly("s_list", "s_list", "s_desc", "s_link", "s_image", "s_price");
    }

    @Test
    void ofShopKeepsSelectorOrder() {
        Shop shop = valid().toShop();
        shop.setDisabled(true);
        ShopDefinition definition = ShopDefinition.of(shop);
        assertThat(definition.getListSelectors()).containsExactly("div.products > div", "ul.list > li");
        assertThat(definition.getDisabled()).isTrue();
        assertThat(definition.getBaseUrl()).isEqualTo("https://walkiria.sklep.pl");
    }

    @Test
    void xmlRoundTrip(@TempDir Path dir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MarshallerUtil.marshall(new ShopDefinitionList(Collections.singletonList(valid())), out);
        String xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertThat(xml).contains("<name>walkiria-broń</name>", "<listSelector>div.products &gt; div</listSelector>");

        File file = dir.resolve("shops.xml").toFile();
        Files.write(file.toPath(), out.toByteArray());
        List<ShopDefinition> read = MarshallerUtil.unmarshall(file).getShops();
        assertThat(read).hasSize(1);
        assertThat(read.get(0)).isEqualTo(valid());
    }

    @Test
    void malformedSelectorAmongGoodOnesIsRejected() {
        ShopDefinition definition = valid();
        definition.setListSelectors(Arrays.asList("div.products > div", "div[class"));
        assertThat(definition.validate()).singleElement().asString().startsWith("list selector \"div[class\":");
    }

    @Test
    void malformedPriceAndImageSelectorsAreRejected() {
        ShopDefinition definition = valid();
        definition.setImageSelectors(Collections.singletonList("a::b("));
        definition.setPriceSelectors(Collections.singletonList("span.price["));
        assertThat(definition.validate()).hasSize(2)
                .anyMatch(p -> p.startsWith("image selector \"a::b(\""))
                .anyMatch(p -> p.startsWith("price selector \"span.price[\""));
    }

    @Test
    void readsTheOldSingleSelectorFormat(@TempDir Path dir) throws Exception {
        // the format of test.xml in the repo root
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<shops><shop>"
                + "<name>polarms</name><baseUrl>https://polarms.pl/</baseUrl><url>https://polarms.pl/pl/new</url>"
                + "<listSelector>#box_mainproducts &gt; div.innerbox &gt; div &gt; div</listSelector>"
                + "<descriptionSelector>div &gt; a.prodname.f-row &gt; span</descriptionSelector>"
                + "<linkSelector>div &gt; a.prodimage.f-row</linkSelector>"
                + "<imageSelector>div &gt; a.prodimage.f-row &gt; span &gt; img</imageSelector>"
                + "<priceSelector>div &gt; div.price.f-row &gt; em</priceSelector>"
                + "</shop></shops>";
        File file = dir.resolve("test.xml").toFile();
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        ShopDefinition definition = MarshallerUtil.unmarshall(file).getShops().get(0);
        assertThat(definition.validate()).isEmpty();
        assertThat(definition.getListSelectors()).containsExactly("#box_mainproducts > div.innerbox > div > div");
        assertThat(definition.toShop().getBaseUrl()).isEqualTo("https://polarms.pl/");
    }
}
