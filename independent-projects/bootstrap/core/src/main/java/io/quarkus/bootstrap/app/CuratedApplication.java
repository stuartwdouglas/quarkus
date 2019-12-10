package io.quarkus.bootstrap.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.DirectoryClassPathElement;
import io.quarkus.bootstrap.classloading.JarClassPathElement;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;

/**
 * The result of the curate step that is done by QuarkusBootstrap.
 *
 * This is responsible creating all the class loaders used by the application.
 *
 *
 */
public class CuratedApplication {

    private static final String GROUP_ID = "io.quarkus";
    private static final String DEVMODE_SPI_ID = "quarkus-development-mode-spi";

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

    private final QuarkusBootstrap quarkusBootstrap;
    final AppModel appModel;
    final AppModelResolver appModelResolver;

    CuratedApplication(QuarkusBootstrap quarkusBootstrap, AppModel appModel, AppModelResolver appModelResolver) {
        this.quarkusBootstrap = quarkusBootstrap;
        this.appModel = appModel;
        this.appModelResolver = appModelResolver;
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public AppModelResolver getAppModelResolver() {
        return appModelResolver;
    }

    public QuarkusBootstrap getQuarkusBootstrap() {
        return quarkusBootstrap;
    }

    public void runInAugmentClassLoader(String consumerName, Map<String, Object> params) {
        runInCl(consumerName, params, getAugmentClassLoader());
    }

    public void runInBaseRuntimeClassLoader(String consumerName, Map<String, Object> params) {
        runInCl(consumerName, params, getBaseRuntimeClassLoader());
    }

    @SuppressWarnings("unchecked")
    private void runInCl(String consumerName, Map<String, Object> params, QuarkusClassLoader cl) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            Class<? extends BiConsumer<CuratedApplication, Map<String, Object>>> clazz = (Class<? extends BiConsumer<CuratedApplication, Map<String, Object>>>) cl
                    .loadClass(consumerName);
            clazz.newInstance().accept(this, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private synchronized ClassPathElement getElement(AppArtifact artifact) {
        if (!artifact.getType().equals("jar")) {
            //avoid the need for this sort of check in multiple places
            return ClassPathElement.EMPTY;
        }
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

    public synchronized QuarkusClassLoader getAugmentClassLoader() {
        if (augmentClassLoader == null) {
            //first run, we need to build all the class loaders
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Augmentation Class Loader",
                    ClassLoader.getSystemClassLoader(), true);
            //we want a class loader that can load the deployment artifacts and all their dependencies, but not
            //any of the runtime artifacts, or user classes
            //this will load any deployment artifacts from the parent CL if they are present
            Set<AppArtifact> deploymentArtifacts = new HashSet<>();
            for (AppDependency i : appModel.getFullDeploymentDeps()) {
                deploymentArtifacts.add(i.getArtifact());
                ClassPathElement element = getElement(i.getArtifact());

                if (i.getArtifact().getGroupId().equals(GROUP_ID) && i.getArtifact().getArtifactId().equals(DEVMODE_SPI_ID)) {
                    //we always load this from the parent if it is availble, as this acts as a bridge between the running
                    //app and the dev mode code
                    builder.addParentFirstElement(element);
                }
                builder.addElement(element);
            }
            //now make sure we can't accidentally load other deps from this CL
            //only extensions and their dependencies.
            for (AppDependency userDep : appModel.getUserDependencies()) {
                if (!deploymentArtifacts.contains(userDep.getArtifact())) {
                    ClassPathElement element = getElement(userDep.getArtifact());
                    builder.addBannedElement(element);
                }
            }
            augmentClassLoader = builder.build();

        }
        return augmentClassLoader;
    }

    /**
     * creates the base runtime class loader.
     *
     * This does not have any generated resources or transformers, these are added by the startup action.
     *
     * The first thing the startup action needs to do is reset this to include generated resources and transformers,
     * as each startup can generate new resources.
     *
     */
    public synchronized QuarkusClassLoader getBaseRuntimeClassLoader() {
        if (baseRuntimeClassLoader == null) {
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Quarkus Base Runtime ClassLoader",
                    ClassLoader.getSystemClassLoader(), false);
            //additional user class path elements first
            for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
                if (!i.isHotReloadable()) {
                    builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
                }
            }
            builder.setResettableElement(new MemoryClassPathElement(Collections.emptyMap()));

            for (AppDependency dependency : appModel.getUserDependencies()) {

                ClassPathElement element = getElement(dependency.getArtifact());
                if (dependency.getArtifact().getGroupId().equals(GROUP_ID)
                        && dependency.getArtifact().getArtifactId().equals(DEVMODE_SPI_ID)) {
                    //we always load this from the parent if it is availble, as this acts as a bridge between the running
                    //app and the dev mode code
                    builder.addParentFirstElement(element);
                }
                builder.addElement(element);
            }
            baseRuntimeClassLoader = builder.build();
        }
        return baseRuntimeClassLoader;
    }

    public QuarkusClassLoader createDeploymentClassLoader() {
        //first run, we need to build all the class loaders
        QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Deployment Class Loader",
                augmentClassLoader, true);
        //add the application root
        builder.addElement(ClassPathElement.fromPath(quarkusBootstrap.getApplicationRoot()));

        //additional user class path elements first
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            if (!i.isHotReloadable()) {
                builder.addElement(ClassPathElement.fromPath(i.getArchivePath()));
            }
        }
        //now all runtime deps, these will only be used if they are not in the parent
        for (AppDependency userDep : appModel.getUserDependencies()) {
            builder.addElement(getElement(userDep.getArtifact()));
        }
        return builder.build();
    }

    private static final class AppKey {

    }

}
