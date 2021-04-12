package io.quarkus.deployment.dev.testing.runner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
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
    volatile List<String> includeTags = Collections.emptyList();
    volatile List<String> excludeTags = Collections.emptyList();
    volatile Pattern include = null;
    volatile Pattern exclude = null;
    volatile InputHandler.ConsoleStatus promptHandler;
    private JunitTestRunner runner;
    private String lastStatus;

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
        disabled = false;
        if (firstRun) {
            promptHandler.setStatus(null);
            promptHandler.setPrompt(FIRST_RUN_PROMPT);
            ContinuousTestingWebsocketListener.setRunning(true);
            runTests();
        } else {
            promptHandler.setPrompt(RUNNING_PROMPT);
            promptHandler.setStatus(lastStatus);
        }
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
                    .setIncludeTags(includeTags)
                    .setExcludeTags(excludeTags)
                    .setInclude(include)
                    .setExclude(exclude)
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
                        }

                        @Override
                        public void runComplete(TestRunResults results) {
                            resultsRef.set(results);
                        }

                        @Override
                        public void runAborted() {
                        }

                        @Override
                        public void testStarted(TestIdentifier testIdentifier, String className) {
                            promptHandler.setStatus("Running " + methodCount.get() + "/" + totalNoTests
                                    + (failureCount.get() == 0 ? "."
                                            : ". " + failureCount + " failures so far.")
                                    + " Running: "
                                    + className + "#" + testIdentifier.getDisplayName());
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
            lastStatus = "\u001B[32mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                    + " were skipped. Tests took " + (results.getTotalTime())
                    + "ms." + "\u001b[0m";
        } else {
            StringBuilder sb = new StringBuilder(
                    "\u001B[91mTest run failed, " + methodCount.get() + " tests were run, " + results.getCurrentFailing().size()
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
        if (firstRun) {
            firstRun = false;
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

    public TestState getResults() {
        return testState;
    }

    public boolean isRunning() {
        return testsRunning;
    }

    public void setTags(List<String> includeTags, List<String> excludeTags) {
        this.includeTags = includeTags;
        this.excludeTags = excludeTags;
    }

    public void setPatterns(String include, String exclude) {
        this.include = include == null ? null : Pattern.compile(include);
        this.exclude = exclude == null ? null : Pattern.compile(exclude);
    }
}
