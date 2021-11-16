package org.jboss.resteasy.reactive.server.processor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Application;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactory;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectiveContextInjectedBeanFactory;
import org.jboss.resteasy.reactive.server.core.startup.RuntimeDeploymentManager;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveContextResolverScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveExceptionMappingScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveFeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResteasyReactiveParamConverterScanner;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

/**
 * Class that hides some complexity of assembling a RESTEasy Reactive application.
 * <p>
 * Quarkus does not use this class directly, as it assembles the application itself.
 */
public class ResteasyReactiveDeploymentManager {
    private static final Logger log = Logger.getLogger(ResteasyReactiveDeploymentManager.class);

    public static ScanStep start(IndexView nonCalculatingIndex) {
        return new ScanStep(nonCalculatingIndex);
    }

    public static class ScanStep {
        final IndexView index;
        int inputBufferSize = 10000;
        /**
         * By default we assume a default produced media type of "text/plain"
         * for String endpoint return types. If this is disabled, the default
         * produced media type will be "[text/plain, *&sol;*]" which is more
         * expensive due to negotiation.
         */
        private boolean singleDefaultProduces;

        /**
         * When one of the quarkus-resteasy-reactive-jackson or quarkus-resteasy-reactive-jsonb extension are active
         * and the result type of an endpoint is an application class or one of {@code Collection}, {@code List}, {@code Set} or
         * {@code Map}, we assume the default return type is "application/json".
         */
        private boolean defaultProduces;

        private Map<DotName, ClassInfo> additionalResources = new HashMap<>();
        private Map<DotName, String> additionalResourcePaths = new HashMap<>();
        private Set<String> excludedClasses = new HashSet<>();
        private final List<MethodScanner> methodScanners = new ArrayList<>();

        public ScanStep(IndexView nonCalculatingIndex) {
            index = JandexUtil.createCalculatingIndex(nonCalculatingIndex);
        }

        public int getInputBufferSize() {
            return inputBufferSize;
        }

        public ScanStep setInputBufferSize(int inputBufferSize) {
            this.inputBufferSize = inputBufferSize;
            return this;
        }

        public boolean isSingleDefaultProduces() {
            return singleDefaultProduces;
        }

        public ScanStep setSingleDefaultProduces(boolean singleDefaultProduces) {
            this.singleDefaultProduces = singleDefaultProduces;
            return this;
        }

        public boolean isDefaultProduces() {
            return defaultProduces;
        }

        public ScanStep setDefaultProduces(boolean defaultProduces) {
            this.defaultProduces = defaultProduces;
            return this;
        }

        public ScanStep addAdditionalResource(DotName className, ClassInfo classInfo) {
            additionalResources.put(className, classInfo);
            return this;
        }

        public ScanStep addAdditionalResourcePath(DotName className, String path) {
            additionalResourcePaths.put(className, path);
            return this;
        }

        public ScanStep addMethodScanner(MethodScanner methodScanner) {
            this.methodScanners.add(methodScanner);
            return this;
        }

        public ScannedApplication scan() {

            ApplicationScanningResult applicationScanningResult = ResteasyReactiveScanner.scanForApplicationClass(index,
                    excludedClasses);
            ResourceScanningResult resources = ResteasyReactiveScanner.scanResources(index, additionalResources,
                    additionalResourcePaths);
            SerializerScanningResult serializerScanningResult = ResteasyReactiveScanner.scanForSerializers(index,
                    applicationScanningResult);

            AdditionalReaders readers = new AdditionalReaders();
            AdditionalWriters writers = new AdditionalWriters();
            ServerEndpointIndexer.Builder builder = new ServerEndpointIndexer.Builder()
                    .setIndex(index)
                    .setScannedResourcePaths(resources.getScannedResourcePaths())
                    .setClassLevelExceptionMappers(new HashMap<>())
                    .setAdditionalReaders(readers)
                    .setAdditionalWriters(writers)
                    .setInjectableBeans(new HashMap<>())
                    .setConfig(new ResteasyReactiveConfig(inputBufferSize, singleDefaultProduces, defaultProduces))
                    .setHttpAnnotationToMethod(resources.getHttpAnnotationToMethod())
                    .setApplicationScanningResult(applicationScanningResult);
            for (MethodScanner scanner : methodScanners) {
                builder.addMethodScanner(scanner);
            }
            ServerEndpointIndexer serverEndpointIndexer = builder
                    .build();

            List<ResourceClass> resourceClasses = new ArrayList<>();
            List<ResourceClass> subResourceClasses = new ArrayList<>();
            for (Map.Entry<DotName, ClassInfo> i : resources.getScannedResources().entrySet()) {
                Optional<ResourceClass> res = serverEndpointIndexer.createEndpoints(i.getValue(), true);
                if (res.isPresent()) {
                    resourceClasses.add(res.get());
                }
            }
            for (Map.Entry<DotName, ClassInfo> i : resources.getPossibleSubResources().entrySet()) {
                Optional<ResourceClass> res = serverEndpointIndexer.createEndpoints(i.getValue(), false);
                if (res.isPresent()) {
                    subResourceClasses.add(res.get());
                }
            }
            return new ScannedApplication(this, readers, writers, serializerScanningResult, applicationScanningResult,
                    resourceClasses, subResourceClasses);
        }

    }

