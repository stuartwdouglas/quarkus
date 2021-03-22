package io.quarkus.deployment.dev;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.testing.TestRunner;
import io.quarkus.deployment.dev.testing.TestState;

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
    volatile boolean consoleOutput;

    public TestSupport(CuratedApplication curatedApplication, List<CompilationProvider> compilationProviders,
            DevModeContext context) {
        this.curatedApplication = curatedApplication;
        this.compilationProviders = compilationProviders;
        this.context = context;
    }

    public void start() {
        boolean runTests = false;
        if (!started) {
            synchronized (this) {
                if (!started) {
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
                                testRunner = new TestRunner(context, testCuratedApplication, testState);
                                testRunner.setConsoleOutput(consoleOutput);
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

    public synchronized TestSupport setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
        if (testRunner != null) {
            testRunner.setConsoleOutput(consoleOutput);
        }
        return this;
    }

    public TestState getResults() {
        return testState;
    }
}
