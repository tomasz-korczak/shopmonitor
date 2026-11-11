package smon.extractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PriceResolverTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            // plain and decimal
            "1299 zł                | 1299",
            "1299,00 zł             | 1299.00",
            "1299.99 PLN            | 1299.99",
            "12,5 zł                | 12.5",
            "0,99 zł                | 0.99",
            "€ 49.90                | 49.90",
            // thousands separators
            "1 299,00 zł            | 1299.00",
            "1 299,00 zł  | 1299.00",
            "1 299,00 zł       | 1299.00",
            "1.299,00 zł            | 1299.00",
            "1,299.00 EUR           | 1299.00",
            "1'299.00               | 1299.00",
            "1.234.567,89 zł        | 1234567.89",
            "12 345 zł              | 12345",
            // M4: a single separator followed by exactly 3 digits is a thousands separator
            "1.299 zł               | 1299",
            "1,299 zł               | 1299",
            // M4: only the first number counts
            "od 100 zł do 200 zł    | 100",
            "100zł 200zł            | 100",
            "Cena: 1 500,00 zł (netto 1 219,51 zł) | 1500.00",
            // noise around the number
            "Cena: 350,00 zł/szt.   | 350.00",
            "350,00 zł/szt.brutto*1 | 350.00",
            "1500,- zł              | 1500",
            "  2 499 zł  ...        | 2499",
            // M1: any group of 3 or more digits is a thousands separator, 1-2 digits is the decimal part
            "1.2345 zł              | 12345",
            "1,2345 zł              | 12345",
            "1.299.99 zł            | 1299.99",
            "2.5 zł                 | 2.5",
            "1,5 zł                 | 1.5",
            "1.234.5 zł             | 1234.5",
    })
    void resolvesPrice(String text, String expected) {
        assertThat(new PriceResolver(text).resolve()).isEqualTo(expected);
        assertThat(Double.parseDouble(new PriceResolver(text).resolve())).isEqualTo(Double.parseDouble(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "zł", "Zapytaj o cenę", "brak"})
    void noNumberGivesNull(String text) {
        assertThat(new PriceResolver(text).resolve()).isNull();
    }

    @Test
    void nullGivesNull() {
        assertThat(new PriceResolver(null).resolve()).isNull();
    }
}
