package io.quarkus.runner.bootstrap;

import java.io.Closeable;

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
}
