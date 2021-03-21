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
     * <p>
     * If this is true results will be printed to the console. If this is false
     * continuous testing can still be used, however it needs to be explicitly
     * started from the Dev UI, and console output will not be printed.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

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
}
