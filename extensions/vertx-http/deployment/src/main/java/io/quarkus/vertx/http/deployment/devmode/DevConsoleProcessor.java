package io.quarkus.vertx.http.deployment.devmode;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Consumer;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.console.DevConsoleRequest;
import io.quarkus.dev.console.DevConsoleResponse;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.runtime.devmode.DevConsoleFilter;

public class DevConsoleProcessor {

    @BuildStep()
    public FilterBuildItem setupBuildStep(LaunchModeBuildItem launchModeBuildItem) {
        if (!launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            return null;
        }
        DevConsoleManager.registerHandler(new Consumer<DevConsoleRequest>() {
            @Override
            public void accept(DevConsoleRequest devConsoleRequest) {
                devConsoleRequest.getResponse().complete(
                        new DevConsoleResponse(200, Collections.emptyMap(), "Hello World".getBytes(StandardCharsets.UTF_8)));
            }
        });
        return new FilterBuildItem(new DevConsoleFilter(), Integer.MAX_VALUE);

    }

}
