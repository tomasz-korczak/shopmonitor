package smon.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class URLUtilTest {

    @Test
    void fillsAllPlaceholderVariants() {
        String url = "https://polarms.pl/pl/searchquery/{search+}/1/phot/5?url={search,}&q={search}";
        assertThat(URLUtil.prepareSearchUrl(url, "mauser k98"))
                .isEqualTo("https://polarms.pl/pl/searchquery/mauser+k98/1/phot/5?url=mauser,k98&q=mauser%20k98");
    }

    @Test
    void encodesPolishLettersAsUtf8() {
        assertThat(URLUtil.prepareSearchUrl("https://shop.pl/?s={search}", "broń łódź"))
                .isEqualTo("https://shop.pl/?s=bro%C5%84%20%C5%82%C3%B3d%C5%BA");
    }

    @Test
    void encodesUrlSpecialCharacters() {
        assertThat(URLUtil.prepareSearchUrl("https://shop.pl/?s={search}&x=1", "a&b#c?d=e/f+g"))
                .isEqualTo("https://shop.pl/?s=a%26b%23c%3Fd%3De%2Ff%2Bg&x=1");
    }

    @Test
    void regexReplacementCharactersDoNotThrow() {
        // M3: these used to be passed to String.replaceAll as the replacement and threw
        assertThat(URLUtil.prepareSearchUrl("https://shop.pl/?s={search}", "$100 C:\\x"))
                .isEqualTo("https://shop.pl/?s=%24100%20C%3A%5Cx");
    }

    @Test
    void trimsAndCollapsesWhitespaceBetweenWords() {
        assertThat(URLUtil.prepareSearchUrl("https://shop.pl/{search+}", "  colt   1911 "))
                .isEqualTo("https://shop.pl/colt+1911");
    }

    @Test
    void urlWithoutPlaceholderIsUnchanged() {
        assertThat(URLUtil.prepareSearchUrl("http://www.armybazar.eu/pl/wyszukiwanie/", "colt"))
                .isEqualTo("http://www.armybazar.eu/pl/wyszukiwanie/");
    }

    @Test
    void baseUrl() {
        assertThat(URLUtil.getBaseUrl("https://www.netgun.pl/karabiny?x=1")).isEqualTo("https://www.netgun.pl");
        assertThat(URLUtil.getBaseUrl("not a url")).isNull();
    }

    @Test
    void baseUrlKeepsThePort() {
        assertThat(URLUtil.getBaseUrl("http://localhost:8080/x")).isEqualTo("http://localhost:8080");
    }
}
