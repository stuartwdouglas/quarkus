package io.quarkus.maven;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;

/**
 * Legacy mojo for backwards compatibility reasons. This should not be used in new projects
 *
 * This has been replaced by setting quarkus.package.type=native in the configuration.
 *
 * @deprecated
 */
@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Deprecated
public class NativeImageMojo extends AbstractMojo {

    protected static final String QUARKUS_PACKAGE_TYPE = "quarkus.package.type";
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    public File javaHome;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter
    private Boolean reportErrorsAtRuntime;

    @Parameter(defaultValue = "false")
    private Boolean debugSymbols;

    @Parameter(defaultValue = "${native-image.debug-build-process}")
    private Boolean debugBuildProcess;

    @Parameter(defaultValue = "true")
    private boolean publishDebugBuildProcessPort;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "${native-image.new-server}")
    private Boolean cleanupServer;

    @Parameter
    private Boolean enableHttpUrlHandler;

    @Parameter
    private Boolean enableHttpsUrlHandler;

    @Parameter
    private Boolean enableAllSecurityServices;

    @Parameter
    private Boolean enableIsolates;

    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private String graalvmHome;

    @Parameter(defaultValue = "false")
    private Boolean enableServer;

    @Parameter(defaultValue = "true")
    private Boolean enableJni;

    @Parameter(defaultValue = "false")
    private Boolean autoServiceLoaderRegistration;

    @Parameter(defaultValue = "false")
    private Boolean dumpProxies;

    @Parameter(defaultValue = "${native-image.xmx}")
    private String nativeImageXmx;

    @Parameter(defaultValue = "${native-image.docker-build}")
    private String dockerBuild;

    @Parameter(defaultValue = "${native-image.container-runtime}")
    private String containerRuntime;

    @Parameter(defaultValue = "${native-image.container-runtime-options}")
    private String containerRuntimeOptions;

    @Parameter(defaultValue = "false")
    private Boolean enableVMInspection;

    @Parameter(defaultValue = "true")
    private Boolean fullStackTraces;

    @Deprecated
    @Parameter(defaultValue = "${native-image.disable-reports}")
    private Boolean disableReports;

    @Parameter(defaultValue = "${native-image.enable-reports}")
    private Boolean enableReports;

    @Parameter
    private List<String> additionalBuildArgs;

    @Parameter
    private Boolean addAllCharsets;

    @Parameter
    private Boolean enableFallbackImages;

    @Parameter
    private Boolean reportExceptionStackTraces;

    /**
     * Coordinates of the Maven artifact containing the original Java application to build the native image for.
     * If not provided, the current project is assumed to be the original Java application.
     * <p>
     * The coordinates are expected to be expressed in the following format:
     * <p>
     * groupId:artifactId:classifier:type:version
     * <p>
     * With the classifier, type and version being optional.
     * <p>
     * If the type is missing, the artifact is assumed to be of type JAR.
     * <p>
     * If the version is missing, the artifact is going to be looked up among the project dependencies using the provided
     * coordinates.
     *
     * <p>
     * However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.
     *
     * <p>
     * If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type.
     */
    @Parameter(required = false, property = "quarkus.appArtifact")
    private String appArtifact;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    public NativeImageMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return;
        }

        boolean clear = false;
        try {

            final Properties projectProperties = project.getProperties();
            final Properties realProperties = new Properties();
            for (String name : projectProperties.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    realProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }
            realProperties.putIfAbsent("quarkus.application.name", project.getArtifactId());
            realProperties.putIfAbsent("quarkus.application.version", project.getVersion());

            Consumer<ConfigBuilder> config = createCustomConfig();
            String old = System.getProperty(QUARKUS_PACKAGE_TYPE);
            System.setProperty(QUARKUS_PACKAGE_TYPE, "native");
            try {
                CuratedApplication curatedApplication = QuarkusBootstrap.builder(outputDirectory.toPath())
                        .setProjectRoot(project.getBasedir().toPath())
                        .setBuildSystemProperties(realProperties)
                        .setBaseName(finalName)
                        .setBaseClassLoader(BuildMojo.class.getClassLoader())
                        .setTargetDirectory(buildDir.toPath())
                        .build().bootstrap();

                AugmentAction action = curatedApplication.createAugmentor();
                AugmentResult result = action.createProductionApplication();
                //TODO: config voerrides

            } finally {

                if (old == null) {
                    System.clearProperty(QUARKUS_PACKAGE_TYPE);
                } else {
                    System.setProperty(QUARKUS_PACKAGE_TYPE, old);
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate native image", e);
        }

    }

    //TODO: this needs to be changed to system props.
    private Consumer<ConfigBuilder> createCustomConfig() {
        return new Consumer<ConfigBuilder>() {
            @Override
            public void accept(ConfigBuilder configBuilder) {
                InMemoryConfigSource type = new InMemoryConfigSource(Integer.MAX_VALUE, "Native Image Type")
                        .add("quarkus.package.type", "native");
                configBuilder.withSources(type);

                InMemoryConfigSource configs = new InMemoryConfigSource(0, "Native Image Maven Settings");
                if (addAllCharsets != null) {
                    configs.add("quarkus.native.add-all-charsets", addAllCharsets.toString());
                }
                if (additionalBuildArgs != null && !additionalBuildArgs.isEmpty()) {
                    configs.add("quarkus.native.additional-build-args", additionalBuildArgs);
                }
                if (autoServiceLoaderRegistration != null) {
                    configs.add("quarkus.native.auto-service-loader-registration", autoServiceLoaderRegistration.toString());
                }
                if (cleanupServer != null) {
                    configs.add("quarkus.native.cleanup-server", cleanupServer.toString());
                }
                if (debugBuildProcess != null) {
                    configs.add("quarkus.native.debug-build-process", debugBuildProcess.toString());
                }
                if (debugSymbols != null) {
                    configs.add("quarkus.native.debug-symbols", debugSymbols.toString());
                }
                if (disableReports != null) {
                    configs.add("quarkus.native.enable-reports", Boolean.toString(!disableReports));
                }
                if (enableReports != null) {
                    configs.add("quarkus.native.enable-reports", enableReports.toString());
                }
                if (containerRuntime != null && !containerRuntime.trim().isEmpty()) {
                    configs.add("quarkus.native.container-runtime", containerRuntime);
                } else if (dockerBuild != null && !dockerBuild.trim().isEmpty()) {
                    if (!dockerBuild.toLowerCase().equals("false")) {
                        if (dockerBuild.toLowerCase().equals("true")) {
                            configs.add("quarkus.native.container-runtime", "docker");
                        } else {
                            configs.add("quarkus.native.container-runtime", dockerBuild);
                        }
                    }
                }
                if (containerRuntimeOptions != null && !containerRuntimeOptions.trim().isEmpty()) {
                    configs.add("quarkus.native.container-runtime-options", containerRuntimeOptions);
                }
                if (dumpProxies != null) {
                    configs.add("quarkus.native.dump-proxies", dumpProxies.toString());
                }
                if (enableAllSecurityServices != null) {
                    configs.add("quarkus.native.enable-all-security-services", enableAllSecurityServices.toString());
                }
                if (enableFallbackImages != null) {
                    configs.add("quarkus.native.enable-fallback-images", enableFallbackImages.toString());
                }
                if (enableHttpsUrlHandler != null) {
                    configs.add("quarkus.native.enable-https-url-handler", enableHttpsUrlHandler.toString());
                }
                if (enableHttpUrlHandler != null) {
                    configs.add("quarkus.native.enable-http-url-handler", enableHttpUrlHandler.toString());
                }
                if (enableIsolates != null) {
                    configs.add("quarkus.native.enable-isolates", enableIsolates.toString());
                }
                if (enableJni != null) {
                    configs.add("quarkus.native.enable-jni", enableJni.toString());
                }

                if (enableServer != null) {
                    configs.add("quarkus.native.enable-server", enableServer.toString());
                }

                if (enableVMInspection != null) {
                    configs.add("quarkus.native.enable-vm-inspection", enableVMInspection.toString());
                }
                if (fullStackTraces != null) {
                    configs.add("quarkus.native.full-stack-traces", fullStackTraces.toString());
                }
                if (graalvmHome != null && !graalvmHome.trim().isEmpty()) {
                    configs.add("quarkus.native.graalvm-home", graalvmHome);
                }
                if (javaHome != null && !javaHome.toString().isEmpty()) {
                    configs.add("quarkus.native.java-home", javaHome.toString());
                }
                if (nativeImageXmx != null && !nativeImageXmx.trim().isEmpty()) {
                    configs.add("quarkus.native.native-image-xmx", nativeImageXmx);
                }
                if (reportErrorsAtRuntime != null) {
                    configs.add("quarkus.native.report-errors-at-runtime", reportErrorsAtRuntime.toString());
                }
                if (reportExceptionStackTraces != null) {
                    configs.add("quarkus.native.report-exception-stack-traces", reportExceptionStackTraces.toString());
                }
                if (publishDebugBuildProcessPort) {
                    configs.add("quarkus.native.publish-debug-build-process-port",
                            Boolean.toString(publishDebugBuildProcessPort));
                }
                configBuilder.withSources(configs);

            }
        };

    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }

        public InMemoryConfigSource add(String key, String value) {
            values.put(key, value);
            return this;
        }

        public InMemoryConfigSource add(String key, List<String> value) {
            values.put(key, value.stream()
                    .map(val -> val.replace("\\", "\\\\"))
                    .map(val -> val.replace(",", "\\,"))
                    .collect(joining(",")));
            return this;
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
