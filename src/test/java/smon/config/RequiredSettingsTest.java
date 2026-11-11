package smon.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredSettingsTest {

    private static MockEnvironment withAllKeysExcept(String... skipped) {
        MockEnvironment environment = new MockEnvironment();
        for (String key : RequiredSettings.KEYS) {
            if (!Arrays.asList(skipped).contains(key)) {
                environment.setProperty(key, "value");
            }
        }
        return environment;
    }

    @Test
    void completeSettingsAreNotMissing() {
        assertThat(RequiredSettings.missingSettings(withAllKeysExcept())).isEmpty();
    }

    @Test
    void absentKeyIsReported() {
        assertThat(RequiredSettings.missingSettings(withAllKeysExcept("spring.datasource.password")))
                .containsExactly("spring.datasource.password");
    }

    @Test
    void mailHostIsRequired() {
        // without it Spring creates no JavaMailSender, and the start fails with a bean error instead
        assertThat(RequiredSettings.missingSettings(withAllKeysExcept("spring.mail.host")))
                .containsExactly("spring.mail.host");
    }

    @Test
    void blankKeyIsReported() {
        MockEnvironment environment = withAllKeysExcept("email.sendTo");
        environment.setProperty("email.sendTo", "   ");

        assertThat(RequiredSettings.missingSettings(environment)).containsExactly("email.sendTo");
    }
}
