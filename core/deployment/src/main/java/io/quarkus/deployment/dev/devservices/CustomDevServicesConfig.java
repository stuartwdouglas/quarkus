package io.quarkus.deployment.dev.devservices;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CustomDevServicesConfig {

    /**
     * The ports to expose
     */
    @ConfigItem
    public Optional<List<Integer>> ports;
    /**
     * The image to use.
     */
    @ConfigItem
    String image;

    /**
     * The environment to pass to the image.
     */
    @ConfigItem
    Map<String, String> env;

    /**
     * If this container can be shared between running Quarkus instances
     */
    @ConfigItem(defaultValue = "true")
    boolean sharable;

    /**
     * The prefix that should be prepended to the host:port to generate the url property. e.g. for a HTTP based service
     * this would be {@literal http://}.
     */
    @ConfigItem
    Optional<String> urlPrefix;

    /**
     * The suffix that should be appended to the host:port to generate the url property.
     */
    @ConfigItem
    Optional<String> urlSuffix;

    /**
     * If shared networking should be used.
     */
    @ConfigItem(defaultValue = "false")
    boolean useSharedNetwork;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CustomDevServicesConfig that = (CustomDevServicesConfig) o;
        return sharable == that.sharable && Objects.equals(ports, that.ports) && Objects.equals(image, that.image)
                && Objects.equals(env, that.env) && Objects.equals(urlPrefix, that.urlPrefix)
                && Objects.equals(urlSuffix, that.urlSuffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ports, image, env, sharable, urlPrefix, urlSuffix);
    }
}
