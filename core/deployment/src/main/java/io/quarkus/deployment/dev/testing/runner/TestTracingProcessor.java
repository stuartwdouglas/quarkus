package io.quarkus.deployment.dev.testing.runner;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.TestConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.console.QuarkusConsole;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.dev.testing.TracingHandler;

/**
 * processor that instruments test and application classes to trace the code path that is in use during a test run.
 * <p>
 * This allows for fine grained running of tests when a file changes.
 */
public class TestTracingProcessor {

    private static TestConfig.Mode lastEnabledValue;

    @BuildStep(onlyIfNot = IsNormal.class)
    LogCleanupFilterBuildItem handle() {
        return new LogCleanupFilterBuildItem("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator", "0 containers");
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Produce(LogHandlerBuildItem.class)
    ServiceStartBuildItem startTesting(TestConfig config) {
        QuarkusConsole.installConsole(config);
        if (RuntimeUpdatesProcessor.INSTANCE == null || config.enabled == TestConfig.Mode.DISABLED) {
            return null;
        }
        if (config.enabled == TestConfig.Mode.ENABLED) {
            RuntimeUpdatesProcessor.INSTANCE.getTestSupport().start();
        } else if (config.enabled == TestConfig.Mode.PAUSED) {
            RuntimeUpdatesProcessor.INSTANCE.getTestSupport().init();
            RuntimeUpdatesProcessor.INSTANCE.getTestSupport().stop();
        }
        RuntimeUpdatesProcessor.INSTANCE.getTestSupport().setTags(config.includeTags.orElse(Collections.emptyList()),
                config.excludeTags);
        return null;
    }

    @BuildStep(onlyIf = IsTest.class)
    public void instrumentTestClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> transformerProducer) {
        if (!launchModeBuildItem.isAuxiliaryApplication()) {
            return;
        }
        for (ClassInfo clazz : combinedIndexBuildItem.getIndex().getKnownClasses()) {
            String theClassName = clazz.name().toString();
            if (isAppClass(theClassName)) {
                transformerProducer.produce(new BytecodeTransformerBuildItem(false, theClassName,
                        new BiFunction<String, ClassVisitor, ClassVisitor>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                return new ClassVisitor(Opcodes.ASM9, classVisitor) {
                                    @Override
                                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                            String signature, String[] exceptions) {
                                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                        if (name.equals("<init>") || name.equals("<clinit>")) {
                                            return mv;
                                        }
                                        return new MethodVisitor(Opcodes.ASM9, mv) {
                                            @Override
                                            public void visitCode() {
                                                super.visitCode();
                                                visitLdcInsn(theClassName);
                                                visitMethodInsn(Opcodes.INVOKESTATIC,
                                                        TracingHandler.class.getName().replace(".", "/"), "trace",
                                                        "(Ljava/lang/String;)V", false);
                                            }
                                        };
                                    }
                                };
                            }
                        }, true));
            }
        }

    }

    public boolean isAppClass(String theClassName) {
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                .getContextClassLoader();
        //if the class file is present in this (and not the parent) CL then it is an application class
        List<ClassPathElement> res = cl
                .getElementsWithResource(theClassName.replace(".", "/") + ".class", true);
        return !res.isEmpty();
    }
}
