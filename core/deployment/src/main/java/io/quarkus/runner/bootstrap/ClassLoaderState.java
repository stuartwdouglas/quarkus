package io.quarkus.runner.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.runner.classloading.ClassPathElement;
import io.quarkus.runner.classloading.DirectoryClassPathElement;
import io.quarkus.runner.classloading.JarClassPathElement;
import io.quarkus.runner.classloading.QuarkusClassLoader;

/**
 * A class that tracks the current class loader state for the application.
 *
 */
class ClassLoaderState {

    /**
     * The class path elements for the various artifacts. These can be used in multiple class loaders
     * so this map allows them to be shared.
     *
     * This should not be used for hot reloadable elements
     */
    private final Map<AppArtifact, ClassPathElement> augmentationElements = new HashMap<>();

    /**
     * The augmentation class loader.
     */
    private volatile QuarkusClassLoader augmentClassLoader;

    /**
     * The base runtime class loader.
     */
    private volatile QuarkusClassLoader baseRuntimeClassLoader;

    public synchronized ClassPathElement getElement(AppArtifact artifact) {
        if (augmentationElements.containsKey(artifact)) {
            return augmentationElements.get(artifact);
        }
        Path path = artifact.getPath();
        ClassPathElement element;
        if (Files.isDirectory(path)) {
            element = new DirectoryClassPathElement(path);
        } else {
            element = new JarClassPathElement(path);
        }
        augmentationElements.put(artifact, element);
        return element;
    }

    public QuarkusClassLoader getAugmentClassLoader() {
        return augmentClassLoader;
    }

    public ClassLoaderState setAugmentClassLoader(QuarkusClassLoader augmentClassLoader) {
        if (this.augmentClassLoader != null) {
            throw new IllegalStateException("Augmentation class loader can only be created once");
        }
        this.augmentClassLoader = augmentClassLoader;
        return this;
    }

    public QuarkusClassLoader getBaseRuntimeClassLoader() {
        return baseRuntimeClassLoader;
    }

    public ClassLoaderState setBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader) {
        if (this.augmentClassLoader != null) {
            throw new IllegalStateException("base runtime class loader can only be created once");
        }
        this.baseRuntimeClassLoader = baseRuntimeClassLoader;
        return this;
    }
}
