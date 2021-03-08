package io.quarkus.vertx.http.deployment.devmode.console;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class DevConsoleResolvedPathBuildItem extends MultiBuildItem {
    private final String name;
    private final String endpointPath;
    private final boolean absolutePath;

    public DevConsoleResolvedPathBuildItem(String name, String endpointPath) {
        this(name, endpointPath, false);
    }

    public DevConsoleResolvedPathBuildItem(String name, String endpointPath, boolean isAbsolutePath) {
        this.name = name;
        this.endpointPath = endpointPath;
        this.absolutePath = isAbsolutePath;
    }

    public String getName() {
        return name;
    }

    public String getEndpointPath(HttpRootPathBuildItem httpRoot) {
        if (absolutePath) {
            return endpointPath;
        } else {
            return TemplateHtmlBuilder.adjustRoot(httpRoot.getRootPath(), endpointPath);
        }
    }

    public String getEndpointPath(NonApplicationRootPathBuildItem nonAppRoot) {
        if (absolutePath) {
            return endpointPath;
        } else {
            return TemplateHtmlBuilder.adjustRoot(nonAppRoot.getNormalizedHttpRootPath(), endpointPath);
        }
    }
}
