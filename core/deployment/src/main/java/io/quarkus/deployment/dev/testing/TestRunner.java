package io.quarkus.deployment.dev.testing;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;

public class TestRunner {

    private static final Logger log = Logger.getLogger(TestRunner.class);
    private static final AtomicLong COUNTER = new AtomicLong();

    private final TestSupport testSupport;
    private final DevModeContext devModeContext;
    private final CuratedApplication testApplication;

    private boolean testsRunning = false;
    private boolean testsQueued = false;
    private ClassScanResult queuedChanges = null;

    private Throwable compileProblem;

    private final TestClassUsages testClassUsages = new TestClassUsages();
    private boolean paused;
    /**
     * disabled is different to paused, when the runner is disabled we abort all runs rather than pausing them.
     */
    private volatile boolean disabled = true;
    private volatile boolean firstRun = true;
    private JunitTestRunner runner;

    public TestRunner(TestSupport testSupport, DevModeContext devModeContext, CuratedApplication testApplication) {
        this.testSupport = testSupport;
        this.devModeContext = devModeContext;
        this.testApplication = testApplication;
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
                    }
                }
            }
        }, "Test runner thread");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void pause() {
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
                    .setTestState(testSupport.testState)
                    .setTestClassUsages(testClassUsages)
                    .setTestApplication(testApplication)
                    .setIncludeTags(testSupport.includeTags)
                    .setExcludeTags(testSupport.excludeTags)
                    .setInclude(testSupport.include)
                    .setExclude(testSupport.exclude);
            for (TestListener i : testSupport.testListeners) {
                i.testRunStarted(builder::addListener);
            }
            builder.addListener(new TestRunListener() {
                @Override
                public void runComplete(TestRunResults results) {
                    testSupport.testRunResults = results;

                }
            });
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

    public boolean isRunning() {
        return testsRunning;
    }
}
