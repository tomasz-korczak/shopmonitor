package smon.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SelectorCheckTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "#products > div.product",
            "h3.name",
            "a[href*=x]",
            "li:nth-child(2n+1)",
            "product-card",
            "div.products > div, ul.list > li",
    })
    void validSelectorsPass(String selector) {
        assertThat(SelectorCheck.syntaxError(selector)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"div[class", "a::b(", "span.price["})
    void malformedSelectorsGiveAMessage(String selector) {
        assertThat(SelectorCheck.syntaxError(selector)).isNotNull().isNotEmpty();
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertThat(SelectorCheck.syntaxError("  h3.name  ")).isNull();
    }
}
