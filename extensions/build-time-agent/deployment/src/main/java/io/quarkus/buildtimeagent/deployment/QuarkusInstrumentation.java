package io.quarkus.buildtimeagent.deployment;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarFile;

public class QuarkusInstrumentation implements Instrumentation {

    final List<ClassFileTransformer> transformers = new CopyOnWriteArrayList<>();
    final List<ClassFileTransformer> retransformers = new CopyOnWriteArrayList<>();
    final Set<Class<?>> toRetransform = new CopyOnWriteArraySet<>();

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        transformers.add(transformer);
        if (canRetransform) {
            retransformers.add(transformer);
        }
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        addTransformer(transformer, false);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        retransformers.remove(transformer);
        return transformers.remove(transformer);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return true;
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        toRetransform.addAll(Arrays.asList(classes));
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return false;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {

    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return !theClass.getName().startsWith("java.");
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return new Class[0];
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return new Class[0];
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return 0;
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {

    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {

    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return false;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {

    }

    @Override
    public void redefineModule(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports,
            Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses, Map<Class<?>, List<Class<?>>> extraProvides) {

    }

    @Override
    public boolean isModifiableModule(Module module) {
        return false;
    }
}
