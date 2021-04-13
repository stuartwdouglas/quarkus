package io.quarkus.deployment.dev.testing.runner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.testing.TestListener;
import io.quarkus.deployment.dev.testing.TestRunListener;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;

public class TestRunner {

    private static final Logger log = Logger.getLogger(TestRunner.class);
    private static final AtomicLong COUNTER = new AtomicLong();

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
    private JunitTestRunner runner;

    public TestRunner(DevModeContext devModeContext, CuratedApplication testApplication, Consumer<TestRunResults> resultHandler,
            TestState testState) {
        this.devModeContext = devModeContext;
        this.testApplication = testApplication;
        this.resultHandler = resultHandler;
        this.testState = testState;
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
            ContinuousTestingWebsocketListener.setRunning(true);
            runTests();
        }
    }

    private void runInternal(ClassScanResult classScanResult) {
        final long runId = COUNTER.incrementAndGet();

        AtomicReference<TestRunResults> resultsRef = new AtomicReference<>();
        synchronized (this) {
            if (runner != null) {
                throw new IllegalStateException("Tests already in progress");
            }
            if (disabled) {
                return;
            }
            JunitTestRunner.Builder builder = new JunitTestRunner.Builder()
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
                    .addListener(new TestRunListener() {
                        @Override
                        public void runComplete(TestRunResults results) {
                            resultHandler.accept(results);
                        }

                    });
            for (TestListener i : TestSupport.instance().getTestListeners()) {
                i.testRunStarted(builder::addListener);
            }
            runner = builder
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
        if (firstRun) {
            firstRun = false;
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
