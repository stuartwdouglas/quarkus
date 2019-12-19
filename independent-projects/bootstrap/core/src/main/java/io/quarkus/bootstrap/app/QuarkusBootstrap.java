package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppModel;

/**
 * The entry point for starting/building a Quarkus application. This class sets up the base class loading
 * architecture. Once this has been established control is passed into the new class loaders
 * to allow for customisation of the boot process.
 *
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

    private final Properties buildSystemProperties;
    private final String baseName;
    private final Path targetDirectory;

    private final Mode mode;
    private final boolean offline;
    private final boolean test;
    private final boolean localProjectDiscovery;
    private final ClassLoader baseClassLoader;

    private QuarkusBootstrap(Builder builder) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.excludeFromClassPath = new ArrayList<>(builder.excludeFromClassPath);
        this.projectRoot = builder.projectRoot != null ? builder.projectRoot.normalize() : null;
        this.buildSystemProperties = builder.buildSystemProperties;
        this.mode = builder.mode;
        this.offline = builder.offline;
        this.test = builder.test;
        this.localProjectDiscovery = builder.localProjectDiscovery;
        this.baseName = builder.baseName;
        this.baseClassLoader = builder.baseClassLoader;
        this.targetDirectory = builder.targetDirectory;
    }

    public CuratedApplication bootstrap() throws BootstrapException {
        //this is super simple, all we want to do is resolve all our dependencies
        //once we have this it is up to augment to set up the class loader to actually use them
        BootstrapAppModelFactory appModelFactory = BootstrapAppModelFactory.newInstance()
                .setOffline(offline)
                .setLocalProjectsDiscovery(localProjectDiscovery)
                .setAppClasses(getProjectRoot() != null ? getProjectRoot()
                        : getApplicationRoot());
        if (mode == Mode.TEST || test) {
            appModelFactory.setTest(true);
        }
        if (mode == Mode.DEV) {
            appModelFactory.setDevMode(true);
        }
        AppModel model = appModelFactory
                .resolveAppModel();
        return new CuratedApplication(this, model, appModelFactory.getAppModelResolver());

    }

    public Path getApplicationRoot() {
        return applicationRoot;
    }

    public List<AdditionalDependency> getAdditionalApplicationArchives() {
        return Collections.unmodifiableList(additionalApplicationArchives);
    }

    public List<Path> getExcludeFromClassPath() {
        return Collections.unmodifiableList(excludeFromClassPath);
    }

    public Properties getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isOffline() {
        return offline;
    }

    public static Builder builder(Path applicationRoot) {
        return new Builder(applicationRoot);
    }

    public String getBaseName() {
        return baseName;
    }

    public ClassLoader getBaseClassLoader() {
        return baseClassLoader;
    }

    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public static class Builder {
        final Path applicationRoot;
        String baseName;
        Path projectRoot;
        ClassLoader baseClassLoader = ClassLoader.getSystemClassLoader();

        final List<AdditionalDependency> additionalApplicationArchives = new ArrayList<>();
        final List<Path> excludeFromClassPath = new ArrayList<>();
        Properties buildSystemProperties;
        Mode mode = Mode.PROD;
        boolean offline;
        boolean test;
        boolean localProjectDiscovery;
        Path targetDirectory;

        public Builder(Path applicationRoot) {
            this.applicationRoot = applicationRoot;
        }

        public Builder addAdditionalApplicationArchive(AdditionalDependency path) {
            additionalApplicationArchives.add(path);
            return this;
        }

        public Builder addExcludedPath(Path path) {
            excludeFromClassPath.add(path);
            return this;
        }

        public Builder setProjectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder setBuildSystemProperties(Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
            return this;
        }

        public Builder setOffline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder setTest(boolean test) {
            this.test = test;
            return this;
        }

        public Builder setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setLocalProjectDiscovery(boolean localProjectDiscovery) {
            this.localProjectDiscovery = localProjectDiscovery;
            return this;
        }

        public Builder setBaseName(String baseName) {
            this.baseName = baseName;
            return this;
        }

        public Builder setBaseClassLoader(ClassLoader baseClassLoader) {
            this.baseClassLoader = baseClassLoader;
            return this;
        }

        public Builder setTargetDirectory(Path targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public QuarkusBootstrap build() {
            return new QuarkusBootstrap(this);
        }

    }

    public enum Mode {
        DEV,
        TEST,
        PROD;
    }
}
