package io.quarkus.runner.bootstrap;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * A representation of the different class paths of a Quarkus application.
 *
 */
public class QuarkusClassPath {

    private final List<URL> deploymentClassPath;

    private final List<URL> runtimeImmutableClassPath;

    private final List<URL> runtimeClassPath;

    public QuarkusClassPath(List<URL> deploymentClassPath, List<URL> runtimeImmutableClassPath,
            List<URL> runtimeClassPath) {
        this.deploymentClassPath = deploymentClassPath;
        this.runtimeImmutableClassPath = runtimeImmutableClassPath;
        this.runtimeClassPath = runtimeClassPath;
    }

    /**
     * This contains -deployment artifacts and their required dependencies,
     * less any dependencies that were part of {@link #runtimeImmutableClassPath}
     */
    public List<URL> getDeploymentClassPath() {
        return Collections.unmodifiableList(deploymentClassPath);
    }

    /**
     * This contains the runtime part of Quarkus extensions, and their dependencies.
     */
    public List<URL> getRuntimeImmutableClassPath() {
        return Collections.unmodifiableList(runtimeImmutableClassPath);
    }

    /**
     * This contains any other runtime dependencies that are missing from the immutable
     * dependency list.
     *
     * Note that this may include hot reloadable elements, so care should be taken
     * when constructing a class loader from this list.
     *
     */
    public List<URL> getRuntimeClassPath() {
        return Collections.unmodifiableList(runtimeClassPath);
    }
}
