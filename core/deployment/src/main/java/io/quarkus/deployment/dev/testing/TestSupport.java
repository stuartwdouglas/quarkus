package io.quarkus.deployment.dev.testing;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.QuarkusCompiler;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.testing.runner.TestRunner;
import io.quarkus.deployment.dev.testing.runner.TestState;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;

public class TestSupport {

    private static final Logger log = Logger.getLogger(TestSupport.class);

    final CuratedApplication curatedApplication;
    final List<CompilationProvider> compilationProviders;
    final DevModeContext context;
    final List<Runnable> startListeners = new ArrayList<>();
    final List<Runnable> stopListeners = new ArrayList<>();
    final TestState testState = new TestState();

    volatile CuratedApplication testCuratedApplication;
    volatile QuarkusCompiler compiler;
    volatile TestRunner testRunner;
    volatile boolean started;
    volatile TestRunResults testRunResults;
    private final List<CompletableFuture<TestRunResults>> resultsListeners = new ArrayList<>();

    public TestSupport(CuratedApplication curatedApplication, List<CompilationProvider> compilationProviders,
            DevModeContext context) {
        this.curatedApplication = curatedApplication;
        this.compilationProviders = compilationProviders;
        this.context = context;
    }

    public static TestSupport instance() {
        return RuntimeUpdatesProcessor.INSTANCE.getTestSupport();
    }

    public boolean isRunning() {
        if (testRunner == null) {
            return false;
        }
        return testRunner.isRunning();
    }

    /**
     * Gets the results for an already completed test run, whoes ID is equal to or larger than the provided id.
     */
    public synchronized TestRunResults getCompletedResults(long expectedId) {
        if (testRunner == null) {
            throw new IllegalStateException("Test runner not started");
        }
        TestRunResults tr = testRunResults;
        if (tr != null) {
            if (tr.getId() >= expectedId) {
                return tr;
            }
        }
        throw new IllegalStateException("Test run with provided id has not completed yet");
    }

    /**
     * Gets the results for a test run, whoes ID is equal to or larger than the provided id.
     * <p>
     * This can be used to wait for a currently running test suite by using {@link #getStatus()} to
     * get the currently running id, and then passing it to this method.
     */
    public synchronized CompletableFuture<TestRunResults> getRunningResults(long expectedId) {
        CompletableFuture<TestRunResults> ret = new CompletableFuture<>();
        if (testRunner == null) {
            ret.completeExceptionally(new IllegalStateException("Test runner not started"));
            return ret;
        }
        TestRunResults tr = testRunResults;
        if (tr != null) {
            if (tr.getId() >= expectedId) {
                ret.complete(tr);
                return ret;
            }
        }
        long status = testRunner.getRunningTestRunId();
        if (status == -1 || expectedId > status) {
            ret.completeExceptionally(new IllegalStateException("Test run with provided id has not started yet"));
        }
        resultsListeners.add(ret);
        return ret;
    }

    /**
     * returns the current status of the test runner.
     * <p>
     * This is expressed in terms of test run ids, where -1 signifies
     * no result.
     */
    public RunStatus getStatus() {
        if (testRunner == null) {
            return new RunStatus(-1, -1);
        }
        long last = -1;
        //get the running test id before the current status
        //otherwise there is a race where they both could be -1 even though it has started
        long runningTestRunId = testRunner.getRunningTestRunId();
        TestRunResults tr = testRunResults;
        if (tr != null) {
            last = tr.getId();
        }
        return new RunStatus(last, runningTestRunId);
    }

    public void start() {
        start(false);
    }

    public void start(boolean runTests) {
        if (!started) {
            synchronized (this) {
                if (!started) {
                    ContinuousTestingWebsocketListener
                            .setLastState(new ContinuousTestingWebsocketListener.State(true, true, 0L, 0L, 0L, 0L));
                    try {
                        if (context.getApplicationRoot().getTest().isPresent()) {
                            started = true;
                            runTests = true;
                            if (testCuratedApplication == null) {
                                testCuratedApplication = curatedApplication.getQuarkusBootstrap().clonedBuilder()
                                        .setMode(QuarkusBootstrap.Mode.TEST)
                                        .setDisableClasspathCache(true)
                                        .setIsolateDeployment(true)
                                        .setTest(true)
                                        .setAuxiliaryApplication(true)
                                        .addAdditionalApplicationArchive(new AdditionalDependency(
                                                Paths.get(context.getApplicationRoot().getTest().get().getClassesPath()), true,
                                                true))
                                        .build()
                                        .bootstrap();
                                compiler = new QuarkusCompiler(testCuratedApplication, compilationProviders, context);
                                testRunner = new TestRunner(context, testCuratedApplication, new Consumer<TestRunResults>() {
                                    @Override
                                    public void accept(TestRunResults testRunResults) {
                                        synchronized (TestSupport.this) {
                                            TestSupport.this.testRunResults = testRunResults;
                                            for (CompletableFuture<TestRunResults> i : resultsListeners) {
                                                i.complete(testRunResults);
                                            }
                                            resultsListeners.clear();
                                        }
                                        ContinuousTestingWebsocketListener.setLastState(
                                                new ContinuousTestingWebsocketListener.State(true, testRunner.isRunning(),
                                                        testRunResults.getTestsPassed() +
                                                                testRunResults.getTestsFailed() +
                                                                testRunResults.getTestsSkipped(),
                                                        testRunResults.getTestsPassed(),
                                                        testRunResults.getTestsFailed(), testRunResults.getTestsSkipped()));

                                    }
                                }, testState);
                            }
                            for (Runnable i : startListeners) {
                                i.run();
                            }
                            testRunner.enable();
                        }
                    } catch (Exception e) {
                        log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                    }

                }
            }
        }
        if (runTests) {
            testRunner.runTests();
        }
    }

    public synchronized void stop() {
        if (started) {
            started = false;
            for (Runnable i : stopListeners) {
                i.run();
            }
            testRunner.disable();
        }
    }

    public void addStartListener(Runnable runnable) {
        boolean run = false;
        synchronized (this) {
            startListeners.add(runnable);
            if (started) {
                run = true;
            }
        }
        if (run) {
            //run outside lock
            runnable.run();
        }
    }

    public synchronized void addStopListener(Runnable runnable) {
        stopListeners.add(runnable);
    }

    public boolean isStarted() {
        return started;
    }

    public TestRunner getTestRunner() {
        return testRunner;
    }

    public CuratedApplication getCuratedApplication() {
        return curatedApplication;
    }

    public QuarkusCompiler getCompiler() {
        return compiler;
    }

    public TestRunResults getTestRunResults() {
        return testRunResults;
    }

    public synchronized void pause() {
        if (started) {
            testRunner.pause();
        }
    }

    public synchronized void resume() {
        if (started) {
            testRunner.resume();
        }
    }

    public TestRunResults getResults() {
        return testRunResults;
    }

    public void setTags(List<String> includeTags, List<String> excludeTags) {
        testRunner.setTags(includeTags, excludeTags);
    }

    public static class RunStatus {

        final long lastRun;
        final long running;

        public RunStatus(long lastRun, long running) {
            this.lastRun = lastRun;
            this.running = running;
        }

        public long getLastRun() {
            return lastRun;
        }

        public long getRunning() {
            return running;
        }
    }
}
