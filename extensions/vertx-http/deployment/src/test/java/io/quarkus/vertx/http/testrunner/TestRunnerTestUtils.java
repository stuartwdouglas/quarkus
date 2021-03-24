package io.quarkus.vertx.http.testrunner;

import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for testing the test runner itself
 */
public class TestRunnerTestUtils {

    public static TestStatus waitForFirstRunToComplete() {
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).pollInterval(50, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
                return ts.getLastRun() == 1;
            }
        });
        return RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
    }

}
