package io.quarkus.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.test.profile=someProfile or -Dquarkus.test.native-image-profile=someProfile
 * <p>
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class TestConfig {

    /**
     * If continuous testing is enabled.
     *
     * <p>
     * If this is true results will be printed to the console. If this is false
     * continuous testing can still be used, however it needs to be explicitly
     * started from the Dev UI, and console output will not be printed.
     */
    @ConfigItem(defaultValue = "PAUSED")
    public Mode continuousTesting;

    /**
     * Tags that should be included for continuous testing.
     */
    @ConfigItem
    public Optional<List<String>> includeTags;

    /**
     * Tags that should be excluded by default with continuous testing.
     *
     * This is ignored if include-tags has been set.
     *
     * Defaults to 'slow'
     */
    @ConfigItem(defaultValue = "slow")
    public Optional<List<String>> excludeTags;

    /**
     * Tests that should be included for continuous testing. This is a regular expression.
     */
    @ConfigItem
    public Optional<String> includePattern;

    /**
     * Tests that should be excluded with continuous testing. This is a regular expression.
     *
     * This is ignored if include-pattern has been set.
     *
     */
    @ConfigItem
    public Optional<String> excludePattern;
    /**
     * Disable the testing status/prompt message at the bottom of the console
     * and log these messages to STDOUT instead.
     *
     * Use this option if your terminal does not support ANSI escape sequences.
     */
    @ConfigItem(defaultValue = "false")
    public boolean basicConsole;

    /**
     * Disable color in the testing status and prompt messages.
     *
     * Use this option if your terminal does not support color.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableColor;

    /**
     * Duration to wait for the native image to built during testing
     */
    @ConfigItem(defaultValue = "PT5M")
    Duration nativeImageWaitTime;

    /**
     * The profile to use when testing the native image
     */
    @ConfigItem(defaultValue = "prod")
    String nativeImageProfile;

    /**
     * Profile related test settings
     */
    @ConfigItem
    Profile profile;

    /**
     * Configures the hang detection in @QuarkusTest. If no activity happens (i.e. no test callbacks are called) over
     * this period then QuarkusTest will dump all threads stack traces, to help diagnose a potential hang.
     *
     * Note that the initial timeout (before Quarkus has started) will only apply if provided by a system property, as
     * it is not possible to read all config sources until Quarkus has booted.
     */
    @ConfigItem(defaultValue = "10m")
    Duration hangDetectionTimeout;

    @ConfigGroup
    public static class Profile {

        /**
         * The profile (dev, test or prod) to use when testing using @QuarkusTest
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "test")
        String profile;

        /**
         * The tags this profile is associated with.
         * When the {@code quarkus.test.profile.tags} System property is set (its value is a comma separated list of strings)
         * then Quarkus will only execute tests that are annotated with a {@code @TestProfile} that has at least one of the
         * supplied (via the aforementioned system property) tags.
         */
        @ConfigItem(defaultValue = "")
        Optional<List<String>> tags;
    }

    public enum Mode {
        PAUSED,
        ENABLED,
        DISABLED

    }
}