    public static class ScannedApplication {
        final ScanStep scanStep;
        final AdditionalReaders readers;
        final AdditionalWriters writers;
        final SerializerScanningResult serializerScanningResult;
        final ApplicationScanningResult applicationScanningResult;
        final List<ResourceClass> resourceClasses;
        final List<ResourceClass> subResourceClasses;

        ScannedApplication(ScanStep scanStep, AdditionalReaders readers, AdditionalWriters writers,
                SerializerScanningResult serializerScanningResult, ApplicationScanningResult applicationScanningResult,
                List<ResourceClass> resourceClasses, List<ResourceClass> subResourceClasses) {
            this.scanStep = scanStep;
            this.readers = readers;
            this.writers = writers;
            this.serializerScanningResult = serializerScanningResult;
            this.applicationScanningResult = applicationScanningResult;
            this.resourceClasses = resourceClasses;
            this.subResourceClasses = subResourceClasses;
        }

        public PreparedApplication prepare(ClassLoader loader, Function<String, BeanFactory<?>> factoryCreator) {
            Features scannedFeatures = ResteasyReactiveFeatureScanner.createFeatures(scanStep.index, applicationScanningResult,
                    factoryCreator);
            ResourceInterceptors resourceInterceptors = ResteasyReactiveInterceptorScanner
                    .createResourceInterceptors(scanStep.index, applicationScanningResult, factoryCreator);
            DynamicFeatures dynamicFeatures = ResteasyReactiveFeatureScanner.createDynamicFeatures(scanStep.index,
                    applicationScanningResult, factoryCreator);
            ParamConverterProviders paramConverters = ResteasyReactiveParamConverterScanner
                    .createParamConverters(scanStep.index, applicationScanningResult, factoryCreator);
            ExceptionMapping exceptionMappers = ResteasyReactiveExceptionMappingScanner.createExceptionMappers(scanStep.index,
                    applicationScanningResult, factoryCreator);
            ContextResolvers contextResolvers = ResteasyReactiveContextResolverScanner.createContextResolvers(scanStep.index,
                    applicationScanningResult, factoryCreator);
            return new PreparedApplication(loader, scanStep, this, readers, writers, serializerScanningResult, scannedFeatures,
                    resourceInterceptors, dynamicFeatures, paramConverters, exceptionMappers, contextResolvers);
        }
    }

    public static class PreparedApplication {

        final ClassLoader classLoader;
        final ScanStep scanStep;
        final ScannedApplication scannedApplication;
        final AdditionalReaders readers;
        final AdditionalWriters writers;
        final ServerSerialisers serialisers = new ServerSerialisers();
        final SerializerScanningResult serializerScanningResult;
        final Features scannedFeatures;
        final ResourceInterceptors resourceInterceptors;
        final DynamicFeatures dynamicFeatures;
        final ParamConverterProviders paramConverters;
        final ExceptionMapping exceptionMappers;
        final ContextResolvers contextResolvers;

        PreparedApplication(ClassLoader classLoader, ScanStep scanStep, ScannedApplication scannedApplication,
                AdditionalReaders readers, AdditionalWriters writers, SerializerScanningResult serializerScanningResult,
                Features scannedFeatures, ResourceInterceptors resourceInterceptors, DynamicFeatures dynamicFeatures,
                ParamConverterProviders paramConverters, ExceptionMapping exceptionMappers, ContextResolvers contextResolvers) {
            this.classLoader = classLoader;
            this.scanStep = scanStep;
            this.scannedApplication = scannedApplication;
            this.readers = readers;
            this.writers = writers;
            this.serializerScanningResult = serializerScanningResult;
            this.scannedFeatures = scannedFeatures;
            this.resourceInterceptors = resourceInterceptors;
            this.dynamicFeatures = dynamicFeatures;
            this.paramConverters = paramConverters;
            this.exceptionMappers = exceptionMappers;
            this.contextResolvers = contextResolvers;
        }

        public void addScannedSerializers() throws ClassNotFoundException {
            for (var i : serializerScanningResult.getWriters()) {
                serialisers.addWriter(Thread.currentThread().getContextClassLoader().loadClass(i.getHandledClassName()),
                        new ResourceWriter()
                                .setMediaTypeStrings(i.getMediaTypeStrings())
                                .setConstraint(i.getRuntimeType())
                                .setBuiltin(i.isBuiltin())
                                .setPriority(i.getPriority())
                                .setFactory(new ReflectionBeanFactory<>(i.getClassName())));
            }
            for (var i : serializerScanningResult.getReaders()) {
                serialisers.addReader(Thread.currentThread().getContextClassLoader().loadClass(i.getHandledClassName()),
                        new ResourceReader()
                                .setMediaTypeStrings(i.getMediaTypeStrings())
                                .setConstraint(i.getRuntimeType())
                                .setBuiltin(i.isBuiltin())
                                .setPriority(i.getPriority())
                                .setFactory(new ReflectionBeanFactory<>(i.getClassName())));
            }
        }

