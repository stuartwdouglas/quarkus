package io.quarkus.vertx.http.testrunner;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.function.Supplier;

public class TestRunnerSmokeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(HelloResource.class);
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(SimpleET.class);
                }
            });

    @Test
    public void checkTestsAreRun() throws InterruptedException {
        Thread.sleep(100);
        TestRunnerTestUtils.waitForFirstRunToComplete();;
        TestStatus ts = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/status").as(TestStatus.class);
        Assertions.assertEquals(1L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

    }
}
