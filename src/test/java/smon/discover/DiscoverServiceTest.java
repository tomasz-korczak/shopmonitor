package smon.discover;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import smon.dto.ShopDefinition;
import smon.extractor.DiscoverExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static smon.discover.DiscoverService.DESCRIPTION;
import static smon.discover.DiscoverService.IMAGE;
import static smon.discover.DiscoverService.LINK;
import static smon.discover.DiscoverService.LIST;
import static smon.discover.DiscoverService.PRICE;

/** Runs the discover matching on the saved Bazooka page, with the selectors stored for that shop. */
class DiscoverServiceTest {

    private static final String URL = "https://www.bazooka.sklep.pl/bron-z-demobilu?sort=newest";
    private static final String LIST_SELECTOR =
            "#TPASection_ke02p96z > div > div > div > div > div > div:nth-child(2) > section > div > ul:nth-child(1) > li";

    private static DiscoverExtractor page() throws Exception {
        File file = new File(DiscoverServiceTest.class.getResource("/pages/bazooka-demobil.html").toURI());
        Document document = Jsoup.parse(file, "UTF-8", URL);
        return new DiscoverExtractor(document);
    }

    private static Map<String, List<String>> stored() {
        Map<String, List<String>> stored = new HashMap<>();
        stored.put(LIST, Collections.singletonList(LIST_SELECTOR));
        stored.put(DESCRIPTION, Collections.singletonList("div > div > div > div.Jzh04F > a > div > div > div.EiRAO9 > div:nth-child(1) > p"));
        stored.put(IMAGE, Collections.singletonList("div > div > a > div > div > div.Wo6AuW.vALCqq > div > wow-image > img"));
        stored.put(LINK, Collections.singletonList("div > div > div > div.Jzh04F > a"));
        stored.put(PRICE, Collections.singletonList("div > div > div > div.Jzh04F > a > div > div > div.EiRAO9 > div.ntj2AV > div > div > span.cfpn1d"));
        return stored;
    }

    @Test
    void everyTypeMatchesTheSavedPage() throws Exception {
        DiscoverResult result = new DiscoverService().discover(page(), stored());

        assertThat(result.getSelectors().keySet()).containsExactlyInAnyOrder(LIST, DESCRIPTION, IMAGE, LINK, PRICE);
        assertThat(result.getLacking()).isEmpty();
    }

    @Test
    void malformedStoredSelectorIsSkippedAndTheRestStillMatches() throws Exception {
        Map<String, List<String>> stored = stored();
        stored.put(LIST, Arrays.asList("div[class", LIST_SELECTOR));

        DiscoverResult result = new DiscoverService().discover(page(), stored);

        assertThat(result.getSelectors().get(LIST)).containsExactly(LIST_SELECTOR);
    }

    @Test
    void typeWithNoMatchingSelectorIsLacking() throws Exception {
        Map<String, List<String>> stored = stored();
        stored.put(PRICE, Collections.singletonList("span.nothing-here"));

        DiscoverResult result = new DiscoverService().discover(page(), stored);

        assertThat(result.getLacking()).containsExactly(PRICE);
        assertThat(result.getSelectors()).doesNotContainKey(PRICE);
    }

    @Test
    void definitionTakesTheBestSelectorOfEachTypeAndIsValid() throws Exception {
        DiscoverService service = new DiscoverService();
        DiscoverResult result = service.discover(page(), stored());

        ShopDefinition definition = service.toDefinition("bazooka-demobil", URL, result);

        assertThat(definition.validate()).isEmpty();
        assertThat(definition.getListSelectors()).containsExactly(LIST_SELECTOR);
    }
}
