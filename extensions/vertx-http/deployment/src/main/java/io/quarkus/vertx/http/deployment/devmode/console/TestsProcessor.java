package io.quarkus.vertx.http.deployment.devmode.console;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class TestsProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem results() {
        return new DevConsoleTemplateInfoBuildItem("tests", RuntimeUpdatesProcessor.INSTANCE.getTestSupport().getResults());
    }
}
