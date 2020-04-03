package io.quarkus.bootstrap.classloading;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Terrible (or awesome) hack
 * 
 * If you think you need to use this class you are probably wrong
 * 
 * It is going to be deleted ASAP, basically as soon as we can get proxy bytes out
 * of hibernate directly.
 */
@Deprecated
public class ByteBuddyCapture {

    private static final ThreadLocal<Map<String, byte[]>> CLASSES = new ThreadLocal<>();

    @SuppressWarnings("unused")
    public static void capture(Map<? extends String, byte[]> types) {
        Map<String, byte[]> map = CLASSES.get();
        if (map != null) {
            map.putAll(types);
        }
    }

    public static void beginCapture() {
        CLASSES.set(new HashMap<>());
    }

    public static Map<String, byte[]> endCapture() {
        Map<String, byte[]> d = CLASSES.get();
        CLASSES.remove();
        return d;
    }

    public static Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> transformers() {
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> ret = new HashMap<>();
        List<BiFunction<String, ClassVisitor, ClassVisitor>> transformer = Collections.singletonList(new Transformer());
        ret.put("net.bytebuddy.dynamic.loading.ClassInjector$UsingInstrumentation", transformer);
        ret.put("net.bytebuddy.dynamic.loading.ClassInjector$UsingLookup", transformer);
        ret.put("net.bytebuddy.dynamic.loading.ClassInjector$UsingReflection", transformer);
        ret.put("net.bytebuddy.dynamic.loading.ClassInjector$UsingUnsafe", transformer);
        return ret;
    }

    static class Transformer implements BiFunction<String, ClassVisitor, ClassVisitor> {
        @Override
        public ClassVisitor apply(String s, ClassVisitor classVisitor) {
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, classVisitor) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    final MethodVisitor target = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("injectRaw")) {
                        return new MethodVisitor(Opcodes.ASM7, target) {
                            @Override
                            public void visitCode() {
                                target.visitVarInsn(Opcodes.ALOAD, 1);
                                target.visitMethodInsn(Opcodes.INVOKESTATIC,
                                        ByteBuddyCapture.class.getName().replace(".", "/"), "capture",
                                        "(Ljava/util/Map;)V", false);
                                target.visitCode();
                            }
                        };
                    }
                    return target;
                }
            };
            return cv;
        }
    }

}
