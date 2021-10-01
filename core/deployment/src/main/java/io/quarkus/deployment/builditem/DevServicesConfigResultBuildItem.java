package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Configuration property that is the result of start dev services.
 *
 * Used to start and configure dev services, any processor starting dev services should produce these items.
 *
 * Quarkus will make sure the relevant settings are present in both JVM and native modes.
 */
public final class DevServicesConfigResultBuildItem extends MultiBuildItem {

    /**
     * bit of a hack, but this prefix is used to specify values that only apply to containers running
     * inside the shared docker network (and not on the host network).
     *
     * This allows custom dev services to talk to other dev services
     */
    public static final String INTERNAL_PREFIX = "$$[DOCKER-INTERNAL]$$";

    final String key;
    final String value;

    public DevServicesConfigResultBuildItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
