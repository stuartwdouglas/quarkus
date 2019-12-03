package io.quarkus.runner.bootstrap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.QuarkusAugmentor;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.runner.classloading.ClassPathElement;
import io.quarkus.runner.classloading.QuarkusClassLoader;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;

/**
 * The augmentation task that produces the application.
 */
public class AugmentAction {

    private final QuarkusBootstrap quarkusBootstrap;
    private final AppModel appModel;
    private final AppModelResolver appModelResolver;

    /**
     * A map that is shared between all re-runs of the same augment instance. This is
     * only really relevant in dev mode, however it is present in all modes for consistency.
     * 
     */
    private final Map<Class<?>, Object> reloadContext = new ConcurrentHashMap<>();

    public AugmentAction(QuarkusBootstrap quarkusBootstrap, AppModel appModel, AppModelResolver appModelResolver) {
        this.quarkusBootstrap = quarkusBootstrap;
        this.appModel = appModel;
        this.appModelResolver = appModelResolver;
    }

    public AugmentResult createProductionApplication() {
        if (quarkusBootstrap.getLaunchMode() != LaunchMode.NORMAL) {
            throw new IllegalStateException("Can only create a production application when using NORMAL launch mode");
        }
        BuildResult result = run(true, Collections.emptySet(), ArtifactResultBuildItem.class);
        return new AugmentResult(result.consumeMulti(ArtifactResultBuildItem.class), result.consumeOptional(JarBuildItem.class),
                result.consumeOptional(NativeImageBuildItem.class));
    }

    public StartupAction createInitialRuntimeApplication() {
        if (quarkusBootstrap.getLaunchMode() == LaunchMode.NORMAL) {
            throw new IllegalStateException("Cannot launch a runtime application with NORMAL launch mode");
        }
        BuildResult result = run(true, Collections.emptySet(), GeneratedClassBuildItem.class,
                GeneratedResourceBuildItem.class, BytecodeTransformerBuildItem.class, ApplicationClassNameBuildItem.class);
        return new StartupAction(quarkusBootstrap, this, result);
    }

    public StartupAction reloadExistingApplication(Set<String> changedResources) {
        if (quarkusBootstrap.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("Only application with launch mode DEVELOPMENT can restart");
        }
        BuildResult result = run(false, changedResources, GeneratedClassBuildItem.class,
                GeneratedResourceBuildItem.class, BytecodeTransformerBuildItem.class, ApplicationClassNameBuildItem.class);
        return new StartupAction(quarkusBootstrap, this, result);
    }

    private BuildResult run(boolean firstRun, Set<String> changedResources, Class<? extends BuildItem>... finalOutputs) {
        ProfileManager.setLaunchMode(quarkusBootstrap.getLaunchMode());
        QuarkusClassLoader classLoader = buildAugmentationClassLoader();

        QuarkusAugmentor.Builder builder = QuarkusAugmentor.builder()
                .setRoot(quarkusBootstrap.getApplicationRoot())
                .setClassLoader(classLoader)
                .addFinal(ApplicationClassNameBuildItem.class)
                .setDeploymentClassLoader(buildDeploymentClassLoader(classLoader))
                .setEffectiveModel(appModel)
                .setResolver(appModelResolver);

        builder.setLaunchMode(quarkusBootstrap.getLaunchMode());
        if (firstRun) {
            builder.setLiveReloadState(new LiveReloadBuildItem(false, Collections.emptySet(), reloadContext));
        } else {
            builder.setLiveReloadState(new LiveReloadBuildItem(true, changedResources, reloadContext));
        }
        for (AdditionalDependency i : quarkusBootstrap.getAdditionalApplicationArchives()) {
            //this gets added to the class path either way
            //but we only need to add it to the additional app archives
            //if it is forced as an app archive
            if (i.isForceApplicationArchive()) {
                builder.addAdditionalApplicationArchive(i.getArchivePath());
            }
        }
        builder.excludeFromIndexing(quarkusBootstrap.getExcludeFromClassPath());
        for (Consumer<BuildChainBuilder> i : quarkusBootstrap.getChainCustomizers()) {
            builder.addBuildChainCustomizer(i);
        }
        for (Class<? extends BuildItem> i : finalOutputs) {
            builder.addFinal(i);
        }

        try {
            return builder.build().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QuarkusClassLoader buildAugmentationClassLoader() {
        ClassLoaderState classLoaderState = quarkusBootstrap.getClassLoaderState();
        if (classLoaderState.getAugmentClassLoader() == null) {
            //first run, we need to build all the class loaders
            QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Augmentation Class Loader",
                    ClassLoader.getSystemClassLoader(), true);
            //we want a class loader that can load the deployment artifacts and all their dependencies, but not
            //any of the runtime artifacts, or user classes
            //this will load any deployment artifacts from the parent CL if they are present
            Set<AppArtifact> deploymentArtifacts = new HashSet<>();
            for (AppDependency i : appModel.getFullDeploymentDeps()) {
                deploymentArtifacts.add(i.getArtifact());
                ClassPathElement element = classLoaderState.getElement(i.getArtifact());
                builder.addElement(element);
            }
            //now make sure we can't accidentally load other deps from this CL
            //only extensions and their dependencies.
            for (AppDependency userDep : appModel.getUserDependencies()) {
                if (!deploymentArtifacts.contains(userDep.getArtifact())) {
                    ClassPathElement element = classLoaderState.getElement(userDep.getArtifact());
                    builder.addBannedElement(element);
                }
            }
            QuarkusClassLoader augmentationClassLoader = builder.build();
            classLoaderState.setAugmentClassLoader(augmentationClassLoader);

        }
        return classLoaderState.getAugmentClassLoader();

    }

    private QuarkusClassLoader buildDeploymentClassLoader(ClassLoader augmentClassLoader) {
        ClassLoaderState classLoaderState = quarkusBootstrap.getClassLoaderState();
        //first run, we need to build all the class loaders
        QuarkusClassLoader.Builder builder = QuarkusClassLoader.builder("Deployment Class Loader",
                augmentClassLoader, false);
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
            builder.addElement(classLoaderState.getElement(userDep.getArtifact()));
        }
        return builder.build();
    }

    AppModel getAppModel() {
        return appModel;
    }

    AppModelResolver getAppModelResolver() {
        return appModelResolver;
    }
}
