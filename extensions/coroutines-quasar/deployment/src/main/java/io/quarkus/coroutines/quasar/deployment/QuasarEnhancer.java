package io.quarkus.coroutines.quasar.deployment;

import java.io.IOException;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.github.fromage.quasi.fibers.instrument.QuasiInstrumentor;

public class QuasarEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor);
    }

    private class HibernateEnhancingClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private QuasiInstrumentor instrumentor;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, new ClassWriter(0));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            instrumentor = new QuasiInstrumentor();
            instrumentor.setCheck(true);
            instrumentor.setDebug(true);
            instrumentor.setVerbose(true);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = quasiEnhancement(className, inputBytes);
            //            String classPath = className.replace('.', '/');
            //            new File("before/" + classPath).mkdirs();
            //            new File("after/" + classPath).mkdirs();
            //            try {
            //                Files.write(Paths.get("before/" + classPath + ".class"), inputBytes);
            //                Files.write(Paths.get("after/" + classPath + ".class"), transformedBytes);
            //            } catch (IOException e) {
            //                // TODO Auto-generated catch block
            //                e.printStackTrace();
            //            }
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, 0);
        }

        private byte[] quasiEnhancement(final String className, final byte[] originalBytes) {
            byte[] enhanced;
            try {
                enhanced = instrumentor.instrumentClass(Thread.currentThread().getContextClassLoader(), className,
                        originalBytes);
                return enhanced == null ? originalBytes : enhanced;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}