package io.quarkus.arquillian;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.runner.bootstrap.AugmentAction;
import io.quarkus.runner.bootstrap.RunningQuarkusApplication;
import io.quarkus.runner.bootstrap.StartupAction;
import io.quarkus.runtime.util.BrokenMpDelegationClassLoader;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.TestInstantiator;

public class QuarkusDeployableContainer implements DeployableContainer<QuarkusConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(QuarkusDeployableContainer.class);

    @Inject
    @DeploymentScoped
    private InstanceProducer<RunningQuarkusApplication> runningApp;

    @Inject
    @DeploymentScoped
    private InstanceProducer<Path> deploymentLocation;

    @Inject
    @DeploymentScoped
    private InstanceProducer<ClassLoader> appClassloader;

    @Inject
    private Instance<TestClass> testClass;

    static Object testInstance;
    static ClassLoader old;

    @Override
    public Class<QuarkusConfiguration> getConfigurationClass() {
        return QuarkusConfiguration.class;
    }

    @Override
    public void setup(QuarkusConfiguration configuration) {
        // No-op
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        old = Thread.currentThread().getContextClassLoader();
        if (testClass.get() == null) {
            throw new IllegalStateException("Test class not available");
        }
        Class testJavaClass = testClass.get().getJavaClass();

        try {
            // Export the test archive
            Path tmpLocation = Files.createTempDirectory("quarkus-arquillian-test");
            deploymentLocation.set(tmpLocation);
            archive.as(ExplodedExporter.class)
                    .exportExplodedInto(tmpLocation.toFile());
            Path appLocation;
            Set<Path> libraries = new HashSet<>();

            if (archive instanceof WebArchive) {
                // Quarkus does not support the WAR layout and so adapt the layout (similarly to quarkus-war-launcher)
                appLocation = tmpLocation.resolve("app").toAbsolutePath();
                //WEB-INF/lib -> lib/
                if (Files.exists(tmpLocation.resolve("WEB-INF/lib"))) {
                    Files.move(tmpLocation.resolve("WEB-INF/lib"), tmpLocation.resolve("lib"));
                }
                //WEB-INF/classes -> archive/
                if (Files.exists(tmpLocation.resolve("WEB-INF/classes"))) {
                    Files.move(tmpLocation.resolve("WEB-INF/classes"), appLocation);
                } else {
                    Files.createDirectory(appLocation);
                }
                //META-INF -> archive/META-INF/
                if (Files.exists(tmpLocation.resolve("META-INF"))) {
                    if (Files.exists(appLocation.resolve("META-INF"))) {
                        // Target directory not empty.
                        try (Stream<Path> fileTreeElements = Files.walk(tmpLocation.resolve("META-INF"), 2)) {
                            fileTreeElements.forEach(p -> {
                                try {
                                    Files.createFile(p);
                                } catch (FileAlreadyExistsException faee) {
                                    // Do Nothing
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    } else {
                        Files.move(tmpLocation.resolve("META-INF"), appLocation.resolve("META-INF"));
                    }
                }
                //WEB-INF -> archive/WEB-INF
                if (Files.exists(tmpLocation.resolve("WEB-INF"))) {
                    Files.move(tmpLocation.resolve("WEB-INF"), appLocation.resolve("WEB-INF"));
                }
                // Collect all libraries
                if (Files.exists(tmpLocation.resolve("lib"))) {
                    try (Stream<Path> libs = Files.walk(tmpLocation.resolve("lib"), 1)) {
                        libs.forEach((i) -> {
                            if (i.getFileName().toString().endsWith(".jar")) {
                                libraries.add(i);
                            }
                        });
                    }
                }
            } else {
                appLocation = tmpLocation;
            }

            List<Consumer<BuildChainBuilder>> customizers = new ArrayList<>();
            // Test class is a bean
            customizers.add(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(AdditionalBeanBuildItem.unremovableOf(testJavaClass));
                        }
                    }).produces(AdditionalBeanBuildItem.class)
                            .build();
                }
            });

            QuarkusBootstrap.Builder bootstrapBuilder = QuarkusBootstrap.builder(appLocation)
                    .setMode(QuarkusBootstrap.Mode.TEST);
            for (Path i : libraries) {
                bootstrapBuilder.addAdditionalApplicationArchive(new AdditionalDependency(i, false, true));
            }
            bootstrapBuilder.setProjectRoot(PathTestHelper.getTestClassesLocation(testJavaClass));

            CuratedApplication curatedApplication = bootstrapBuilder.build().bootstrap();
            AugmentAction augmentAction = new AugmentAction(curatedApplication, customizers);
            StartupAction app = augmentAction.createInitialRuntimeApplication();
            RunningQuarkusApplication runningQuarkusApplication = app.run();
            appClassloader.set(runningQuarkusApplication.getClassLoader());
            runningApp.set(runningQuarkusApplication);
            Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
            // Instantiate the real test instance
            testInstance = TestInstantiator.instantiateTest(testJavaClass, runningQuarkusApplication.getClassLoader());

        } catch (Throwable t) {
            throw new DeploymentException("Unable to start the runtime runner", t);
        }

        ProtocolMetaData metadata = new ProtocolMetaData();

        try {
            BrokenMpDelegationClassLoader.setupBrokenClWorkaround();
            //TODO: fix this
            //String testUri = TestHTTPResourceManager.getUri();

            System.setProperty("test.url", "http://localhost:8080");
            URI uri = URI.create("http://localhost:8080");
            HTTPContext httpContext = new HTTPContext(uri.getHost(), uri.getPort());
            // This is to work around https://github.com/arquillian/arquillian-core/issues/216
            httpContext.add(new Servlet("dummy", "/"));
            metadata.addContext(httpContext);
        } finally {
            BrokenMpDelegationClassLoader.teardownBrokenClWorkaround();
        }
        return metadata;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        try {
            testInstance = null;
            Path location = deploymentLocation.get();
            if (location != null) {
                try {
                    Files.walkFileTree(location, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    LOGGER.warn("Unable to delete the deployment dir: " + location, e);
                }
            }
            RunningQuarkusApplication runner = runningApp.get();
            if (runner != null) {
                try {
                    runner.close();
                } catch (Exception e) {
                    throw new DeploymentException("Unable to close the runtime runner", e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void start() throws LifecycleException {
        // No-op
    }

    @Override
    public void stop() throws LifecycleException {
        // No-op
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Quarkus");
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

}
