package io.quarkus.deployment.dev.remote;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;

public class RemoteDevDeployHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        DeploymentResultBuildItem result = buildResult.consumeOptional(DeploymentResultBuildItem.class);
        if (result != null) {
            Map<String, String> ctx = (Map<String, String>) o;
            ctx.put("url", result.getUrl());
        }

    }
}
