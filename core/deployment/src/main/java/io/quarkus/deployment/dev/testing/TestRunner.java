package io.quarkus.deployment.dev.testing;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.dev.testing.ContinuousTestingLogHandler;

public class TestRunner implements Consumer<CuratedApplication> {

    private static final Logger log = Logger.getLogger(TestRunner.class);
    public static volatile CuratedApplication curatedApplication;

    public static void runTests(DevModeContext devModeContext, CuratedApplication testApplication) {
        if (testApplication == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                runInternal(devModeContext, testApplication);
            }
        }, "Test runner thread");
        t.setDaemon(true);
        t.start();
    }

    public static void runInternal(DevModeContext devModeContext, CuratedApplication testApplication) {
        long start = System.currentTimeMillis();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            curatedApplication = testApplication;

            ClassLoader tcl = curatedApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(tcl);
            ((Consumer) tcl.loadClass(TestRunner.class.getName()).newInstance()).accept(curatedApplication);

            List<Class<?>> quarkusTestClasses = discoverTestClasses(devModeContext);

            Launcher launcher = LauncherFactory.create(LauncherConfig.builder().build());
            LauncherDiscoveryRequest request = new LauncherDiscoveryRequestBuilder()
                    .selectors(quarkusTestClasses.stream().map(DiscoverySelectors::selectClass).collect(Collectors.toList()))
                    .build();
            TestPlan testPlan = launcher.discover(request);

            log.info("Starting test run with " + quarkusTestClasses.size() + " test cases");
            final AtomicInteger methodCount = new AtomicInteger();
            final AtomicInteger skipped = new AtomicInteger();
            final Map<String, TestExecutionResult> failures = new HashMap<>();

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
                    if (thread == null) {
                        return true;
                    }
                    ClassLoader cl = thread.getContextClassLoader();
                    while (cl.getParent() != null) {
                        if (cl == testApplication.getAugmentClassLoader()
                                || cl == testApplication.getBaseRuntimeClassLoader()) {
                            return false;
                        }
                        cl = cl.getParent();
                    }
                    return true;
                }
            });

            launcher.execute(testPlan, new TestExecutionListener() {

                @Override
                public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                    skipped.incrementAndGet();
                }

                @Override
                public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                    Class<?> testClass = null;
                    String displayName = testIdentifier.getDisplayName();
                    TestSource testSource = testIdentifier.getSource().orElse(null);
                    if (testSource instanceof ClassSource) {
                        testClass = ((ClassSource) testSource).getJavaClass();
                    } else if (testSource instanceof MethodSource) {
                        testClass = ((MethodSource) testSource).getJavaClass();
                        methodCount.incrementAndGet();
                        displayName = ((MethodSource) testSource).getJavaMethod().toString();
                    }
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
            ContinuousTestingLogHandler.setLogHandler(null);
            if (failures.isEmpty()) {
                log.info("Tests all passed, " + methodCount.get() + " tests were run, " + skipped.get() + " were skipped.");
            } else {
                log.error("Test run failed, " + methodCount.get() + " tests were run, " + failures.size() + " failed.");
                for (Map.Entry<String, TestExecutionResult> entry : failures.entrySet()) {
                    log.error(
                            "Test " + entry.getKey() + " failed "
                                    + entry.getValue().getStatus()
                                    + "\n",
                            entry.getValue().getThrowable().get());

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ContinuousTestingLogHandler.setLogHandler(null);
            Thread.currentThread().setContextClassLoader(old);
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

    @Override
    public void accept(CuratedApplication c) {
        curatedApplication = c; //huge hack, fixme before merge
    }
}
