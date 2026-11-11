package smon.cli;

import org.junit.jupiter.api.Test;
import smon.dto.ShopDefinition;
import smon.util.SelectorCheck;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The template written by 'shop export --blank' must be importable as it is (H2). */
class ExportTemplateTest {

    @Test
    void templatePassesValidation() {
        assertThat(ExportCommand.template().validate()).isEmpty();
    }

    @Test
    void everyTemplateSelectorParses() {
        ShopDefinition template = ExportCommand.template();
        List<List<String>> lists = java.util.Arrays.asList(template.getListSelectors(), template.getDescriptionSelectors(),
                template.getLinkSelectors(), template.getImageSelectors(), template.getPriceSelectors());
        for (List<String> selectors : lists) {
            for (String selector : selectors) {
                assertThat(SelectorCheck.syntaxError(selector)).as(selector).isNull();
            }
        }
    }

    @Test
    void searchUrlHasNoHelpText() {
        assertThat(ExportCommand.template().getSearchUrl()).isEqualTo("https://shop.example.pl/search?q={search}");
    }
}
