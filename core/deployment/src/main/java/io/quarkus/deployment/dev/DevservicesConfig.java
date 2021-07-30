package io.quarkus.deployment.dev;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class DevservicesConfig {

    /**
     * If this is set to false all DevServices support is disabled for all extensions
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
