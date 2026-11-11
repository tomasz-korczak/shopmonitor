package smon.config;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards L14: the packaged application.properties must not hold the DB user or password, mail account, or addresses. */
class ClasspathConfigTest {

    private static final List<String> PERSONAL = Arrays.asList(
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.mail.username",
            "spring.mail.password",
            "email.sendTo",
            "email.footer.unsubscribe-to",
            "email.footer.copyright");

    private static List<String> packagedLines() throws IOException {
        try (InputStream in = ClasspathConfigTest.class.getClassLoader().getResourceAsStream("application.properties");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    @Test
    void packagedConfigSetsNoPersonalKeys() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = ClasspathConfigTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
        }

        assertThat(properties.stringPropertyNames()).doesNotContainAnyElementsOf(PERSONAL);
    }

    @Test
    void packagedConfigContainsNoAddress() throws IOException {
        assertThat(packagedLines())
                .filteredOn(line -> !line.trim().startsWith("#"))
                .noneMatch(line -> line.contains("@"));
    }
}