        public void addBuiltinSerializers() {
            for (var i : writers.get()) {
                serialisers.addWriter(i.getEntityClass(),
                        new ResourceWriter().setFactory(new ReflectiveContextInjectedBeanFactory(i.getHandlerClass()))
                                .setConstraint(i.getConstraint())
                                .setMediaTypeStrings(Collections.singletonList(i.getMediaType())));
            }
            for (var i : readers.get()) {
                serialisers.addReader(i.getEntityClass(),
                        new ResourceReader().setFactory(new ReflectiveContextInjectedBeanFactory(i.getHandlerClass()))
                                .setConstraint(i.getConstraint())
                                .setMediaTypeStrings(Collections.singletonList(i.getMediaType())));
            }
            for (Serialisers.BuiltinReader builtinReader : ServerSerialisers.BUILTIN_READERS) {
                serialisers.addReader(builtinReader.entityClass,
                        new ResourceReader().setFactory(new ReflectiveContextInjectedBeanFactory(builtinReader.readerClass))
                                .setConstraint(builtinReader.constraint)
                                .setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType)).setBuiltin(true));
            }
            for (Serialisers.BuiltinWriter builtinReader : ServerSerialisers.BUILTIN_WRITERS) {
                serialisers.addWriter(builtinReader.entityClass,
                        new ResourceWriter().setFactory(new ReflectiveContextInjectedBeanFactory(builtinReader.writerClass))
                                .setConstraint(builtinReader.constraint)
                                .setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType)).setBuiltin(true));
            }
        }

        public RunnableApplication createApplication(RuntimeConfiguration runtimeConfiguration,
                RequestContextFactory requestContextFactory, Executor executor) {

            DeploymentInfo info = new DeploymentInfo()
                    .setApplicationPath("/")
                    .setConfig(new ResteasyReactiveConfig())
                    .setFeatures(scannedFeatures)
                    .setInterceptors(resourceInterceptors)
                    .setDynamicFeatures(dynamicFeatures)
                    .setParamConverterProviders(paramConverters)
                    .setSerialisers(serialisers)
                    .setExceptionMapping(exceptionMappers)
                    .setResourceClasses(scannedApplication.resourceClasses)
                    .setCtxResolvers(contextResolvers)
                    .setLocatableResourceClasses(scannedApplication.subResourceClasses)
                    .setFactoryCreator(ReflectiveContextInjectedBeanFactory.FACTORY)
                    .setApplicationSupplier(new Supplier<Application>() {
                        @Override
                        public Application get() {
                            //TODO: make pluggable
                            if (scannedApplication.applicationScanningResult.getSelectedAppClass() == null) {
                                return new Application();
                            } else {
                                try {
                                    return (Application) Class
                                            .forName(scannedApplication.applicationScanningResult.getSelectedAppClass().name()
                                                    .toString(), false, classLoader)
                                            .getDeclaredConstructor()
                                            .newInstance();
                                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                                        | NoSuchMethodException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
            List<Closeable> closeTasks = new ArrayList<>();
            String path = getApplicationPath();
            RuntimeDeploymentManager runtimeDeploymentManager = new RuntimeDeploymentManager(info, () -> executor,
                    closeTasks::add, requestContextFactory, ThreadSetupAction.NOOP, path);
            Deployment deployment = runtimeDeploymentManager.deploy();
            deployment.setRuntimeConfiguration(runtimeConfiguration);
            RestInitialHandler initialHandler = new RestInitialHandler(deployment);
            List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = deployment
                    .getRuntimeConfigurableServerRestHandlers();
            for (RuntimeConfigurableServerRestHandler handler : runtimeConfigurableServerRestHandlers) {
                handler.configure(runtimeConfiguration);
            }
            return new RunnableApplication(closeTasks, initialHandler, path);
        }

        private String getApplicationPath() {
            String path = "/";
            if (scannedApplication.applicationScanningResult.getSelectedAppClass() != null) {
                var pathAn = scannedApplication.applicationScanningResult.getSelectedAppClass()
                        .classAnnotation(ResteasyReactiveDotNames.APPLICATION_PATH);
                if (pathAn != null) {
                    path = pathAn.value().asString();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                }
            }
            return path;
        }
    }

    public static class RunnableApplication implements AutoCloseable {
        final List<Closeable> closeTasks;
        final RestInitialHandler initialHandler;
        final String path;

        public RunnableApplication(List<Closeable> closeTasks, RestInitialHandler initialHandler, String path) {
            this.closeTasks = closeTasks;
            this.initialHandler = initialHandler;
            this.path = path;
        }

        @Override
        public void close() {
            for (var task : closeTasks) {
                try {
                    task.close();
                } catch (IOException e) {
                    log.error("Failed to run close task", e);
                }
            }
        }

        public String getPath() {
            return path;
        }

        public RestInitialHandler getInitialHandler() {
            return initialHandler;
        }
    }

}
