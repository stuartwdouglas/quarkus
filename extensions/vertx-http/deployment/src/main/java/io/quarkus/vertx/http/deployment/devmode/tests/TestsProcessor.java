package io.quarkus.vertx.http.deployment.devmode.tests;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class TestsProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem results() {
        return new DevConsoleTemplateInfoBuildItem("tests", RuntimeUpdatesProcessor.INSTANCE.getTestSupport().getResults());
    }

    @BuildStep
    DevConsoleRouteBuildItem handlePost() {
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/status", "GET", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                jsonResponse(event);

            }
        });
        //

    }

    public MultiMap jsonResponse(RoutingContext event) {
        return event.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
    }
}
