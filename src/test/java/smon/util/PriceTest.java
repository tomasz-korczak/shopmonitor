package smon.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "1299, 1299.00",
            "1299.5, 1299.50",
            "1299.00, 1299.00",
            "0, 0.00",
            "99999999.99, 99999999.99",
            "1299.000, 1299.00",
    })
    void validPricesHaveScaleTwo(String resolved, String expected) {
        BigDecimal price = Price.parse(resolved);
        assertThat(price).isEqualTo(new BigDecimal(expected));
        assertThat(price.scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"100000000", "12345678901234567890", "-1", "1299.001", "abc", "NaN", ""})
    void notAPriceGivesNull(String resolved) {
        assertThat(Price.parse(resolved)).isNull();
    }

    @Test
    void nullGivesNull() {
        assertThat(Price.parse(null)).isNull();
    }

    @Test
    void formatUsesTwoDecimalsAndComma() {
        assertThat(Price.format(new BigDecimal("1299"))).isEqualTo("1299,00");
        assertThat(Price.format(new BigDecimal("12.5").setScale(2))).isEqualTo("12,50");
        assertThat(Price.format(null)).isEmpty();
    }
}
