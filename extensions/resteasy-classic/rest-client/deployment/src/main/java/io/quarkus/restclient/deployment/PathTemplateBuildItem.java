package io.quarkus.restclient.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

final class PathTemplateBuildItem extends SimpleBuildItem {

    private Map<String, String> pathTemplates;

    public PathTemplateBuildItem(Map<String, String> pathTemplates) {
        this.pathTemplates = pathTemplates;
    }

    public Map<String, String> getPathTemplates() {
        return pathTemplates;
    }
}
