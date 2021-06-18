package io.quarkus.deployment.dev.remote;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.runner.bootstrap.AugmentActionImpl;

public class IsolatedRemoteDevDeployTask implements BiConsumer<CuratedApplication, Map<String, Object>> {
    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {
        new AugmentActionImpl(curatedApplication).performCustomBuild(RemoteDevDeployHandler.class.getName(), stringObjectMap,
                DeploymentResultBuildItem.class.getName());
    }
}
