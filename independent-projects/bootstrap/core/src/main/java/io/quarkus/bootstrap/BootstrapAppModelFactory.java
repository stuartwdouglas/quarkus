package io.quarkus.bootstrap;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CurationResult;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.resolver.update.DefaultUpdateDiscovery;
import io.quarkus.bootstrap.resolver.update.DependenciesOrigin;
import io.quarkus.bootstrap.resolver.update.UpdateDiscovery;
import io.quarkus.bootstrap.resolver.update.VersionUpdate;
import io.quarkus.bootstrap.resolver.update.VersionUpdateNumber;
import io.quarkus.bootstrap.util.ZipUtils;

/**
 * The factory that creates the application dependency model.
 *
 * This is used to build the application class loader.
 */
public class BootstrapAppModelFactory {

    private static final Map<String, String> BANNED_DEPENDENCIES = createBannedDependenciesMap();

    public static final String CREATOR_APP_GROUP_ID = "creator.app.groupId";
    public static final String CREATOR_APP_ARTIFACT_ID = "creator.app.artifactId";
    public static final String CREATOR_APP_CLASSIFIER = "creator.app.classifier";
    public static final String CREATOR_APP_TYPE = "creator.app.type";
    public static final String CREATOR_APP_VERSION = "creator.app.version";

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
    private AppModelResolver bootstrapAppModelResolver;

    private VersionUpdateNumber versionUpdateNumber;
    private VersionUpdate versionUpdate;
    private DependenciesOrigin dependenciesOrigin;
    private AppArtifact appArtifact;

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

    public BootstrapAppModelFactory setBootstrapAppModelResolver(AppModelResolver bootstrapAppModelResolver) {
        this.bootstrapAppModelResolver = bootstrapAppModelResolver;
        return this;
    }

    public BootstrapAppModelFactory setVersionUpdateNumber(VersionUpdateNumber versionUpdateNumber) {
        this.versionUpdateNumber = versionUpdateNumber;
        return this;
    }

    public BootstrapAppModelFactory setVersionUpdate(VersionUpdate versionUpdate) {
        this.versionUpdate = versionUpdate;
        return this;
    }

    public BootstrapAppModelFactory setDependenciesOrigin(DependenciesOrigin dependenciesOrigin) {
        this.dependenciesOrigin = dependenciesOrigin;
        return this;
    }

    public BootstrapAppModelFactory setAppArtifact(AppArtifact appArtifact) {
        this.appArtifact = appArtifact;
        return this;
    }

    public AppModelResolver getAppModelResolver() {
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

    public CurationResult resolveAppModel() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }

