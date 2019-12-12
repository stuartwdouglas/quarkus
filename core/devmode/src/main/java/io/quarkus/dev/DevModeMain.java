package io.quarkus.dev;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.runner.bootstrap.AugmentAction;
import io.quarkus.runner.bootstrap.RunningQuarkusApplication;
import io.quarkus.runner.bootstrap.StartupAction;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.runtime.util.BrokenMpDelegationClassLoader;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain implements Closeable {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private final DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    private static volatile RunningQuarkusApplication runner;
    static volatile Throwable deploymentProblem;
    static volatile Throwable compileProblem;
    static volatile RuntimeUpdatesProcessor runtimeUpdatesProcessor;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;

    public DevModeMain(DevModeContext context) {
        this.context = context;
    }

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();

        try (InputStream devModeCp = DevModeMain.class.getClassLoader().getResourceAsStream(DEV_MODE_CONTEXT)) {
            DevModeContext context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
            new DevModeMain(context).start();

            LockSupport.park();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {

        }
    }

    public void start() throws Exception {
        //propagate system props
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            if (!System.getProperties().containsKey(i.getKey())) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }

        try {

            QuarkusBootstrap.Builder bootstrapBuilder = QuarkusBootstrap.builder(context.getClassesRoots().get(0).toPath())
                    .setMode(QuarkusBootstrap.Mode.DEV);
            bootstrapBuilder.setProjectRoot(new File(".").toPath());
            for (int i = 1; i < context.getClassesRoots().size(); ++i) {
                bootstrapBuilder.addAdditionalApplicationArchive(
                        new AdditionalDependency(context.getClassesRoots().get(i).toPath(), false, false));
            }

            for (DevModeContext.ModuleInfo i : context.getModules()) {
                if (i.getClassesPath() != null) {
                    Path classesPath = Paths.get(i.getClassesPath());
                    bootstrapBuilder.addAdditionalApplicationArchive(new AdditionalDependency(classesPath, true, false));

                }
            }
            Properties buildSystemProperties = new Properties();
            buildSystemProperties.putAll(context.getBuildSystemProperties());
            bootstrapBuilder.setBuildSystemProperties(buildSystemProperties);
            curatedApplication = bootstrapBuilder.setTest(context.isTest()).build().bootstrap();
        } catch (Throwable t) {
            log.error("Quarkus dev mode failed to start in curation phase", t);
            System.exit(1);
        }
        augmentAction = new AugmentAction(curatedApplication, Collections.emptyList());
        runtimeUpdatesProcessor = setupRuntimeCompilation(context);
        if (runtimeUpdatesProcessor != null) {
            runtimeUpdatesProcessor.checkForFileChange();
            runtimeUpdatesProcessor.checkForChangedClasses();
        }
        firstStart();

        //        doStart(false, Collections.emptySet());
        if (deploymentProblem != null || compileProblem != null) {
            if (context.isAbortOnFailedStart()) {
                throw new RuntimeException(deploymentProblem == null ? compileProblem : deploymentProblem);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DevModeMain.class) {
                    if (runner != null) {
                        try {
                            runner.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Quarkus Shutdown Thread"));

    }

    private synchronized void firstStart() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.createInitialRuntimeApplication();
                runner = start.run();
            } catch (Throwable t) {
                deploymentProblem = t;
                if (context.isAbortOnFailedStart()) {
                    log.error("Failed to start quarkus", t);
                } else {
                    //we need to set this here, while we still have the correct TCCL
                    //this is so the config is still valid, and we can read HTTP config from application.properties
                    log.error("Failed to start Quarkus", t);
                    log.info("Attempting to start hot replacement endpoint to recover from previous Quarkus startup failure");
                    if (runtimeUpdatesProcessor != null) {
                        runtimeUpdatesProcessor.startupFailed();
                    }
                }

            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public synchronized void restartApp(Set<String> changedResources) {
        stop();
        Timing.restart();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            //ok, we have resolved all the deps
            try {
                StartupAction start = augmentAction.reloadExistingApplication(changedResources);
                runner = start.run();
            } catch (Throwable t) {
                deploymentProblem = t;
                if (context.isAbortOnFailedStart()) {
                    log.error("Failed to start quarkus", t);
                } else {
                    //we need to set this here, while we still have the correct TCCL
                    //this is so the config is still valid, and we can read HTTP config from application.properties
                    log.error("Failed to start Quarkus", t);
                    log.info("Attempting to start hot replacement endpoint to recover from previous Quarkus startup failure");
                    if (runtimeUpdatesProcessor != null) {
                        runtimeUpdatesProcessor.startupFailed();
                    }
                }

            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context) throws Exception {
        if (!context.getModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            ClassLoaderCompiler compiler;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(),
                        compilationProviders, context);
            } catch (Exception e) {
                log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(context, compiler, this);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class,
                    curatedApplication.getBaseRuntimeClassLoader())) {
                hotReplacementSetups.add(service);
                service.setupHotDeployment(processor);
                processor.addHotReplacementSetup(service);
            }
            return processor;
        }
        return null;
    }

    public void stop() {
        if (runner != null) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runner.getClassLoader());
            try {
                runner.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        QuarkusConfigFactory.setConfig(null);
        BrokenMpDelegationClassLoader.setupBrokenClWorkaround();
        final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
            // just means no config was installed, which is fine
        } finally {
            BrokenMpDelegationClassLoader.teardownBrokenClWorkaround();
        }
        DevModeMain.runner = null;
    }

    public void close() {
        try {
            stop();
        } finally {
            for (HotReplacementSetup i : hotReplacementSetups) {
                i.close();
            }
        }
    }
}
