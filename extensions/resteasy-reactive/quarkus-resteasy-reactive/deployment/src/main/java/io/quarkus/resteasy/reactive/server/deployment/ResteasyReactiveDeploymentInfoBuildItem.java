package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.DeploymentInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResteasyReactiveDeploymentInfoBuildItem extends SimpleBuildItem {

    private final DeploymentInfo deploymentInfo;
    private final String applicationPath;

    public ResteasyReactiveDeploymentInfoBuildItem(DeploymentInfo deploymentInfo, String applicationPath) {
        this.deploymentInfo = deploymentInfo;
        this.applicationPath = applicationPath;
    }

    public DeploymentInfo getDeploymentInfo() {
        return deploymentInfo;
    }

    public String getApplicationPath() {
        return applicationPath;
    }
}
