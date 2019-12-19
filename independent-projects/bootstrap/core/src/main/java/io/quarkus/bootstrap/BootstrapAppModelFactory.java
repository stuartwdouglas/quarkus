package io.quarkus.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

/**
 * The factory that creates the application dependency model.
 *
 * This is used to build the application class loader.
 */
public class BootstrapAppModelFactory {

    private static final String QUARKUS = "quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String DEPLOYMENT_CP = "deployment.cp";

    private static final int CP_CACHE_FORMAT_ID = 1;

    private static final Logger log = Logger.getLogger(BootstrapAppModelFactory.class);

    public static BootstrapAppModelFactory newInstance() {
        return new BootstrapAppModelFactory();
    }

    private Path appClasses;
    private List<Path> appCp = new ArrayList<>(0);
    private boolean localProjectsDiscovery;
    private Boolean offline;
    private boolean enableClasspathCache;
    private boolean test;
    private boolean devMode;

    private BootstrapAppModelResolver bootstrapAppModelResolver;

    private BootstrapAppModelFactory() {
    }

    public BootstrapAppModelFactory setTest(boolean test) {
        this.test = test;
        return this;
    }

    public BootstrapAppModelFactory setDevMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public BootstrapAppModelFactory setAppClasses(Path appClasses) {
        this.appClasses = appClasses;
        return this;
    }

    public BootstrapAppModelFactory addToClassPath(Path path) {
        this.appCp.add(path);
        return this;
    }

    public BootstrapAppModelFactory setLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapAppModelFactory setOffline(Boolean offline) {
        this.offline = offline;
        return this;
    }

    public BootstrapAppModelFactory setEnableClasspathCache(boolean enable) {
        this.enableClasspathCache = enable;
        return this;
    }

    public BootstrapAppModelResolver getAppModelResolver() {
        try {
            if (bootstrapAppModelResolver != null) {
                return bootstrapAppModelResolver;
            }
            if (appClasses == null) {
                throw new IllegalArgumentException("Application classes path has not been set");
            }
            if (!Files.isDirectory(appClasses)) {
                final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder();
                if (offline != null) {
                    mvnBuilder.setOffline(offline);
                }
                final LocalProject localProject = localProjectsDiscovery
                        ? LocalProject.loadWorkspace(Paths.get("").normalize().toAbsolutePath(), false)
                        : null;
                if (localProject != null) {
                    mvnBuilder.setWorkspace(localProject.getWorkspace());
                }
                final MavenArtifactResolver mvn = mvnBuilder.build();

                return bootstrapAppModelResolver = new BootstrapAppModelResolver(mvn)
                        .setTest(test)
                        .setDevMode(devMode);
            }

//        final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
//                ? LocalProject.loadWorkspace(appClasses)
//                : LocalProject.load(appClasses);
            final LocalProject localProject = LocalProject.loadWorkspace(appClasses);

            //TODO: we need some way to cache this for performance reasons
            final MavenArtifactResolver.Builder mvn = MavenArtifactResolver.builder()
                    .setWorkspace(localProject.getWorkspace());
            if (offline != null) {
                mvn.setOffline(offline);
            }
            return bootstrapAppModelResolver = new BootstrapAppModelResolver(mvn.build())
                    .setTest(test)
                    .setDevMode(devMode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create resolver for " + appClasses, e);
        }
    }

    public AppModel resolveAppModel() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }

        if (!Files.isDirectory(appClasses)) {
            final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder();
            if (offline != null) {
                mvnBuilder.setOffline(offline);
            }
            final LocalProject localProject = localProjectsDiscovery
                    ? LocalProject.loadWorkspace(Paths.get("").normalize().toAbsolutePath(), false)
                    : null;
            if (localProject != null) {
                mvnBuilder.setWorkspace(localProject.getWorkspace());
            }
            //TODO: the workspace is being loaded twice, share this with getAppModelResolver, although I am not sure if there will ever be a workspace in this code path
            try {
                final BootstrapAppModelResolver appModelResolver = getAppModelResolver();
                final AppArtifact appArtifact = ModelUtils.resolveAppArtifact(appClasses);
                return appModelResolver
                        .resolveManagedModel(appArtifact, Collections.emptyList(),
                                localProject == null ? null : localProject.getAppArtifact());
            } catch (Exception e) {
                throw new BootstrapException("Failed to resolve deployment dependencies for " + appClasses, e);
            }
        }
//        final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
//                ? LocalProject.loadWorkspace(appClasses)
//                : LocalProject.load(appClasses);
        final LocalProject localProject = LocalProject.loadWorkspace(appClasses);
        try {
            //TODO: we need some way to cache this for performance reasons
            return getAppModelResolver()
                    .resolveModel(localProject.getAppArtifact());
        } catch (Exception e) {
            throw new BootstrapException("Failed to create the application model for " + localProject.getAppArtifact(), e);
        }
    }

    private static void debug(String msg, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(msg, args));
        }
    }
}
