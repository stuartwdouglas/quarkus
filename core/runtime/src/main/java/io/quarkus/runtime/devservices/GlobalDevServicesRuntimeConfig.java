package io.quarkus.runtime.devservices;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "devservices", phase = ConfigPhase.RUN_TIME)
public class GlobalDevServicesRuntimeConfig {

    /**
     * Dev services that can be started by manually specifying the image.
     */
    @ConfigItem
    public Map<String, CustomDevServicesRuntimeConfig> custom;

    @ConfigGroup
    public static class CustomDevServicesRuntimeConfig {
        /**
         * The results of starting the dev services at runtime.
         *
         * Note that this property should not be explicitly set, it is set when the dev services are started,
         * and is intended for use in other configuration entries.
         */
        @ConfigItem
        public Map<String, String> mappedPort;
        /**
         * The results of starting the dev services at runtime.
         *
         * Note that this property should not be explicitly set, it is set when the dev services are started,
         * and is intended for use in other configuration entries.
         */
        @ConfigItem
        public Map<String, String> mappedUrl;

        /**
         * If only a single port is mapped then this will contain the URL that can be used to connect to the service.
         */
        @ConfigItem
        public Optional<String> url;

        /**
         * The hostname of the container
         */
        @ConfigItem
        public Optional<String> host;
    }
}
