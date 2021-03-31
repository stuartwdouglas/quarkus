package io.quarkus.deployment.dev.testing.runner;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.console.InputHandler;
import io.quarkus.deployment.dev.console.QuarkusConsole;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;

public class TestRunner {

    private static final Logger log = Logger.getLogger(TestRunner.class);
    private static final AtomicLong COUNTER = new AtomicLong();
    public static final String DISABLED_PROMPT = "\u001b[33mTests Disabled, press [e] to enable\u001b[0m";
    public static final String FIRST_RUN_PROMPT = "\u001b[33mRunning Tests for the first time\u001b[0m";
    public static final String RUNNING_PROMPT = "Press [r] to re-run, [v] to view full results, [d] to disable, [?] for more options>";

    private final DevModeContext devModeContext;
    private final CuratedApplication testApplication;
    private final Consumer<TestRunResults> resultHandler;

    private boolean testsRunning = false;
    private boolean testsQueued = false;
    private ClassScanResult queuedChanges = null;

    private Throwable compileProblem;

    private final TestClassUsages testClassUsages = new TestClassUsages();
    private final TestState testState;
    private boolean paused;
    /**
     * disabled is different to paused, when the runner is disabled we abort all runs rather than pausing them.
     */
    private volatile boolean disabled = true;
    private volatile boolean firstRun = true;
    private volatile boolean consoleOutput;
    volatile InputHandler.ConsoleStatus promptHandler;
    private JunitTestRunner runner;

