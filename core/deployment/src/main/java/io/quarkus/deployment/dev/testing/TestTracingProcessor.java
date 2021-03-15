package io.quarkus.deployment.dev.testing;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.testing.TracingHandler;

/**
 * processor that instruments test and application classes to trace the code path that is in use during a test run.
 * <p>
 * This allows for fine grained running of tests when a file changes.
 */
public class TestTracingProcessor {

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
