package io.quarkus.deployment.dev.testing.runner;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.testing.TestClassResult;
import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.dev.terminal.StatusPrintStream;
import io.quarkus.dev.testing.ContinuousTestingLogHandler;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;
import io.quarkus.dev.testing.TracingHandler;

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
    private boolean disabled;
    private boolean consoleOutput;

    private static final StatusPrintStream OUT;

    static {
        OUT = StatusPrintStream.INSTANCE;
    }

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
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public synchronized void disable() {
        disabled = true;
        notifyAll();
    }

    public synchronized void enable() {
        disabled = false;
    }

    private void runInternal(ClassScanResult classScanResult) {
        long start = System.currentTimeMillis();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            ClassLoader tcl = testApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(tcl);
            ((Consumer) tcl.loadClass(CurrentTestApplication.class.getName()).newInstance()).accept(testApplication);

            List<Class<?>> quarkusTestClasses = discoverTestClasses(devModeContext);

            Launcher launcher = LauncherFactory.create(LauncherConfig.builder().build());
            LauncherDiscoveryRequestBuilder launchBuilder = new LauncherDiscoveryRequestBuilder()
                    .selectors(quarkusTestClasses.stream().map(DiscoverySelectors::selectClass).collect(Collectors.toList()));
            if (classScanResult != null) {
                launchBuilder.filters(testClassUsages.getTestsToRun(classScanResult.getChangedClassNames()));
            }
            LauncherDiscoveryRequest request = launchBuilder
                    .build();
            TestPlan testPlan = launcher.discover(request);
            if (!testPlan.containsTests()) {
                //nothing to see here
                return;
            }
            long toRun = testPlan.countTestIdentifiers(TestIdentifier::isTest);
            OUT.setStatusString("Running 0/" + toRun);

            log.debug("Starting test run with " + quarkusTestClasses.size() + " test cases");
            final AtomicInteger methodCount = new AtomicInteger();
            final AtomicInteger skipped = new AtomicInteger();
            final Map<String, TestExecutionResult> failures = new HashMap<>();
            final List<LogRecord> logOutput = new ArrayList<>();

            final long runId = COUNTER.incrementAndGet();

            ContinuousTestingLogHandler.setLogHandler(new Predicate<LogRecord>() {

                @Override
                public boolean test(LogRecord logRecord) {
                    int threadId = logRecord.getThreadID();
                    Thread thread = null;
                    if (threadId == Thread.currentThread().getId()) {
                        thread = Thread.currentThread();
                    } else {
                        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                            if (e.getKey().getId() == threadId) {
                                thread = e.getKey();
                                break;
                            }
                        }
                    }
                    if (thread != null) {
                        ClassLoader cl = thread.getContextClassLoader();
                        while (cl.getParent() != null) {
                            if (cl == testApplication.getAugmentClassLoader()
                                    || cl == testApplication.getBaseRuntimeClassLoader()) {
                                synchronized (logOutput) {
                                    if (logOutput.isEmpty() || logOutput.get(logOutput.size() - 1) != logRecord) { //this can be called multiple times
                                        logOutput.add(logRecord);
                                    }
                                }
                                return false;
                            }
                            cl = cl.getParent();
                        }
                    }
                    return true;
                }
            });

            final Deque<Set<String>> touchedClasses = new LinkedBlockingDeque<>();
            final AtomicReference<Set<String>> startupClasses = new AtomicReference<>();
            TracingHandler.setTracingHandler(new TracingHandler.TraceListener() {
                @Override
                public void touched(String className) {
                    Set<String> set = touchedClasses.peek();
                    if (set != null) {
                        set.add(className);
                    }
                }

                @Override
                public void quarkusStarting() {
                    startupClasses.set(touchedClasses.peek());
                }
            });

            Map<String, Map<UniqueId, TestResult>> resultsByClass = new HashMap<>();

            launcher.execute(testPlan, new TestExecutionListener() {

                @Override
                public void executionStarted(TestIdentifier testIdentifier) {
                    waitTillResumed();
                    touchedClasses.push(Collections.synchronizedSet(new HashSet<>()));
                }

                @Override
                public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                    //TODO
                    skipped.incrementAndGet();
                }

                @Override
                public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                    if (disabled) {
                        return;
                    }
                    Class<?> testClass = null;
                    String displayName = testIdentifier.getDisplayName();
                    TestSource testSource = testIdentifier.getSource().orElse(null);
                    Set<String> touched = touchedClasses.pop();
                    UniqueId id = UniqueId.parse(testIdentifier.getUniqueId());
                    if (testSource instanceof ClassSource) {
                        testClass = ((ClassSource) testSource).getJavaClass();
                        if (testExecutionResult.getStatus() != TestExecutionResult.Status.ABORTED) {
                            for (Set<String> i : touchedClasses) {
                                //also add the parent touched classes
                                touched.addAll(i);
                            }
                            if (startupClasses.get() != null) {
                                touched.addAll(startupClasses.get());
                            }
                            testClassUsages.updateTestData(testClass.getName(), touched);
                        }
                    } else if (testSource instanceof MethodSource) {
                        testClass = ((MethodSource) testSource).getJavaClass();
                        methodCount.incrementAndGet();
                        displayName = ((MethodSource) testSource).getJavaMethod().toString();

                        if (testExecutionResult.getStatus() != TestExecutionResult.Status.ABORTED) {
                            for (Set<String> i : touchedClasses) {
                                //also add the parent touched classes
                                touched.addAll(i);
                            }
                            if (startupClasses.get() != null) {
                                touched.addAll(startupClasses.get());
                            }
                            testClassUsages.updateTestData(testClass.getName(), id,
                                    touched);
                        }
                        OUT.setStatusString("Running " + methodCount.get() + "/" + toRun
                                + (failures.isEmpty() ? "." : ". " + failures.size() + " failures so far."));
                    }
                    if (testClass != null) {
                        Map<UniqueId, TestResult> results = resultsByClass.computeIfAbsent(testClass.getName(),
                                s -> new HashMap<>());
                        results.put(id,
                                new TestResult(displayName, testClass.getName(), id, testExecutionResult,
                                        new ArrayList<>(logOutput), testIdentifier.isTest(), runId));
                    }
                    logOutput.clear();
                    if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                        Throwable throwable = testExecutionResult.getThrowable().get();
                        if (testClass != null) {
                            //first we cut all the platform stuff out of the stack trace
                            StackTraceElement[] st = throwable.getStackTrace();
                            for (int i = st.length - 1; i >= 0; --i) {
                                StackTraceElement elem = st[i];
                                if (elem.getClassName().equals(testClass.getName())) {
                                    StackTraceElement[] newst = new StackTraceElement[i + 1];
                                    System.arraycopy(st, 0, newst, 0, i + 1);
                                    st = newst;
                                    break;
                                }
                            }

                            //now cut out all the restassured internals
                            //TODO: this should be pluggable
                            for (int i = st.length - 1; i >= 0; --i) {
                                StackTraceElement elem = st[i];
                                if (elem.getClassName().startsWith("io.restassured")) {
                                    StackTraceElement[] newst = new StackTraceElement[st.length - i];
                                    System.arraycopy(st, i, newst, 0, st.length - i);
                                    st = newst;
                                    break;
                                }
                            }
                            throwable.setStackTrace(st);
                        }
                        failures.put(displayName, testExecutionResult);
                    } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
                        skipped.incrementAndGet();
                    }
                }

                @Override
                public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {

                }
            });
            if (disabled) {
                return;
            }
            testState.updateResults(resultsByClass);
            if (classScanResult != null) {
                testState.classesRemoved(classScanResult.getDeletedClassNames());
            }

            OUT.setStatusString("Tests run");
            System.out.print("\r");
            System.out.flush();
            ContinuousTestingLogHandler.setLogHandler(null);
            waitTillResumed();
            List<TestResult> historicFailures = testState.getHistoricFailures(resultsByClass);
            resultHandler.accept(new TestRunResults(runId, classScanResult, classScanResult == null, start,
                    System.currentTimeMillis(), toResultsMap(historicFailures, resultsByClass)));
            if (consoleOutput) {
                if (failures.isEmpty()) {
                    if (historicFailures.isEmpty()) {
                        OUT.setStatusString(
                                " \u001B[32mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                                        + " were skipped. Tests took " + (System.currentTimeMillis() - start)
                                        + "ms. All tests are passing." + "\u001b[0m");
                    } else {
                        OUT.setStatusString(
                                "\u001B[33mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                                        + " were skipped. Tests took " + (System.currentTimeMillis() - start) + "ms. "
                                        + historicFailures.size() + " tests that were not run are still failing,"
                                        + formatFailureSummary(historicFailures) + "\u001b[0m");
                    }
                } else {
                    StringBuilder sb = new StringBuilder(
                            "\u001B[91mTest run failed, " + methodCount.get() + " tests were run, " + failures.size()
                                    + " failed, "
                                    + skipped.get()
                                    + " were skipped. Tests took " + (System.currentTimeMillis() - start) + "ms");
                    for (Map.Entry<String, TestExecutionResult> entry : failures.entrySet()) {
                        log.error(
                                "Test " + entry.getKey() + " failed "
                                        + entry.getValue().getStatus()
                                        + "\n",
                                entry.getValue().getThrowable().get());
                    }
                    if (!historicFailures.isEmpty()) {
                        sb.append("In addition " + historicFailures.size() + " tests that were not re-run are still failing,"
                                + formatFailureSummary(historicFailures));
                    }
                    OUT.setStatusString(sb.toString() + "\u001b[0m");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ContinuousTestingLogHandler.setLogHandler(null);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private Map<String, TestClassResult> toResultsMap(List<TestResult> historicFailures,
            Map<String, Map<UniqueId, TestResult>> resultsByClass) {
        Map<String, TestClassResult> resultMap = new HashMap<>();
        Map<String, List<TestResult>> historicMap = new HashMap<>();
        for (TestResult i : historicFailures) {
            historicMap.computeIfAbsent(i.getTestClass(), s -> new ArrayList<>()).add(i);
        }
        Set<String> classes = new HashSet<>(resultsByClass.keySet());
        classes.addAll(historicMap.keySet());
        for (String clazz : classes) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            List<TestResult> skipped = new ArrayList<>();
            for (TestResult i : Optional.ofNullable(resultsByClass.get(clazz)).orElse(Collections.emptyMap()).values()) {
                if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(i);
                } else if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(i);
                } else {
                    passing.add(i);
                }
            }
            for (TestResult i : Optional.ofNullable(historicMap.get(clazz)).orElse(Collections.emptyList())) {
                if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(i);
                } else if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(i);
                } else {
                    passing.add(i);
                }
            }
            resultMap.put(clazz, new TestClassResult(clazz, passing, failing, skipped));
        }
        return resultMap;
    }

    private String formatFailureSummary(List<TestResult> totalFailures) {
        StringBuilder sb = new StringBuilder();
        if (totalFailures.size() > 3) {
            sb.append(" including: ");
        } else {
            sb.append(" failing tests are: ");
        }
        for (int i = 0; i < Math.min(totalFailures.size(), 3); ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(totalFailures.get(i).getDisplayName());
        }
        if (totalFailures.size() > 3) {
            sb.append(", ...");
        }
        return sb.toString();
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

    private static List<Class<?>> discoverTestClasses(DevModeContext devModeContext) {
        //maven has a lot of rules around this and is configurable
        //for now this is out of scope, we are just going to consider all @QuarkusTest classes
        //we can revisit this later

        //simple class loading
        List<URL> classRoots = new ArrayList<>();
        try {
            for (DevModeContext.ModuleInfo i : devModeContext.getAllModules()) {
                classRoots.add(Paths.get(i.getMain().getClassesPath()).toFile().toURL());
            }
            //we know test is not empty, otherwise we would not be runnning
            classRoots.add(Paths.get(devModeContext.getApplicationRoot().getTest().get().getClassesPath()).toFile().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URLClassLoader ucl = new URLClassLoader(classRoots.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

        //we also only run tests from the current module, which we can also revisit later
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(Paths.get(devModeContext.getApplicationRoot().getTest().get().getClassesPath()))) {
            files.filter(s -> s.getFileName().toString().endsWith(".class")).forEach(s -> {
                try (InputStream in = Files.newInputStream(s)) {
                    indexer.index(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //todo: sort by profile, account for modules
        Index index = indexer.complete();
        List<Class<?>> ret = new ArrayList<>();
        for (AnnotationInstance i : index.getAnnotations(DotName.createSimple("io.quarkus.test.junit.QuarkusTest"))) {
            try {
                ret.add(ucl.loadClass(i.target().asClass().name().toString()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return ret;
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
