package io.quarkus.deployment.dev.testing.runner;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.deployment.dev.console.InputHandler;
import io.quarkus.deployment.dev.console.QuarkusConsole;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestController;
import io.quarkus.deployment.dev.testing.TestListener;
import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.deployment.dev.testing.TestRunListener;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;

public class TestConsoleHandler implements TestListener {

    private static final Logger log = Logger.getLogger("io.quarkus.test");

    public static final String DISABLED_PROMPT = "\u001b[33mTests Disabled, press [e] to enable\u001b[0m";
    public static final String FIRST_RUN_PROMPT = "\u001b[33mRunning Tests for the first time\u001b[0m";
    public static final String RUNNING_PROMPT = "Press [r] to re-run, [v] to view full results, [d] to disable, [?] for more options>";

    boolean firstRun = true;
    boolean disabled = true;
    volatile InputHandler.ConsoleStatus promptHandler;
    volatile TestController testController;
    private String lastStatus;

    public void install() {
        QuarkusConsole.INSTANCE.pushInputHandler(inputHandler);
    }

    private final InputHandler inputHandler = new InputHandler() {

        @Override
        public void handleInput(int[] keys) {
            if (disabled) {
                for (int i : keys) {
                    if (i == 'e') {
                        TestSupport.instance().start();
                    }
                }
            } else if (!firstRun) {
                for (int k : keys) {
                    if (k == 'r') {
                        testController.runAllTests();
                    } else if (k == 'v') {
                        printFullResults();
                    } else if (k == 'd') {
                        TestSupport.instance().stop();
                    }
                }
            }
        }

        @Override
        public void promptHandler(InputHandler.ConsoleStatus promptHandler) {
            TestConsoleHandler.this.promptHandler = promptHandler;
        }
    };

    @Override
    public void listenerRegistered(TestController testController) {
        this.testController = testController;
        promptHandler.setStatus(DISABLED_PROMPT);
    }

    private void printFullResults() {
        for (TestClassResult i : testController.currentState().getFailingClasses()) {
            for (TestResult failed : i.getFailing()) {
                log.error(
                        "Test " + failed.getDisplayName() + " failed "
                                + failed.getTestExecutionResult().getStatus()
                                + "\n",
                        failed.getTestExecutionResult().getThrowable().get());
            }
        }

    }

    @Override
    public void testsEnabled() {
        disabled = false;
        if (firstRun) {
            promptHandler.setStatus(null);
            promptHandler.setPrompt(FIRST_RUN_PROMPT);
        } else {
            promptHandler.setPrompt(RUNNING_PROMPT);
            promptHandler.setStatus(lastStatus);
        }
    }

    @Override
    public void testsDisabled() {
        disabled = true;
        promptHandler.setPrompt(DISABLED_PROMPT);
        promptHandler.setStatus(null);
    }

    @Override
    public void testRunStarted(Consumer<TestRunListener> listenerConsumer) {

        AtomicLong totalNoTests = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicLong methodCount = new AtomicLong();
        AtomicLong failureCount = new AtomicLong();
        listenerConsumer.accept(new TestRunListener() {
            @Override
            public void runStarted(long toRun) {
                totalNoTests.set(toRun);
                promptHandler.setStatus("Running 0/" + toRun + ".");
            }

            @Override
            public void testComplete(TestResult result) {

                if (result.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failureCount.incrementAndGet();
                } else if (result.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.incrementAndGet();
                }
                methodCount.incrementAndGet();
            }

            @Override
            public void runComplete(TestRunResults results) {
                if (results.getCurrentFailing().isEmpty()) {
                    lastStatus = "\u001B[32mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                            + " were skipped. Tests took " + (results.getTotalTime())
                            + "ms." + "\u001b[0m";
                } else {
                    StringBuilder sb = new StringBuilder(
                            "\u001B[91mTest run failed, " + methodCount.get() + " tests were run, "
                                    + results.getCurrentFailing().size()
                                    + " failed, "
                                    + skipped.get()
                                    + " were skipped. Tests took " + results.getTotalTime() + "ms");
                    for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                        for (TestResult test : classEntry.getValue().getFailing()) {
                            log.error(
                                    "Test " + test.getDisplayName() + " failed \n",
                                    test.getTestExecutionResult().getThrowable().get());
                        }
                    }
                    lastStatus = sb.toString() + "\u001b[0m";
                }
                //this will re-print when using the basic console
                promptHandler.setPrompt(RUNNING_PROMPT);
                promptHandler.setStatus(lastStatus);
            }

            @Override
            public void runAborted() {
                promptHandler.setStatus("Test run aborted.");
            }

            @Override
            public void testStarted(TestIdentifier testIdentifier, String className) {
                promptHandler.setStatus("Running " + methodCount.get() + "/" + totalNoTests
                        + (failureCount.get() == 0 ? "."
                                : ". " + failureCount + " failures so far.")
                        + " Running: "
                        + className + "#" + testIdentifier.getDisplayName());
            }
        });

    }

}
