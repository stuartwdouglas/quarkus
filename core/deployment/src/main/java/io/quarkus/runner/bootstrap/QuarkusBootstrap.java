package io.quarkus.runner.bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.runtime.LaunchMode;

/**
 * The entry point for starting/building a Quarkus application. This class
 * provides a means of starting the Quarkus boot process.
 *
 * All the intial inputs are set in this class, and then the produced phase tasks can be used to
 * control the lifecycle of the build/run.
 */
public class QuarkusBootstrap {

    /**
     * The root of the application, where the application classes live.
     */
    private final Path applicationRoot;

    /**
     * The root of the project. This may be different to the application root for tests that
     * run in a different directory.
     */
    private final Path projectRoot;

    /**
     * Any additional application archives that should be added to the application, that would not be otherwise
     * discovered. The main used case for this is testing to add src/test to the application even if it does
     * not have a beans.xml.
     */
    private final List<AdditionalDependency> additionalApplicationArchives;

    /**
     * Any paths that should never be part of the application. This can be used to exclude the main src/test directory when
     * doing
     * unit testing, to make sure only the generated test archive is picked up.
     */
    private final List<Path> excludeFromClassPath;

    /**
     * A list of consumers that can be used to control the build
     */
    private final List<Consumer<BuildChainBuilder>> chainCustomizers;

    /**
     * The launch mode of this build.
     */
    private final LaunchMode launchMode;

    private final ClassLoaderState classLoaderState = new ClassLoaderState();

    private final Properties buildSystemProperties;

    private QuarkusBootstrap(Builder builder) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.excludeFromClassPath = new ArrayList<>(builder.excludeFromClassPath);
        this.launchMode = builder.launchMode;
        this.chainCustomizers = new ArrayList<>(builder.chainCustomizers);
        this.projectRoot = builder.projectRoot;
        this.buildSystemProperties = builder.buildSystemProperties;
    }

    public CurateAction bootstrap() {
        return new CurateAction(this);
    }

    public Path getApplicationRoot() {
        return applicationRoot;
    }

    public LaunchMode getLaunchMode() {
        return launchMode;
    }

    public List<AdditionalDependency> getAdditionalApplicationArchives() {
        return Collections.unmodifiableList(additionalApplicationArchives);
    }

    public List<Path> getExcludeFromClassPath() {
        return Collections.unmodifiableList(excludeFromClassPath);
    }

    public List<Consumer<BuildChainBuilder>> getChainCustomizers() {
        return chainCustomizers;
    }

    ClassLoaderState getClassLoaderState() {
        return classLoaderState;
    }

    Properties getBuildSystemProperties() {
        return buildSystemProperties;
    }

    Path getProjectRoot() {
        return projectRoot;
    }

    public static Builder builder(Path applicationRoot, LaunchMode launchMode) {
        return new Builder(applicationRoot, launchMode);
    }

    public static class Builder {
        private final Path applicationRoot;
        private Path projectRoot;

        private final List<AdditionalDependency> additionalApplicationArchives = new ArrayList<>();
        private final List<Path> excludeFromClassPath = new ArrayList<>();
        private final LaunchMode launchMode;
        private final List<Consumer<BuildChainBuilder>> chainCustomizers = new ArrayList<>();
        private Properties buildSystemProperties;

        public Builder(Path applicationRoot, LaunchMode launchMode) {
            this.applicationRoot = applicationRoot;
            this.launchMode = launchMode;
        }

        public Builder addAdditionalApplicationArchive(AdditionalDependency path) {
            additionalApplicationArchives.add(path);
            return this;
        }

        public Builder addExcludedPath(Path path) {
            excludeFromClassPath.add(path);
            return this;
        }

        public Builder addBuildChainCustomizer(Consumer<BuildChainBuilder> chainCustomiser) {
            this.chainCustomizers.add(chainCustomiser);
            return this;
        }

        public Builder addBuildChainCustomizers(Collection<Consumer<BuildChainBuilder>> chainCustomizers) {
            this.chainCustomizers.addAll(chainCustomizers);
            return this;
        }

        public Builder setProjectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }
        public void setBuildSystemProperties(Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
        }

        public QuarkusBootstrap build() {
            return new QuarkusBootstrap(this);
        }

    }

}