    private final InputHandler inputHandler = new InputHandler() {

        @Override
        public void handleInput(int[] keys) {
            if (disabled) {
                for (int i : keys) {
                    if (i == 'e') {
                        TestSupport.instance().start(true);
                    }
                }
            } else if (!firstRun) {
                for (int k : keys) {
                    if (k == 'r') {
                        runTests();
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
            TestRunner.this.promptHandler = promptHandler;
            promptHandler.setPrompt(DISABLED_PROMPT);
        }
    };

    public TestRunner(DevModeContext devModeContext, CuratedApplication testApplication, Consumer<TestRunResults> resultHandler,
            TestState testState) {
        this.devModeContext = devModeContext;
        this.testApplication = testApplication;
        this.resultHandler = resultHandler;
        this.testState = testState;
        QuarkusConsole.INSTANCE.pushInputHandler(inputHandler);
    }

    public void runTests() {
        runTests(null);
    }

    public synchronized long getRunningTestRunId() {
        if (testsRunning) {
            return COUNTER.get();
        }
        return -1;
    }

    public void runTests(ClassScanResult classScanResult) {
        if (compileProblem != null) {
            return;
        }
        if (testApplication == null) {
            return;
        }
        if (disabled) {
            return;
        }
        synchronized (TestRunner.this) {
            if (testsRunning) {
                if (testsQueued) {
                    if (queuedChanges != null) { //if this is null a full run is scheduled
                        this.queuedChanges = ClassScanResult.merge(this.queuedChanges, classScanResult);
                    }
                } else {
                    testsQueued = true;
                    this.queuedChanges = classScanResult;
                }
                return;
            } else {
                testsRunning = true;
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ContinuousTestingWebsocketListener.setInProgress(true);
                    runInternal(classScanResult);
                } finally {
                    waitTillResumed();
                    boolean run = false;
                    ClassScanResult current = null;
                    synchronized (TestRunner.this) {
                        if (!disabled) {
                            testsRunning = false;
                            if (testsQueued) {
                                testsQueued = false;
                                run = true;
                            }
                            current = queuedChanges;
                            queuedChanges = null;
                        }
                    }
                    if (run) {
                        runTests(current);
                    } else {
                        ContinuousTestingWebsocketListener.setInProgress(false);
                    }
                }
            }
        }, "Test runner thread");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void pause() {
        //todo
        paused = true;
        if (runner != null) {
            runner.pause();
        }
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
        if (runner != null) {
            runner.resume();
        }
    }

    public synchronized void disable() {
        promptHandler.setPrompt(DISABLED_PROMPT);
        promptHandler.setStatus(null);
        ContinuousTestingWebsocketListener.setRunning(false);
        disabled = true;
        notifyAll();
        if (runner != null) {
            runner.abort();
        }
    }

    public synchronized void enable() {
        if (!disabled) {
            return;
        }
        firstRun = true;
        promptHandler.setStatus(null);
        promptHandler.setPrompt(FIRST_RUN_PROMPT);
        ContinuousTestingWebsocketListener.setRunning(true);
        disabled = false;
    }

    private void runInternal(ClassScanResult classScanResult) {
        final long runId = COUNTER.incrementAndGet();

        AtomicLong totalNoTests = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicLong methodCount = new AtomicLong();
        AtomicLong failureCount = new AtomicLong();
        AtomicReference<TestRunResults> resultsRef = new AtomicReference<>();
        synchronized (this) {
            if (runner != null) {
                throw new IllegalStateException("Tests already in progress");
            }
            if (disabled) {
                return;
            }
            runner = new JunitTestRunner.Builder()
                    .setClassScanResult(classScanResult)
                    .setDevModeContext(devModeContext)
                    .setRunId(runId)
                    .setTestState(testState)
                    .setTestClassUsages(testClassUsages)
                    .setTestApplication(testApplication)
                    .setListener(new JunitTestRunner.TestListener() {
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
                            promptHandler.setStatus("Running " + methodCount.get() + "/" + totalNoTests
                                    + (failureCount.get() == 0 ? "." : ". " + failureCount + " failures so far."));
                        }

                        @Override
                        public void runComplete(TestRunResults results) {
                            resultsRef.set(results);
                        }

                        @Override
                        public void runAborted() {
                        }
                    })
                    .build();
            if (paused) {
                runner.pause();
            }
        }
        runner.runTests();
        synchronized (this) {
            runner = null;
        }
        TestRunResults results = resultsRef.get();
        if (disabled || results == null) {
            return;
        }
        resultHandler.accept(results);
        if (results.getCurrentFailing().isEmpty()) {
            promptHandler.setStatus(
                    "\u001B[32mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                            + " were skipped. Tests took " + (results.getTotalTime())
                            + "ms." + "\u001b[0m");
        } else {
            StringBuilder sb = new StringBuilder(
                    "\u001B[91mTest run failed, " + methodCount.get() + " tests were run, " + results.getCurrentFailing().size()
                            + " failed, "
                            + skipped.get()
                            + " were skipped. Tests took " + results.getTotalTime() + "ms");
            if (consoleOutput) {
                for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                    for (TestResult test : classEntry.getValue().getFailing()) {
                        log.error(
                                "Test " + test.getDisplayName() + " failed \n",
                                test.getTestExecutionResult().getThrowable().get());
                    }
                }
            }
            promptHandler.setStatus(sb.toString() + "\u001b[0m");
        }
        if (firstRun) {
            firstRun = false;
            promptHandler.setPrompt(RUNNING_PROMPT);
        }

    }

    private void printFullResults() {
        for (TestClassResult i : testState.getFailingClasses()) {
            for (TestResult failed : i.getFailing()) {
                log.error(
                        "Test " + failed.getDisplayName() + " failed "
                                + failed.getTestExecutionResult().getStatus()
                                + "\n",
                        failed.getTestExecutionResult().getThrowable().get());
            }
        }

    }

    public void waitTillResumed() {
        synchronized (TestRunner.this) {
            while (paused && !disabled) {
                try {
                    TestRunner.this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (disabled) {
                throw new TestAbortedException("Tests are disabled");
            }
        }
    }

    public synchronized void testCompileFailed(Throwable e) {
        compileProblem = e;
        log.error("Test compile failed", e);
    }

    public synchronized void testCompileSucceeded() {
        compileProblem = null;
    }

    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public boolean getConsoleOutput() {
        return consoleOutput;
    }

    public TestState getResults() {
        return testState;
    }

    public boolean isRunning() {
        return testsRunning;
    }
}
