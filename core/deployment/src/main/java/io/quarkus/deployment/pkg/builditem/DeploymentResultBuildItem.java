
package io.quarkus.deployment.pkg.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DeploymentResultBuildItem extends SimpleBuildItem {

    private final String name;
    private final Map<String, String> labels;
    private final String url;

    public DeploymentResultBuildItem(String name, Map<String, String> labels, String url) {
        this.name = name;
        this.labels = labels;
        this.url = url;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, String> getLabels() {
        return this.labels;
    }

    public String getUrl() {
        return url;
    }
}
