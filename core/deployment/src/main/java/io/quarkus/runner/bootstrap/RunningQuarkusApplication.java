package io.quarkus.runner.bootstrap;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.util.BrokenMpDelegationClassLoader;

public class RunningQuarkusApplication implements AutoCloseable {

    private final Closeable closeTask;
    private final ClassLoader classLoader;

    public RunningQuarkusApplication(Closeable closeTask, ClassLoader classLoader) {
        this.closeTask = closeTask;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        closeTask.close();
    }

    public <T> Optional<T> getConfigValue(String key, Class<T> type) {
        //the config is in an isolated CL
        //we need to extract it via reflection
        //this is pretty yuck, but I don't really see a solution
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> configProviderClass = classLoader.loadClass(ConfigProvider.class.getName());
            Method getConfig = configProviderClass.getMethod("getConfig", ClassLoader.class);
            BrokenMpDelegationClassLoader cl = new BrokenMpDelegationClassLoader(classLoader);
            Thread.currentThread().setContextClassLoader(cl);
            Object config = getConfig.invoke(null, cl);
            return (Optional<T>) getConfig.getReturnType().getMethod("getOptionalValue", String.class, Class.class)
                    .invoke(config, key, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }
}
