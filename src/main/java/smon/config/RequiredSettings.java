package smon.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stops the app at start when a setting that has no default in the packaged application.properties is absent or blank.
 * Without this check a missing DB password shows up as an H2 login error, and a missing mail key as a placeholder error.
 * Registered in META-INF/spring.factories. Runs before logging starts, so it prints to System.err.
 */
public class RequiredSettings implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final List<String> KEYS = Arrays.asList(
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.mail.host",
            "spring.mail.username",
            "spring.mail.password",
            "email.sendTo",
            "email.display-name");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        List<String> missing = missingSettings(event.getEnvironment());
        if (!missing.isEmpty()) {
            System.err.println("smon-cli: missing settings: " + String.join(", ", missing)
                    + ". Copy config/application.properties.example to config/application.properties and fill it in.");
            System.exit(1);
        }
    }

    /** The required keys that are absent or blank. The file, -D properties and environment variables all count. */
    public static List<String> missingSettings(ConfigurableEnvironment environment) {
        List<String> missing = new ArrayList<>();
        for (String key : KEYS) {
            String value = environment.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                missing.add(key);
            }
        }
        return missing;
    }
}
