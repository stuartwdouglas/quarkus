package io.quarkus.vertx.http.deployment.devmode.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TestsProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem results() {
        return new DevConsoleTemplateInfoBuildItem("tests", TestSupport.instance());
    }

    @BuildStep
    DevConsoleRouteBuildItem handleTestStatus() {
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/status", "GET", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                jsonResponse(event);
                TestSupport.RunStatus status = TestSupport.instance().getStatus();
                TestStatus testStatus = new TestStatus();
                testStatus.setLastRun(status.getLastRun());
                testStatus.setRunning(status.getRunning());
                if (status.getLastRun() > 0) {
                    TestRunResults result = TestSupport.instance().getCompletedResults(status.getLastRun());
                    testStatus.setTestsFailed(result.getTestsFailed());
                    testStatus.setTestsPassed(result.getTestsPassed());
                    testStatus.setTestsSkipped(result.getTestsSkipped());
                    testStatus.setTestsRun(result.getTestsFailed() + result.getTestsPassed());
                }
                event.response().end(JsonObject.mapFrom(testStatus).encode());
            }
        });
    }

    @BuildStep
    DevConsoleRouteBuildItem toggleTestRunner() {
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/toggle", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (TestSupport.instance().isStarted()) {
                    TestSupport.instance().stop();
                } else {
                    TestSupport.instance().start();
                }
            }
        });
    }

    @BuildStep
    DevConsoleRouteBuildItem handleTestResult() {
        //GET tests/status
        //DISABLED, RUNNING (run id), RUN (run id, start time, nextRunQueued)
        //GET tests/results

        return new DevConsoleRouteBuildItem("tests/result", "GET", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                long run = Long.parseLong(event.request().params().get("run"));
                TestSupport.instance().getRunningResults(run).whenComplete(new BiConsumer<TestRunResults, Throwable>() {
                    @Override
                    public void accept(TestRunResults testRunResults, Throwable throwable) {
                        if (throwable != null) {
                            event.fail(throwable);
                        } else {
                            jsonResponse(event);
                            Map<String, ClassResult> results = new HashMap<>();
                            for (Map.Entry<String, TestClassResult> entry : testRunResults.getResults().entrySet()) {
                                results.put(entry.getKey(), new ClassResult(entry.getValue()));
                            }
                            SuiteResult result = new SuiteResult(results);
                            event.response().end(JsonObject.mapFrom(result).encode());

                        }
                    }
                });
            }
        });
    }

    public MultiMap jsonResponse(RoutingContext event) {
        return event.response().headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
    }
}
