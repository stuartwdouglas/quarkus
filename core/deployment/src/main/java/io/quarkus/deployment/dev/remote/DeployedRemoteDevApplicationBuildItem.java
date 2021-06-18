package io.quarkus.deployment.dev.remote;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents the results of a deployment of a
 */
public final class DeployedRemoteDevApplicationBuildItem extends SimpleBuildItem {

    private final String url;
    private final String password;

    public DeployedRemoteDevApplicationBuildItem(String url, String password) {
        this.url = url;
        this.password = password;
    }
}