        if (!Files.isDirectory(appClasses)) {
            return createAppModelForJar(appClasses);
        }
        //        final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
        //                ? LocalProject.loadWorkspace(appClasses)
        //                : LocalProject.load(appClasses);
        final LocalProject localProject = LocalProject.loadWorkspace(appClasses);
        try {
            //TODO: we need some way to cache this for performance reasons
            return new CurationResult(getAppModelResolver()
                    .resolveModel(localProject.getAppArtifact()));
        } catch (Exception e) {
            throw new BootstrapException("Failed to create the application model for " + localProject.getAppArtifact(), e);
        }
    }

    private CurationResult createAppModelForJar(Path appArtifactPath) {
        log.debug("provideOutcome depsOrigin=" + dependenciesOrigin + ", versionUpdate=" + versionUpdate
                + ", versionUpdateNumber="
                + versionUpdateNumber);

        AppArtifact stateArtifact = null;
        boolean loadedFromState = false;
        AppModelResolver modelResolver = getAppModelResolver();
        final AppModel initialDepsList;
        AppArtifact appArtifact = this.appArtifact;
        try {
            if (appArtifact == null) {
                appArtifact = ModelUtils.resolveAppArtifact(appArtifactPath);
            }
            Path appJar;
            try {
                appJar = modelResolver.resolve(appArtifact);
            } catch (AppModelResolverException e) {
                throw new RuntimeException("Failed to resolve artifact", e);
            }
            if (!Files.exists(appJar)) {
                throw new RuntimeException("Application " + appJar + " does not exist on disk");
            }

            modelResolver.relink(appArtifact, appJar);

            if (dependenciesOrigin == DependenciesOrigin.LAST_UPDATE) {
                log.info("Looking for the state of the last update");
                Path statePath = null;
                try {
                    stateArtifact = ModelUtils.getStateArtifact(appArtifact);
                    final String latest = modelResolver.getLatestVersion(stateArtifact, null, false);
                    if (!stateArtifact.getVersion().equals(latest)) {
                        stateArtifact = new AppArtifact(stateArtifact.getGroupId(), stateArtifact.getArtifactId(),
                                stateArtifact.getClassifier(), stateArtifact.getType(), latest);
                    }
                    statePath = modelResolver.resolve(stateArtifact);
                    log.info("- located the state at " + statePath);
                } catch (AppModelResolverException e) {
                    // for now let's assume this means artifact does not exist
                    // System.out.println(" no state found");
                }

                if (statePath != null) {
                    Model model;
                    try {
                        model = ModelUtils.readModel(statePath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read application state " + statePath, e);
                    }
                    /*
                     * final Properties props = model.getProperties(); final String appGroupId =
                     * props.getProperty(CurateOutcome.CREATOR_APP_GROUP_ID); final String appArtifactId =
                     * props.getProperty(CurateOutcome.CREATOR_APP_ARTIFACT_ID); final String appClassifier =
                     * props.getProperty(CurateOutcome.CREATOR_APP_CLASSIFIER); final String appType =
                     * props.getProperty(CurateOutcome.CREATOR_APP_TYPE); final String appVersion =
                     * props.getProperty(CurateOutcome.CREATOR_APP_VERSION); final AppArtifact modelAppArtifact = new
                     * AppArtifact(appGroupId, appArtifactId, appClassifier, appType, appVersion);
                     */
                    final List<Dependency> modelStateDeps = model.getDependencies();
                    final List<AppDependency> updatedDeps = new ArrayList<>(modelStateDeps.size());
                    final String groupIdProp = "${" + CREATOR_APP_GROUP_ID + "}";
                    for (Dependency modelDep : modelStateDeps) {
                        if (modelDep.getGroupId().equals(groupIdProp)) {
                            continue;
                        }
                        updatedDeps.add(new AppDependency(new AppArtifact(modelDep.getGroupId(), modelDep.getArtifactId(),
                                modelDep.getClassifier(), modelDep.getType(), modelDep.getVersion()), modelDep.getScope(),
                                modelDep.isOptional()));
                    }
                    initialDepsList = modelResolver.resolveModel(appArtifact, updatedDeps);
                    loadedFromState = true;
                } else {
                    initialDepsList = modelResolver.resolveModel(appArtifact);
                }
            } else {
                initialDepsList = modelResolver.resolveModel(appArtifact);
            }
        } catch (AppModelResolverException | IOException e) {
            throw new RuntimeException("Failed to resolve initial application dependencies", e);
        }

        log.debug("Checking for potential banned dependencies");
        checkBannedDependencies(initialDepsList);

        if (versionUpdate == VersionUpdate.NONE) {
            return new CurationResult(initialDepsList, Collections.emptyList(), loadedFromState, appArtifact, stateArtifact);
        }

        log.info("Checking for available updates");
        List<AppDependency> appDeps;
        try {
            appDeps = modelResolver.resolveUserDependencies(appArtifact, initialDepsList.getUserDependencies());
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to determine the list of dependencies to update", e);
        }
        final Iterator<AppDependency> depsI = appDeps.iterator();
        while (depsI.hasNext()) {
            final AppArtifact appDep = depsI.next().getArtifact();
            if (!appDep.getType().equals(AppArtifact.TYPE_JAR)) {
                depsI.remove();
                continue;
            }
            final Path path = appDep.getPath();
            if (Files.isDirectory(path)) {
                if (!Files.exists(path.resolve(BootstrapConstants.DESCRIPTOR_PATH))) {
                    depsI.remove();
                }
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    if (!Files.exists(artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH))) {
                        depsI.remove();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open " + path, e);
                }
            }
        }

        final UpdateDiscovery ud = new DefaultUpdateDiscovery(modelResolver, versionUpdateNumber);
        List<AppDependency> availableUpdates = null;
        int i = 0;
        while (i < appDeps.size()) {
            final AppDependency dep = appDeps.get(i++);
            final AppArtifact depArtifact = dep.getArtifact();
            final String updatedVersion = versionUpdate == VersionUpdate.NEXT ? ud.getNextVersion(depArtifact)
                    : ud.getLatestVersion(depArtifact);
            if (updatedVersion == null || depArtifact.getVersion().equals(updatedVersion)) {
                continue;
            }
            log.info(dep.getArtifact() + " -> " + updatedVersion);
            if (availableUpdates == null) {
                availableUpdates = new ArrayList<>();
            }
            availableUpdates.add(new AppDependency(new AppArtifact(depArtifact.getGroupId(), depArtifact.getArtifactId(),
                    depArtifact.getClassifier(), depArtifact.getType(), updatedVersion), dep.getScope()));
        }

        if (availableUpdates != null) {
            try {
                return new CurationResult(modelResolver.resolveModel(appArtifact, availableUpdates), availableUpdates, loadedFromState, appArtifact, stateArtifact);
            } catch (AppModelResolverException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.info("- no updates available");
            return new CurationResult(initialDepsList, Collections.emptyList(), loadedFromState, appArtifact, stateArtifact);
        }
    }

    private static org.apache.maven.model.RepositoryPolicy toMavenRepoPolicy(RepositoryPolicy policy) {
        final org.apache.maven.model.RepositoryPolicy mvnPolicy = new org.apache.maven.model.RepositoryPolicy();
        mvnPolicy.setEnabled(policy.isEnabled());
        mvnPolicy.setChecksumPolicy(policy.getChecksumPolicy());
        mvnPolicy.setUpdatePolicy(policy.getUpdatePolicy());
        return mvnPolicy;
    }

    private static void checkBannedDependencies(AppModel initialDepsList) {
        List<String> detectedBannedDependencies = new ArrayList<>();

        for (AppDependency userDependency : initialDepsList.getUserDependencies()) {
            String ga = userDependency.getArtifact().getGroupId() + ":" + userDependency.getArtifact().getArtifactId();
            if (!"test".equals(userDependency.getScope()) && BANNED_DEPENDENCIES.containsKey(ga)) {
                detectedBannedDependencies.add(ga);
            }
        }

        if (!detectedBannedDependencies.isEmpty()) {
            String warnMessage = detectedBannedDependencies.stream()
                    .sorted()
                    .map(d -> "\t- " + d + " should be replaced by " + BANNED_DEPENDENCIES.get(d))
                    .collect(Collectors.joining("\n"));
            log.warnf(
                    "These dependencies are not recommended:%n" +
                            "%s%n" +
                            "You might end up with two different versions of the same classes or with an artifact you shouldn't have in your classpath.",
                    warnMessage);
        }
    }

    private static Map<String, String> createBannedDependenciesMap() {
        Map<String, String> bannedDependencies = new HashMap<>();

        bannedDependencies.put("org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec",
                "jakarta.annotation:jakarta.annotation-api");
        bannedDependencies.put("org.jboss.spec.javax.annotation:jboss-annotations-api_1.3_spec",
                "jakarta.annotation:jakarta.annotation-api");
        bannedDependencies.put("org.jboss.spec.javax.transaction:jboss-transaction-api_1.2_spec",
                "jakarta.transaction:jakarta.transaction-api");
        bannedDependencies.put("org.jboss.spec.javax.transaction:jboss-transaction-api_1.3_spec",
                "jakarta.transaction:jakarta.transaction-api");
        bannedDependencies.put("org.jboss.spec.javax.servlet:jboss-servlet-api_4.0_spec",
                "jakarta.servlet:jakarta.servlet-api");
        bannedDependencies.put("org.jboss.spec.javax.security.jacc:jboss-jacc-api_1.5_spec",
                "jakarta.security.jacc:jakarta.security.jacc-api");
        bannedDependencies.put("org.jboss.spec.javax.security.auth.message:jboss-jaspi-api_1.1_spec",
                "jakarta.security.auth.message:jakarta.security.auth.message-api");
        bannedDependencies.put("org.jboss.spec.javax.websocket:jboss-websocket-api_1.1_spec",
                "jakarta.websocket:jakarta.websocket-api");
        bannedDependencies.put("org.jboss.spec.javax.interceptor:jboss-interceptors-api_1.2_spec",
                "jakarta.interceptor:jakarta.interceptor-api");

        bannedDependencies.put("javax.activation:activation", "com.sun.activation:jakarta.activation");
        bannedDependencies.put("javax.activation:javax.activation-api", "jakarta.activation:jakarta.activation-api");
        bannedDependencies.put("javax.annotation:javax.annotation-api", "jakarta.annotation:jakarta.annotation-api");
        bannedDependencies.put("javax.enterprise:cdi-api", "jakarta.enterprise:jakarta.enterprise.cdi-api");
        bannedDependencies.put("javax.inject:javax.inject", "jakarta.inject:jakarta.inject-api");
        bannedDependencies.put("javax.json:javax.json-api", "jakarta.json:jakarta.json-api");
        bannedDependencies.put("javax.json.bind:javax.json.bind-api", "jakarta.json.bind:jakarta.json.bind-api");
        bannedDependencies.put("org.glassfish:javax.json", "org.glassfish:jakarta.json");
        bannedDependencies.put("org.glassfish:javax.el", "org.glassfish:jakarta.el");
        bannedDependencies.put("javax.persistence:javax.persistence-api", "jakarta.persistence:jakarta.persistence-api");
        bannedDependencies.put("javax.persistence:persistence-api", "jakarta.persistence:jakarta.persistence-api");
        bannedDependencies.put("javax.security.enterprise:javax.security.enterprise-api", "");
        bannedDependencies.put("javax.servlet:servlet-api", "jakarta.servlet:jakarta.servlet-api");
        bannedDependencies.put("javax.servlet:javax.servlet-api", "jakarta.servlet:jakarta.servlet-api");
        bannedDependencies.put("javax.transaction:jta", "jakarta.transaction:jakarta.transaction-api");
        bannedDependencies.put("javax.transaction:javax.transaction-api", "jakarta.transaction:jakarta.transaction-api");
        bannedDependencies.put("javax.validation:validation-api", "jakarta.validation:jakarta.validation-api");
        bannedDependencies.put("javax.xml.bind:jaxb-api", "org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec");
        bannedDependencies.put("javax.websocket:javax.websocket-api", "jakarta.websocket:jakarta.websocket-api");
        bannedDependencies.put("javax.ws.rs:javax.ws.rs-api", "org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec");

        // for now, we use the JBoss API Spec artifacts for those two as that's what RESTEasy use
        bannedDependencies.put("jakarta.xml.bind:jakarta.xml.bind-api",
                "org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec");
        bannedDependencies.put("jakarta.ws.rs:jakarta.ws.rs-api", "org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec");

        return Collections.unmodifiableMap(bannedDependencies);
    }

    private static void debug(String msg, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(msg, args));
        }
    }
}
