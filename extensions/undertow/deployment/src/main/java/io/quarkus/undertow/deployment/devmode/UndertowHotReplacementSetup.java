package io.quarkus.undertow.deployment.devmode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.undertow.runtime.UndertowDeploymentTemplate;

public class UndertowHotReplacementSetup implements HotReplacementSetup {

    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path i : context.getResourcesDir()) {
            Path resolved = i.resolve(META_INF_RESOURCES);
            if (Files.exists(resolved)) {
                resources.add(resolved);
            }
        }
        UndertowDeploymentTemplate.setHotDeploymentResources(resources);
    }

    @Override
    public void handleFailedInitialStart() {
    }
}
