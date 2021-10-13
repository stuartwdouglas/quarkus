package io.quarkus.buildtimeagent.deployment;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo.Gizmo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class BuildTimeAgentProcessor {

    @BuildStep(onlyIf = IsNormal.class)
    BytecodeTransformerBuildItem handleAgents(CurateOutcomeBuildItem curateOutcomeBuildItem, AppModelProviderBuildItem appModelProviderBuildItem) throws Exception {
        System.out.println("Running");
        QuarkusInstrumentation instrumentation = new QuarkusInstrumentation();

        for (var dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            for (var file : dep.getResolvedPaths()) {
                try (JarFile jar = new JarFile(file.toFile(), false)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        var preMain = manifest.getMainAttributes().getValue("Premain-Class");
                        if (preMain != null) {

                            System.out.println("+==================================================================");
                            System.out.println(preMain);
                            Class<?> preMainClazz = Thread.currentThread().getContextClassLoader().loadClass(preMain);

                            var entries = jar.entries();
                            Map<String, byte[]> data = new HashMap<>();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                if (entry.getName().endsWith(".class")) {
                                    ClassReader reader = new ClassReader(jar.getInputStream(entry));
                                    QuarkusClassWriter quarkusClassWriter = new QuarkusClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                    reader.accept(new ClassVisitor(Gizmo.ASM_API_VERSION, quarkusClassWriter) {

                                        @Override
                                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                            return new MethodVisitor(Gizmo.ASM_API_VERSION, super.visitMethod(access, name, descriptor, signature, exceptions)) {

                                                @Override
                                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                                    if ()
                                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                                }
                                            };
                                        }
                                    }, 0);
                                    data.put(entry.getName(), quarkusClassWriter.toByteArray());
                                } else {
                                    data.put(entry.getName(), IoUtil.readBytes(jar.getInputStream(entry)));
                                }
                            }

                            Method preMainMethod = preMainClazz.getMethod("premain", String.class, Instrumentation.class);
                            try {
                                preMainMethod.invoke(null, "", instrumentation);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
        return null;
    }

}
