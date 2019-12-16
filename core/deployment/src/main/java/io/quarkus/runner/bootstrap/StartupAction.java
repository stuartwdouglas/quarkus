package io.quarkus.runner.bootstrap;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;

public class StartupAction {

    private static final Logger log = Logger.getLogger(StartupAction.class);

    static final String DEBUG_CLASSES_DIR = System.getProperty("quarkus.debug.generated-classes-dir");

    private final CuratedApplication curatedApplication;
    private final AugmentAction augmentAction;
    private final BuildResult buildResult;

    public StartupAction(CuratedApplication curatedApplication, AugmentAction augmentAction, BuildResult buildResult) {
        this.curatedApplication = curatedApplication;
        this.augmentAction = augmentAction;
        this.buildResult = buildResult;
    }

    /**
     * Runs the application, and returns a handle that can be used to shut it down.
     */
    public RunningQuarkusApplication run(String... args) throws Exception {
        //first
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = extractTransformers();
        QuarkusClassLoader baseClassLoader = curatedApplication.getBaseRuntimeClassLoader();
        ClassLoader transformerClassLoader = buildResult.consume(DeploymentClassLoaderBuildItem.class).getClassLoader();
        baseClassLoader.reset(extractGeneratedResources(false), bytecodeTransformers, transformerClassLoader);
        QuarkusClassLoader runtimeClassLoader = createRuntimeClassLoader(baseClassLoader, bytecodeTransformers,
                transformerClassLoader);

        //we have our class loaders
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(runtimeClassLoader);
            final String className = buildResult.consume(ApplicationClassNameBuildItem.class).getClassName();
            Class<?> appClass;
            try {
                // force init here
                appClass = Class.forName(className, true, runtimeClassLoader);
            } catch (Throwable t) {
                // todo: dev mode expects run time config to be available immediately even if static init didn't complete.
                try {
                    final Class<?> configClass = Class.forName(RunTimeConfigurationGenerator.CONFIG_CLASS_NAME, true,
                            runtimeClassLoader);
                    configClass.getDeclaredMethod(RunTimeConfigurationGenerator.C_CREATE_RUN_TIME_CONFIG.getName())
                            .invoke(null);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }

            Method start = appClass.getMethod("start", String[].class);
            Object application = appClass.newInstance();
            start.invoke(application, (Object) args);
            return new RunningQuarkusApplication((Closeable) application, runtimeClassLoader);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw new RuntimeException("Failed to start Quarkus", e.getCause());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

    }

    private Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> extractTransformers() {
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>();
        List<BytecodeTransformerBuildItem> transformers = buildResult.consumeMulti(BytecodeTransformerBuildItem.class);
        for (BytecodeTransformerBuildItem i : transformers) {
            List<BiFunction<String, ClassVisitor, ClassVisitor>> list = bytecodeTransformers.get(i.getClassToTransform());
            if (list == null) {
                bytecodeTransformers.put(i.getClassToTransform(), list = new ArrayList<>());
            }
            list.add(i.getVisitorFunction());
        }
        return bytecodeTransformers;
    }

    private QuarkusClassLoader createRuntimeClassLoader(QuarkusClassLoader loader,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers,
            ClassLoader deploymentClassLoader) {
        QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Quarkus Runtime ClassLoader",
                loader, false);
        builder.setTransformerClassLoader(deploymentClassLoader);
        builder.addElement(ClassPathElement.fromPath(curatedApplication.getQuarkusBootstrap().getApplicationRoot()));
        builder.addElement(new MemoryClassPathElement(extractGeneratedResources(true)));

        for (AdditionalDependency i : curatedApplication.getQuarkusBootstrap().getAdditionalApplicationArchives()) {
            if (i.isHotReloadable()) {
                builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
            }
        }
        builder.setBytecodeTransformers(bytecodeTransformers);
        return builder.build();
    }

    private Map<String, byte[]> extractGeneratedResources(boolean applicationClasses) {
        Map<String, byte[]> data = new HashMap<>();
        for (GeneratedClassBuildItem i : buildResult.consumeMulti(GeneratedClassBuildItem.class)) {
            if (i.isApplicationClass() == applicationClasses) {
                data.put(i.getName().replace(".", "/") + ".class", i.getClassData());
                if (DEBUG_CLASSES_DIR != null) {
                    try {
                        File debugPath = new File(DEBUG_CLASSES_DIR);
                        if (!debugPath.exists()) {
                            debugPath.mkdir();
                        }
                        File classFile = new File(debugPath, i.getName() + ".class");
                        classFile.getParentFile().mkdirs();
                        try (FileOutputStream classWriter = new FileOutputStream(classFile)) {
                            classWriter.write(i.getClassData());
                        }
                        log.infof("Wrote %s", classFile.getAbsolutePath());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        if (applicationClasses) {
            for (GeneratedResourceBuildItem i : buildResult.consumeMulti(GeneratedResourceBuildItem.class)) {
                data.put(i.getName(), i.getClassData());
            }
        }
        return data;
    }

}
