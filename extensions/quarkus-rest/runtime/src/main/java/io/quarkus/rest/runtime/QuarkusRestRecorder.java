package io.quarkus.rest.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.runtime.client.ClientProxies;
import io.quarkus.rest.runtime.core.ArcBeanFactory;
import io.quarkus.rest.runtime.core.ContextResolvers;
import io.quarkus.rest.runtime.core.DynamicFeatures;
import io.quarkus.rest.runtime.core.ExceptionMapping;
import io.quarkus.rest.runtime.core.Features;
import io.quarkus.rest.runtime.core.GenericTypeMapping;
import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.ParamConverterProviders;
import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.core.SingletonBeanFactory;
import io.quarkus.rest.runtime.core.parameters.AsyncResponseExtractor;
import io.quarkus.rest.runtime.core.parameters.BeanParamExtractor;
import io.quarkus.rest.runtime.core.parameters.BodyParamExtractor;
import io.quarkus.rest.runtime.core.parameters.ContextParamExtractor;
import io.quarkus.rest.runtime.core.parameters.CookieParamExtractor;
import io.quarkus.rest.runtime.core.parameters.FormParamExtractor;
import io.quarkus.rest.runtime.core.parameters.HeaderParamExtractor;
import io.quarkus.rest.runtime.core.parameters.MatrixParamExtractor;
import io.quarkus.rest.runtime.core.parameters.NullParamExtractor;
import io.quarkus.rest.runtime.core.parameters.ParameterExtractor;
import io.quarkus.rest.runtime.core.parameters.PathParamExtractor;
import io.quarkus.rest.runtime.core.parameters.QueryParamExtractor;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.RuntimeResolvedConverter;
import io.quarkus.rest.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.rest.runtime.handlers.AbortChainHandler;
import io.quarkus.rest.runtime.handlers.BlockingHandler;
import io.quarkus.rest.runtime.handlers.ClassRoutingHandler;
import io.quarkus.rest.runtime.handlers.CompletionStageResponseHandler;
import io.quarkus.rest.runtime.handlers.ExceptionHandler;
import io.quarkus.rest.runtime.handlers.InputHandler;
import io.quarkus.rest.runtime.handlers.InstanceHandler;
import io.quarkus.rest.runtime.handlers.InterceptorHandler;
import io.quarkus.rest.runtime.handlers.InvocationHandler;
import io.quarkus.rest.runtime.handlers.MediaTypeMapper;
import io.quarkus.rest.runtime.handlers.MultiResponseHandler;
import io.quarkus.rest.runtime.handlers.ParameterHandler;
import io.quarkus.rest.runtime.handlers.PerRequestInstanceHandler;
import io.quarkus.rest.runtime.handlers.QuarkusRestInitialHandler;
import io.quarkus.rest.runtime.handlers.ReadBodyHandler;
import io.quarkus.rest.runtime.handlers.RequestDeserializeHandler;
import io.quarkus.rest.runtime.handlers.ResourceLocatorHandler;
import io.quarkus.rest.runtime.handlers.ResourceRequestFilterHandler;
import io.quarkus.rest.runtime.handlers.ResourceResponseFilterHandler;
import io.quarkus.rest.runtime.handlers.ResponseHandler;
import io.quarkus.rest.runtime.handlers.ResponseWriterHandler;
import io.quarkus.rest.runtime.handlers.ServerRestHandler;
import io.quarkus.rest.runtime.handlers.SseResponseWriterHandler;
import io.quarkus.rest.runtime.handlers.UniResponseHandler;
import io.quarkus.rest.runtime.handlers.VariableProducesHandler;
import io.quarkus.rest.runtime.headers.FixedProducesHandler;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestDynamicFeatureContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestFeatureContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResourceMethod;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.mapping.URITemplate;
import io.quarkus.rest.runtime.model.HasPriority;
import io.quarkus.rest.runtime.model.MethodParameter;
import io.quarkus.rest.runtime.model.ParameterType;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceContextResolver;
import io.quarkus.rest.runtime.model.ResourceDynamicFeature;
import io.quarkus.rest.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.runtime.model.ResourceFeature;
import io.quarkus.rest.runtime.model.ResourceInterceptors;
import io.quarkus.rest.runtime.model.ResourceMethod;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceReaderInterceptor;
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.model.ResourceWriterInterceptor;
import io.quarkus.rest.runtime.spi.BeanFactory;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.quarkus.rest.runtime.util.QuarkusMultivaluedHashMap;
import io.quarkus.rest.runtime.util.RuntimeResourceVisitor;
import io.quarkus.rest.runtime.util.ScoreSystem;
import io.quarkus.rest.runtime.util.ServerMediaType;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuarkusRestRecorder {

    private static final Logger log = Logger.getLogger(QuarkusRestRecorder.class);

    private static final Map<String, Class<?>> primitiveTypes;
    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    private static final LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> EMPTY_INTERCEPTOR_REQUEST_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> EMPTY_INTERCEPTOR_RESPONSE_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> EMPTY_INTERCEPTOR_READER_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> EMPTY_INTERCEPTOR_WRITER_MAP = new LinkedHashMap<>();
    public static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];

    private static volatile QuarkusRestDeployment currentDeployment;

    static {
        Map<String, Class<?>> prims = new HashMap<>();
        prims.put(byte.class.getName(), byte.class);
        prims.put(boolean.class.getName(), boolean.class);
        prims.put(char.class.getName(), char.class);
        prims.put(short.class.getName(), short.class);
        prims.put(int.class.getName(), int.class);
        prims.put(float.class.getName(), float.class);
        prims.put(double.class.getName(), double.class);
        prims.put(long.class.getName(), long.class);
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public static QuarkusRestDeployment getCurrentDeployment() {
        return currentDeployment;
    }

    public <T> BeanFactory<T> factory(String targetClass, BeanContainer beanContainer) {
        return new ArcBeanFactory<>(loadClass(targetClass),
                beanContainer);
    }

    public Supplier<EndpointInvoker> invoker(String baseName) {
        return new Supplier<EndpointInvoker>() {
            @Override
            public EndpointInvoker get() {
                try {
                    return (EndpointInvoker) loadClass(baseName).newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException("Unable to generate endpoint invoker", e);
                }

            }
        };
    }

    public Handler<RoutingContext> handler(ResourceInterceptors interceptors,
            ExceptionMapping exceptionMapping,
            ContextResolvers ctxResolvers, Features features, DynamicFeatures dynamicFeatures, Serialisers serialisers,
            List<ResourceClass> resourceClasses, List<ResourceClass> locatableResourceClasses, BeanContainer beanContainer,
            ShutdownContext shutdownContext, QuarkusRestConfig quarkusRestConfig, HttpBuildTimeConfig vertxConfig,
            String applicationPath, Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations,
            GenericTypeMapping genericTypeMapping,
            ParamConverterProviders paramConverterProviders, BeanFactory<QuarkusRestInitialiser> initClassFactory,
            Class<? extends Application> applicationClass, boolean applicationSingletonClassesEmpty) {

        Supplier<Application> applicationSupplier = handleApplication(applicationClass, applicationSingletonClassesEmpty);

        DynamicEntityWriter dynamicEntityWriter = new DynamicEntityWriter(serialisers);

        QuarkusRestConfiguration quarkusRestConfiguration = configureFeatures(features, interceptors, exceptionMapping,
                beanContainer);
        boolean dynamicFeaturesExist = !dynamicFeatures.getResourceDynamicFeatures().isEmpty();

        Map<ResourceRequestInterceptor, ContainerRequestFilter> globalRequestInterceptorsMap = createContainerRequestFilterInstances(
                interceptors.getGlobalRequestInterceptors(), shutdownContext);

        Map<ResourceResponseInterceptor, ContainerResponseFilter> globalResponseInterceptorsMap = createContainerResponseFilterInstances(
                interceptors.getGlobalResponseInterceptors(), shutdownContext);

        Map<ResourceRequestInterceptor, ContainerRequestFilter> nameRequestInterceptorsMap = createContainerRequestFilterInstances(
                interceptors.getNameRequestInterceptors(), shutdownContext);

        Map<ResourceResponseInterceptor, ContainerResponseFilter> nameResponseInterceptorsMap = createContainerResponseFilterInstances(
                interceptors.getNameResponseInterceptors(), shutdownContext);

        Map<ResourceReaderInterceptor, ReaderInterceptor> globalReaderInterceptorsMap = createReaderInterceptorInstances(
                interceptors.getGlobalResourceReaderInterceptors(), shutdownContext);

        Map<ResourceWriterInterceptor, WriterInterceptor> globalWriterInterceptorsMap = createWriterInterceptorInstances(
                interceptors.getGlobalResourceWriterInterceptors(), shutdownContext);

        Map<ResourceReaderInterceptor, ReaderInterceptor> nameReaderInterceptorsMap = createReaderInterceptorInstances(
                interceptors.getNameResourceReaderInterceptors(), shutdownContext);

        Map<ResourceWriterInterceptor, WriterInterceptor> nameWriterInterceptorsMap = createWriterInterceptorInstances(
                interceptors.getNameResourceWriterInterceptors(), shutdownContext);

        Collection<ContainerResponseFilter> responseFilters = globalResponseInterceptorsMap.values();
        List<ResourceResponseFilterHandler> globalResponseInterceptorHandlers = new ArrayList<>(responseFilters.size());
        for (ContainerResponseFilter responseFilter : responseFilters) {
            globalResponseInterceptorHandlers.add(new ResourceResponseFilterHandler(responseFilter));
        }
        Collection<ContainerRequestFilter> requestFilters = globalRequestInterceptorsMap.values();
        List<ResourceRequestFilterHandler> globalRequestInterceptorHandlers = new ArrayList<>(requestFilters.size());
        for (ContainerRequestFilter requestFilter : requestFilters) {
            globalRequestInterceptorHandlers.add(new ResourceRequestFilterHandler(requestFilter, false));
        }

        InterceptorHandler globalInterceptorHandler = null;
        if (!globalReaderInterceptorsMap.isEmpty() ||
                !globalWriterInterceptorsMap.isEmpty()) {
            WriterInterceptor[] writers = null;
            ReaderInterceptor[] readers = null;
            if (!globalReaderInterceptorsMap.isEmpty()) {
                readers = new ReaderInterceptor[globalReaderInterceptorsMap.size()];
                int idx = 0;
                for (ReaderInterceptor i : globalReaderInterceptorsMap.values()) {
                    readers[idx++] = i;
                }
            }
            if (!globalWriterInterceptorsMap.isEmpty()) {
                writers = new WriterInterceptor[globalWriterInterceptorsMap.size()];
                int idx = 0;
                for (WriterInterceptor i : globalWriterInterceptorsMap.values()) {
                    writers[idx++] = i;
                }
            }
            globalInterceptorHandler = new InterceptorHandler(writers, readers);

        }

        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler();
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        for (ResourceClass clazz : possibleSubResource) {
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (ResourceMethod method : clazz.getMethods()) {
                //TODO: add DynamicFeature for these
                RuntimeResource runtimeResource = buildResourceMethod(serialisers, quarkusRestConfig,
                        globalRequestInterceptorsMap,
                        globalResponseInterceptorsMap,
                        globalRequestInterceptorHandlers, globalResponseInterceptorHandlers,
                        nameRequestInterceptorsMap, nameResponseInterceptorsMap,
                        Collections.emptyMap(), Collections.emptyMap(),
                        globalReaderInterceptorsMap,
                        globalWriterInterceptorsMap,
                        nameReaderInterceptorsMap,
                        nameWriterInterceptorsMap, globalInterceptorHandler, Collections.emptyMap(), Collections.emptyMap(),
                        clazz,
                        resourceLocatorHandler, method,
                        true, classPathTemplate, dynamicEntityWriter, beanContainer, paramConverterProviders);

                buildMethodMapper(templates, method, runtimeResource);
            }
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(templates);
            resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
        }

        //it is possible that multiple resource classes use the same path
        //we use this map to merge them
        Map<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> mappers = new TreeMap<>();

        for (ResourceClass clazz : resourceClasses) {
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = mappers
                    .get(classTemplate);
            if (perClassMappers == null) {
                mappers.put(classTemplate, perClassMappers = new HashMap<>());
            }
            for (ResourceMethod method : clazz.getMethods()) {
                Map<ResourceRequestInterceptor, ContainerRequestFilter> methodSpecificRequestInterceptorsMap = Collections
                        .emptyMap();
                Map<ResourceResponseInterceptor, ContainerResponseFilter> methodSpecificResponseInterceptorsMap = Collections
                        .emptyMap();
                Map<ResourceReaderInterceptor, ReaderInterceptor> methodSpecificReaderInterceptorsMap = Collections
                        .emptyMap();
                Map<ResourceWriterInterceptor, WriterInterceptor> methodSpecificWriterInterceptorsMap = Collections
                        .emptyMap();

                if (dynamicFeaturesExist) {
                    // we'll basically just use this as a way to capture the registering of filters
                    // in the global fields
                    ResourceInterceptors dynamicallyConfiguredInterceptors = new ResourceInterceptors();

                    QuarkusRestResourceMethod quarkusRestResourceMethod = new QuarkusRestResourceMethod(clazz, method); // TODO: look into using LazyMethod
                    QuarkusRestDynamicFeatureContext context = new QuarkusRestDynamicFeatureContext(
                            dynamicallyConfiguredInterceptors, quarkusRestConfiguration, beanContainer);
                    for (ResourceDynamicFeature resourceDynamicFeature : dynamicFeatures.getResourceDynamicFeatures()) {
                        DynamicFeature feature = resourceDynamicFeature.getFactory().createInstance().getInstance();
                        feature.configure(quarkusRestResourceMethod, context);
                    }

                    if (!dynamicallyConfiguredInterceptors.getGlobalRequestInterceptors().isEmpty()) {
                        methodSpecificRequestInterceptorsMap = createContainerRequestFilterInstances(
                                dynamicallyConfiguredInterceptors.getGlobalRequestInterceptors(), shutdownContext);
                    }
                    if (!dynamicallyConfiguredInterceptors.getGlobalResponseInterceptors().isEmpty()) {
                        methodSpecificResponseInterceptorsMap = createContainerResponseFilterInstances(
                                dynamicallyConfiguredInterceptors.getGlobalResponseInterceptors(), shutdownContext);
                    }
                    if (!dynamicallyConfiguredInterceptors.getGlobalResourceReaderInterceptors().isEmpty()) {
                        methodSpecificReaderInterceptorsMap = createReaderInterceptorInstances(
                                dynamicallyConfiguredInterceptors.getGlobalResourceReaderInterceptors(), shutdownContext);
                    }
                    if (!dynamicallyConfiguredInterceptors.getGlobalResourceWriterInterceptors().isEmpty()) {
                        methodSpecificWriterInterceptorsMap = createWriterInterceptorInstances(
                                dynamicallyConfiguredInterceptors.getGlobalResourceWriterInterceptors(), shutdownContext);
                    }
                }

                RuntimeResource runtimeResource = buildResourceMethod(serialisers, quarkusRestConfig,
                        globalRequestInterceptorsMap,
                        globalResponseInterceptorsMap,
                        globalRequestInterceptorHandlers, globalResponseInterceptorHandlers, nameRequestInterceptorsMap,
                        nameResponseInterceptorsMap, methodSpecificRequestInterceptorsMap,
                        methodSpecificResponseInterceptorsMap,
                        globalReaderInterceptorsMap,
                        globalWriterInterceptorsMap,
                        nameReaderInterceptorsMap,
                        nameWriterInterceptorsMap, globalInterceptorHandler,
                        methodSpecificReaderInterceptorsMap, methodSpecificWriterInterceptorsMap,
                        clazz, resourceLocatorHandler, method,
                        false, classTemplate, dynamicEntityWriter, beanContainer, paramConverterProviders);

                buildMethodMapper(perClassMappers, method, runtimeResource);
            }

        }
        List<RequestMapper.RequestPath<QuarkusRestInitialHandler.InitialMatch>> classMappers = new ArrayList<>();
        for (Map.Entry<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> entry : mappers
                .entrySet()) {
            URITemplate classTemplate = entry.getKey();
            int classTemplateNameCount = classTemplate.countPathParamNames();
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = entry
                    .getValue();
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(perClassMappers);
            ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(mappersByMethod, classTemplateNameCount);

            int maxMethodTemplateNameCount = 0;
            for (TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> i : perClassMappers.values()) {
                for (URITemplate j : i.keySet()) {
                    maxMethodTemplateNameCount = Math.max(maxMethodTemplateNameCount, j.countPathParamNames());
                }
            }
            classMappers.add(new RequestMapper.RequestPath<>(true, classTemplate,
                    new QuarkusRestInitialHandler.InitialMatch(new ServerRestHandler[] { classRoutingHandler },
                            maxMethodTemplateNameCount + classTemplateNameCount)));
        }

        List<ServerRestHandler> abortHandlingChain = new ArrayList<>();

        if (globalInterceptorHandler != null) {
            abortHandlingChain.add(globalInterceptorHandler);
        }
        abortHandlingChain.add(new ExceptionHandler());
        if (!interceptors.getGlobalResponseInterceptors().isEmpty()) {
            abortHandlingChain.addAll(globalResponseInterceptorHandlers);
        }
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        // sanitise the prefix for our usage to make it either an empty string, or something which starts with a / and does not
        // end with one
        String prefix = vertxConfig.rootPath;
        if (prefix != null) {
            prefix = sanitizePathPrefix(prefix);
        } else {
            prefix = "";
        }
        if ((applicationPath != null) && !applicationPath.isEmpty()) {
            prefix = prefix + sanitizePathPrefix(applicationPath);
        }
        QuarkusRestDeployment deployment = new QuarkusRestDeployment(exceptionMapping, ctxResolvers, serialisers,
                abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY), dynamicEntityWriter,
                createClientImpls(clientImplementations),
                prefix, genericTypeMapping, paramConverterProviders, quarkusRestConfiguration, applicationSupplier);

        initClassFactory.createInstance().getInstance().init(deployment);

        currentDeployment = deployment;

        //pre matching interceptors are run first
        List<ResourceRequestFilterHandler> preMatchHandlers = null;
        if (!interceptors.getResourcePreMatchRequestInterceptors().isEmpty()) {
            Map<ResourceRequestInterceptor, ContainerRequestFilter> preMatchContainerRequestFilters = createContainerRequestFilterInstances(
                    interceptors.getResourcePreMatchRequestInterceptors(), shutdownContext);
            preMatchHandlers = new ArrayList<>(preMatchContainerRequestFilters.size());
            for (ContainerRequestFilter containerRequestFilter : preMatchContainerRequestFilters.values()) {
                preMatchHandlers.add(new ResourceRequestFilterHandler(containerRequestFilter, true));
            }
        }

        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            NotFoundExceptionMapper.classMappers = classMappers;
            RuntimeResourceVisitor.visitRuntimeResources(classMappers, ScoreSystem.ScoreVisitor);
        }

        return new QuarkusRestInitialHandler(new RequestMapper<>(classMappers), deployment, preMatchHandlers);
    }

    // TODO: don't use reflection to instantiate Application
    private Supplier<Application> handleApplication(final Class<? extends Application> applicationClass,
            final boolean singletonClassesEmpty) {
        Supplier<Application> applicationSupplier;
        if (singletonClassesEmpty) {
            applicationSupplier = new Supplier<Application>() {
                @Override
                public Application get() {
                    try {
                        return applicationClass.getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            try {
                final Application application = applicationClass.getConstructor().newInstance();
                for (Object i : application.getSingletons()) {
                    SingletonBeanFactory.setInstance(i.getClass().getName(), i);
                }
                applicationSupplier = new Supplier<Application>() {
                    @Override
                    public Application get() {
                        return application;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return applicationSupplier;
    }

    private String sanitizePathPrefix(String prefix) {
        prefix = prefix.trim();
        if (prefix.equals("/"))
            prefix = "";
        // add leading slash
        if (!prefix.startsWith("/"))
            prefix = "/" + prefix;
        // remove trailing slash
        if (prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length() - 1);
        return prefix;
    }

    private ClientProxies createClientImpls(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        Map<Class<?>, Function<WebTarget, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<Function<WebTarget, ?>>> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), entry.getValue().getValue());
        }
        return new ClientProxies(map);
    }

    //TODO: this needs plenty more work to support all possible types and provide all information the FeatureContext allows
    private QuarkusRestConfiguration configureFeatures(Features features, ResourceInterceptors interceptors,
            ExceptionMapping exceptionMapping,
            BeanContainer beanContainer) {

        QuarkusRestConfiguration configuration = new QuarkusRestConfiguration(RuntimeType.SERVER);
        if (features.getResourceFeatures().isEmpty()) {
            return configuration;
        }

        QuarkusRestFeatureContext featureContext = new QuarkusRestFeatureContext(interceptors, exceptionMapping,
                configuration, beanContainer);
        List<ResourceFeature> resourceFeatures = features.getResourceFeatures();
        for (ResourceFeature resourceFeature : resourceFeatures) {
            Feature feature = resourceFeature.getFactory().createInstance().getInstance();
            boolean enabled = feature.configure(featureContext);
            if (enabled) {
                configuration.addEnabledFeature(feature);
            }
        }
        if (featureContext.isFiltersNeedSorting()) {
            interceptors.sort();
        }
        return configuration;
    }

    // we need to preserve the order of ResourceRequestInterceptor because they have been sorted according to priorities
    private LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> createContainerRequestFilterInstances(
            List<ResourceRequestInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_REQUEST_MAP;
        }

        LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ContainerRequestFilter>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceRequestInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ContainerRequestFilter> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ContainerRequestFilter containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);

        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    // we need to preserve the order of ResourceResponseInterceptor because they have been sorted according to priorities
    private LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> createContainerResponseFilterInstances(
            List<ResourceResponseInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_RESPONSE_MAP;
        }

        LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ContainerResponseFilter>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceResponseInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ContainerResponseFilter> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ContainerResponseFilter containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);

        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    private LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> createReaderInterceptorInstances(
            List<ResourceReaderInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_READER_MAP;
        }

        LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ReaderInterceptor>> responseBeanInstances = new ArrayList<>(interceptors.size());
        Collections.sort(interceptors);
        for (ResourceReaderInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ReaderInterceptor> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ReaderInterceptor containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);
        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    private LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> createWriterInterceptorInstances(
            List<ResourceWriterInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_WRITER_MAP;
        }
        Collections.sort(interceptors);

        LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<WriterInterceptor>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceWriterInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<WriterInterceptor> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            WriterInterceptor containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);
        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    public void buildMethodMapper(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers,
            ResourceMethod method, RuntimeResource runtimeResource) {
        TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> templateMap = perClassMappers
                .get(method.getHttpMethod());
        if (templateMap == null) {
            perClassMappers.put(method.getHttpMethod(), templateMap = new TreeMap<>());
        }
        List<RequestMapper.RequestPath<RuntimeResource>> list = templateMap.get(runtimeResource.getPath());
        if (list == null) {
            templateMap.put(runtimeResource.getPath(), list = new ArrayList<>());
        }
        list.add(new RequestMapper.RequestPath<>(method.getHttpMethod() == null, runtimeResource.getPath(),
                runtimeResource));
    }

    public Map<String, RequestMapper<RuntimeResource>> buildClassMapper(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers) {
        Map<String, RequestMapper<RuntimeResource>> mappersByMethod = new HashMap<>();
        SortedMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> nullMethod = perClassMappers.get(null);
        if (nullMethod == null) {
            nullMethod = Collections.emptySortedMap();
        }
        for (Map.Entry<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> i : perClassMappers
                .entrySet()) {
            for (Map.Entry<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> nm : nullMethod.entrySet()) {
                TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> templateMap = i.getValue();
                if (!templateMap.containsKey(nm.getKey())) {
                    //resource methods take precedence
                    //just skip sub resource locators for now
                    //may need to be revisited if we want to pass the TCK 100%
                    templateMap.put(nm.getKey(), nm.getValue());
                }
            }
            //now we have all our possible resources
            List<RequestMapper.RequestPath<RuntimeResource>> result = new ArrayList<>();
            for (Map.Entry<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> entry : i.getValue().entrySet()) {
                if (entry.getValue().size() == 1) {
                    //simple case, only one match
                    result.addAll(entry.getValue());
                } else {
                    List<RuntimeResource> resources = new ArrayList<>();
                    for (RequestMapper.RequestPath<RuntimeResource> val : entry.getValue()) {
                        resources.add(val.value);
                    }
                    MediaTypeMapper mapper = new MediaTypeMapper(resources);
                    //now we just create a fake RuntimeResource
                    //we could add another layer of indirection, however this is not a common case
                    //so we don't want to add any extra latency into the common case
                    RuntimeResource fake = new RuntimeResource(i.getKey(), entry.getKey(), null, null, Collections.emptyList(),
                            null, null,
                            new ServerRestHandler[] { mapper }, null, new Class[0], null, false, null, null, null, null);
                    result.add(new RequestMapper.RequestPath<>(false, fake.getPath(), fake));
                }
            }
            mappersByMethod.put(i.getKey(), new RequestMapper<>(result));
        }
        return mappersByMethod;
    }

    public QuarkusRestRecorder() {
        super();
    }

    public RuntimeResource buildResourceMethod(Serialisers serialisers,
            QuarkusRestConfig quarkusRestConfig,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> globalRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> globalResponseInterceptorsMap,
            List<ResourceRequestFilterHandler> globalRequestInterceptorHandlers,
            List<ResourceResponseFilterHandler> globalResponseInterceptorHandlers,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> nameRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> nameResponseInterceptorsMap,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> methodSpecificRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> methodSpecificResponseInterceptorsMap,
            Map<ResourceReaderInterceptor, ReaderInterceptor> globalReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> globalWriterInterceptorsMap,
            Map<ResourceReaderInterceptor, ReaderInterceptor> nameReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> nameWriterInterceptorsMap,
            InterceptorHandler globalInterceptorHandler,
            Map<ResourceReaderInterceptor, ReaderInterceptor> methodSpecificReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> methodSpecificWriterInterceptorsMap, ResourceClass clazz,
            ResourceLocatorHandler resourceLocatorHandler,
            ResourceMethod method, boolean locatableResource, URITemplate classPathTemplate,
            DynamicEntityWriter dynamicEntityWriter, BeanContainer beanContainer,
            ParamConverterProviders paramConverterProviders) {
        URITemplate methodPathTemplate = new URITemplate(method.getPath(), false);
        List<ServerRestHandler> abortHandlingChain = new ArrayList<>();
        MultivaluedMap<ScoreSystem.Category, ScoreSystem.Diagnostic> score = new QuarkusMultivaluedHashMap<>();

        Map<String, Integer> pathParameterIndexes = buildParamIndexMap(classPathTemplate, methodPathTemplate);
        List<ServerRestHandler> handlers = new ArrayList<>();
        List<MediaType> consumesMediaTypes;
        if (method.getConsumes() == null) {
            consumesMediaTypes = Collections.emptyList();
        } else {
            consumesMediaTypes = new ArrayList<>(method.getConsumes().length);
            for (String s : method.getConsumes()) {
                consumesMediaTypes.add(MediaType.valueOf(s));
            }
        }

        //setup reader and writer interceptors first
        setupInterceptorHandler(globalReaderInterceptorsMap, globalWriterInterceptorsMap, nameReaderInterceptorsMap,
                nameWriterInterceptorsMap, globalInterceptorHandler, methodSpecificReaderInterceptorsMap,
                methodSpecificWriterInterceptorsMap, method, handlers);
        //at this point the handler chain only has interceptors
        //which we also want in the abort handler chain
        abortHandlingChain.addAll(handlers);

        setupRequestFilterHandler(globalRequestInterceptorsMap, globalRequestInterceptorHandlers, nameRequestInterceptorsMap,
                methodSpecificRequestInterceptorsMap, method, handlers);

        Class<?>[] parameterTypes = new Class[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; ++i) {
            parameterTypes[i] = loadClass(method.getParameters()[i].declaredType);
        }
        // some parameters need the body to be read
        MethodParameter[] parameters = method.getParameters();
        // body can only be in a parameter
        MethodParameter bodyParameter = null;
        int bodyParameterIndex = -1;
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            if (param.parameterType == ParameterType.BODY) {
                bodyParameter = param;
                bodyParameterIndex = i;
                break;
            }
        }
        // form params can be everywhere (field, beanparam, param)
        if (method.isFormParamRequired()) {
            // read the body as multipart in one go
            handlers.add(new ReadBodyHandler(bodyParameter != null));
        } else if (bodyParameter != null) {
            // allow the body to be read by chunks
            handlers.add(new InputHandler(quarkusRestConfig.inputBufferSize.asLongValue(), EXECUTOR_SUPPLIER));
        }
        // if we need the body, let's deserialise it
        if (bodyParameter != null) {
            handlers.add(new RequestDeserializeHandler(loadClass(bodyParameter.type),
                    consumesMediaTypes.isEmpty() ? null : consumesMediaTypes.get(0), serialisers, bodyParameterIndex));
        }

        // given that we may inject form params in the endpoint we need to make sure we read the body before
        // we create/inject our endpoint
        EndpointInvoker invoker = method.getInvoker().get();
        if (!locatableResource) {
            if (clazz.isPerRequestResource()) {
                handlers.add(new PerRequestInstanceHandler(clazz.getFactory()));
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourcePerRequest);
            } else {
                handlers.add(new InstanceHandler(clazz.getFactory()));
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourceSingleton);
            }
        }

        Class<Object> resourceClass = loadClass(clazz.getClassName());
        LazyMethod lazyMethod = new LazyMethod(method.getName(), resourceClass, parameterTypes);

        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            boolean single = param.isSingle();
            ParameterExtractor extractor = parameterExtractor(pathParameterIndexes, param.parameterType, param.type, param.name,
                    single, beanContainer, param.encoded);
            ParameterConverter converter = null;
            boolean userProviderConvertersExist = !paramConverterProviders.getParamConverterProviders().isEmpty();
            if (param.converter != null) {
                converter = param.converter.get();
                if (userProviderConvertersExist) {
                    Method javaMethod = lazyMethod.getMethod();
                    // Workaround our lack of support for generic params by not doing this init if there are not runtime
                    // param converter providers
                    converter.init(paramConverterProviders, javaMethod.getParameterTypes()[i],
                            javaMethod.getGenericParameterTypes()[i],
                            javaMethod.getParameterAnnotations()[i]);
                    // make sure we give the user provided resolvers the chance to convert
                    converter = new RuntimeResolvedConverter(converter);
                    converter.init(paramConverterProviders, javaMethod.getParameterTypes()[i],
                            javaMethod.getGenericParameterTypes()[i],
                            javaMethod.getParameterAnnotations()[i]);
                }
            }

            handlers.add(new ParameterHandler(i, param.getDefaultValue(), extractor,
                    converter, param.parameterType,
                    param.isObtainedAsCollection()));
        }
        if (method.isBlocking()) {
            handlers.add(new BlockingHandler(EXECUTOR_SUPPLIER));
            score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionBlocking);
        } else {
            score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionNonBlocking);
        }
        handlers.add(new InvocationHandler(invoker, method.isCDIRequestScopeRequired()));

        Type returnType = TypeSignatureParser.parse(method.getReturnType());
        Class<?> rawReturnType = getRawType(returnType);
        Type nonAsyncReturnType = getNonAsyncReturnType(returnType);
        Class<?> rawNonAsyncReturnType = getRawType(nonAsyncReturnType);

        if (CompletionStage.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new CompletionStageResponseHandler());
        } else if (Uni.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new UniResponseHandler());
        } else if (Multi.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new MultiResponseHandler());
        }
        ServerMediaType serverMediaType = null;
        if (method.getProduces() != null && method.getProduces().length > 0) {
            serverMediaType = new ServerMediaType(method.getProduces(), StandardCharsets.UTF_8.name());
        }
        if (method.getHttpMethod() == null) {
            //this is a resource locator method
            handlers.add(resourceLocatorHandler);
        } else if (!Response.class.isAssignableFrom(rawNonAsyncReturnType)) {
            //try and statically determine the media type and response writer
            //we can't do this for all cases, but we can do it for the most common ones
            //in practice this should work for the majority of endpoints
            if (method.getProduces() != null && method.getProduces().length > 0) {
                //the method can only produce a single content type, which is the most common case
                if (method.getProduces().length == 1) {
                    MediaType mediaType = MediaType.valueOf(method.getProduces()[0]);
                    //its a wildcard type, makes it hard to determine statically
                    if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                        handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                    } else if (rawNonAsyncReturnType != Void.class
                            && rawNonAsyncReturnType != void.class) {
                        List<MessageBodyWriter<?>> buildTimeWriters = serialisers.findBuildTimeWriters(rawNonAsyncReturnType,
                                RuntimeType.SERVER, method.getProduces());
                        if (buildTimeWriters == null) {
                            //if this is null this means that the type cannot be resolved at build time
                            //this happens when the method returns a generic type (e.g. Object), so there
                            //are more specific mappers that could be invoked depending on the actual return value
                            handlers.add(new FixedProducesHandler(mediaType, dynamicEntityWriter));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (buildTimeWriters.isEmpty()) {
                            //we could not find any writers that can write a response to this endpoint
                            log.warn("Cannot find any combination of response writers for the method " + clazz.getClassName()
                                    + "#" + method.getName() + "(" + Arrays.toString(method.getParameters()) + ")");
                            handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (buildTimeWriters.size() == 1) {
                            //only a single handler that can handle the response
                            //this is a very common case
                            MessageBodyWriter<?> writer = buildTimeWriters.get(0);
                            handlers.add(new FixedProducesHandler(mediaType, new FixedEntityWriter(
                                    writer, serialisers)));
                            if (writer instanceof QuarkusRestMessageBodyWriter)
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTimeDirect(writer));
                            else
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTime(writer));
                        } else {
                            //multiple writers, we try them in the proper order which had already been created
                            handlers.add(new FixedProducesHandler(mediaType,
                                    new FixedEntityWriterArray(buildTimeWriters.toArray(new MessageBodyWriter[0]),
                                            serialisers)));
                            score.add(ScoreSystem.Category.Writer,
                                    ScoreSystem.Diagnostic.WriterBuildTimeMultiple(buildTimeWriters));
                        }
                    } else {
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterNotRequired);
                    }
                } else {
                    //there are multiple possibilities
                    //we could optimise this more in future
                    handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                    score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                }
            } else {
                score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
            }
        } else {
            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
        }

        //the response filter handlers, they need to be added to both the abort and
        //normal chains. At the moment this only has one handler added to it but
        //in future there will be one per filter
        List<ServerRestHandler> responseFilterHandlers = new ArrayList<>();
        if (method.isSse()) {
            handlers.add(new SseResponseWriterHandler());
        } else {
            handlers.add(new ResponseHandler());

            setupResponseFilterHandler(globalResponseInterceptorsMap, globalResponseInterceptorHandlers,
                    nameResponseInterceptorsMap, methodSpecificResponseInterceptorsMap, method, responseFilterHandlers);
            handlers.addAll(responseFilterHandlers);
            handlers.add(new ResponseWriterHandler(dynamicEntityWriter));
        }
        abortHandlingChain.add(new ExceptionHandler());
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.addAll(responseFilterHandlers);

        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        handlers.add(0, new AbortChainHandler(abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY)));

        RuntimeResource runtimeResource = new RuntimeResource(method.getHttpMethod(), methodPathTemplate,
                classPathTemplate,
                method.getProduces() == null ? null : serverMediaType,
                consumesMediaTypes, invoker,
                clazz.getFactory(), handlers.toArray(EMPTY_REST_HANDLER_ARRAY), method.getName(), parameterTypes,
                nonAsyncReturnType, method.isBlocking(), resourceClass,
                lazyMethod,
                pathParameterIndexes, score);
        return runtimeResource;
    }

    private void setupInterceptorHandler(Map<ResourceReaderInterceptor, ReaderInterceptor> globalReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> globalWriterInterceptorsMap,
            Map<ResourceReaderInterceptor, ReaderInterceptor> nameReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> nameWriterInterceptorsMap,
            InterceptorHandler globalInterceptorHandler,
            Map<ResourceReaderInterceptor, ReaderInterceptor> methodSpecificReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> methodSpecificWriterInterceptorsMap, ResourceMethod method,
            List<ServerRestHandler> handlers) {
        if (method.getNameBindingNames().isEmpty() && methodSpecificReaderInterceptorsMap.isEmpty()
                && methodSpecificWriterInterceptorsMap.isEmpty()) {
            if (globalInterceptorHandler != null) {
                handlers.add(globalInterceptorHandler);
            }
        } else if (nameReaderInterceptorsMap.isEmpty() && nameWriterInterceptorsMap.isEmpty()
                && methodSpecificReaderInterceptorsMap.isEmpty() && methodSpecificWriterInterceptorsMap.isEmpty()) {
            // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
            if (globalInterceptorHandler != null) {
                handlers.add(globalInterceptorHandler);
            }
        } else {
            TreeMap<ResourceReaderInterceptor, ReaderInterceptor> readerInterceptorsToUse = new TreeMap<>(
                    HasPriority.TreeMapComparator.INSTANCE);
            readerInterceptorsToUse.putAll(globalReaderInterceptorsMap);
            readerInterceptorsToUse.putAll(methodSpecificReaderInterceptorsMap);
            for (ResourceReaderInterceptor nameInterceptor : nameReaderInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    readerInterceptorsToUse.put(nameInterceptor, nameReaderInterceptorsMap.get(nameInterceptor));
                }
            }

            TreeMap<ResourceWriterInterceptor, WriterInterceptor> writerInterceptorsToUse = new TreeMap<>(
                    HasPriority.TreeMapComparator.INSTANCE);
            writerInterceptorsToUse.putAll(globalWriterInterceptorsMap);
            writerInterceptorsToUse.putAll(methodSpecificWriterInterceptorsMap);
            for (ResourceWriterInterceptor nameInterceptor : nameWriterInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    writerInterceptorsToUse.put(nameInterceptor, nameWriterInterceptorsMap.get(nameInterceptor));
                }
            }
            WriterInterceptor[] writers = null;
            ReaderInterceptor[] readers = null;
            if (!readerInterceptorsToUse.isEmpty()) {
                readers = new ReaderInterceptor[readerInterceptorsToUse.size()];
                int idx = 0;
                for (ReaderInterceptor i : readerInterceptorsToUse.values()) {
                    readers[idx++] = i;
                }
            }
            if (!writerInterceptorsToUse.isEmpty()) {
                writers = new WriterInterceptor[writerInterceptorsToUse.size()];
                int idx = 0;
                for (WriterInterceptor i : writerInterceptorsToUse.values()) {
                    writers[idx++] = i;
                }
            }
            handlers.add(new InterceptorHandler(writers, readers));
        }
    }

    private void setupRequestFilterHandler(Map<ResourceRequestInterceptor, ContainerRequestFilter> globalRequestInterceptorsMap,
            List<ResourceRequestFilterHandler> globalRequestInterceptorsHandlers,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> nameRequestInterceptorsMap,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> methodSpecificRequestInterceptorsMap, ResourceMethod method,
            List<ServerRestHandler> handlers) {
        // according to the spec, global request filters apply everywhere
        // and named request filters only apply to methods with exactly matching "qualifiers"
        if (method.getNameBindingNames().isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
            if (!globalRequestInterceptorsHandlers.isEmpty()) {
                handlers.addAll(globalRequestInterceptorsHandlers);
            }
        } else if (nameRequestInterceptorsMap.isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
            // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
            if (!globalRequestInterceptorsHandlers.isEmpty()) {
                handlers.addAll(globalRequestInterceptorsHandlers);
            }
        } else {
            // TODO: refactor to use the TreeMap procedure used above for interceptors
            List<ResourceRequestInterceptor> interceptorsToUse = new ArrayList<>(
                    globalRequestInterceptorsMap.size() + nameRequestInterceptorsMap.size()
                            + methodSpecificRequestInterceptorsMap.size());
            interceptorsToUse.addAll(globalRequestInterceptorsMap.keySet());
            interceptorsToUse.addAll(methodSpecificRequestInterceptorsMap.keySet());
            for (ResourceRequestInterceptor nameInterceptor : nameRequestInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    interceptorsToUse.add(nameInterceptor);
                }
            }
            // since we have now mixed global, name and method specific interceptors, we need to sort
            Collections.sort(interceptorsToUse);
            for (ResourceRequestInterceptor interceptor : interceptorsToUse) {
                Map<ResourceRequestInterceptor, ContainerRequestFilter> properMap;
                if (interceptor.getNameBindingNames().isEmpty()) {
                    if (methodSpecificRequestInterceptorsMap.containsKey(interceptor)) {
                        properMap = methodSpecificRequestInterceptorsMap;
                    } else {
                        properMap = globalRequestInterceptorsMap;
                    }
                } else {
                    properMap = nameRequestInterceptorsMap;
                }
                handlers.add(new ResourceRequestFilterHandler(properMap.get(interceptor), false));
            }
        }
    }

    private void setupResponseFilterHandler(
            Map<ResourceResponseInterceptor, ContainerResponseFilter> globalResponseInterceptorsMap,
            List<ResourceResponseFilterHandler> globalResponseInterceptorHandlers,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> nameResponseInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> methodSpecificResponseInterceptorsMap,
            ResourceMethod method, List<ServerRestHandler> responseFilterHandlers) {
        // according to the spec, global request filters apply everywhere
        // and named request filters only apply to methods with exactly matching "qualifiers"
        if (method.getNameBindingNames().isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
            if (!globalResponseInterceptorHandlers.isEmpty()) {
                responseFilterHandlers.addAll(globalResponseInterceptorHandlers);
            }
        } else if (nameResponseInterceptorsMap.isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
            // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
            if (!globalResponseInterceptorHandlers.isEmpty()) {
                responseFilterHandlers.addAll(globalResponseInterceptorHandlers);
            }
        } else {
            List<ResourceResponseInterceptor> interceptorsToUse = new ArrayList<>(
                    globalResponseInterceptorsMap.size() + nameResponseInterceptorsMap.size()
                            + methodSpecificResponseInterceptorsMap.size());
            interceptorsToUse.addAll(globalResponseInterceptorsMap.keySet());
            interceptorsToUse.addAll(methodSpecificResponseInterceptorsMap.keySet());
            for (ResourceResponseInterceptor nameInterceptor : nameResponseInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    interceptorsToUse.add(nameInterceptor);
                }
            }
            // since we have now mixed global, name and method specific interceptors, we need to sort
            Collections.sort(interceptorsToUse);
            for (ResourceResponseInterceptor interceptor : interceptorsToUse) {
                Map<ResourceResponseInterceptor, ContainerResponseFilter> properMap;
                if (interceptor.getNameBindingNames().isEmpty()) {
                    if (methodSpecificResponseInterceptorsMap.containsKey(interceptor)) {
                        properMap = methodSpecificResponseInterceptorsMap;
                    } else {
                        properMap = globalResponseInterceptorsMap;
                    }
                } else {
                    properMap = nameResponseInterceptorsMap;
                }
                responseFilterHandlers.add(new ResourceResponseFilterHandler(properMap.get(interceptor)));
            }
        }
    }

    public ParameterExtractor parameterExtractor(Map<String, Integer> pathParameterIndexes, ParameterType type, String javaType,
            String name,
            boolean single, BeanContainer beanContainer, boolean encoded) {
        ParameterExtractor extractor;
        switch (type) {
            case HEADER:
                return new HeaderParamExtractor(name, single);
            case COOKIE:
                return new CookieParamExtractor(name);
            case FORM:
                return new FormParamExtractor(name, single, encoded);
            case PATH:
                Integer index = pathParameterIndexes.get(name);
                if (index == null) {
                    extractor = new NullParamExtractor();
                } else {
                    extractor = new PathParamExtractor(index, encoded);
                }
                return extractor;
            case CONTEXT:
                return new ContextParamExtractor(javaType);
            case ASYNC_RESPONSE:
                return new AsyncResponseExtractor();
            case QUERY:
                extractor = new QueryParamExtractor(name, single, encoded);
                return extractor;
            case BODY:
                return new BodyParamExtractor();
            case MATRIX:
                extractor = new MatrixParamExtractor(name, single, encoded);
                return extractor;
            case BEAN:
                return new BeanParamExtractor(factory(javaType, beanContainer));
            default:
                return new QueryParamExtractor(name, single, encoded);
        }
    }

    public Map<String, Integer> buildParamIndexMap(URITemplate classPathTemplate, URITemplate methodPathTemplate) {
        Map<String, Integer> pathParameterIndexes = new HashMap<>();
        int pathCount = 0;
        if (classPathTemplate != null) {
            for (URITemplate.TemplateComponent i : classPathTemplate.components) {
                if (i.name != null) {
                    pathParameterIndexes.put(i.name, pathCount++);
                } else if (i.names != null) {
                    for (String nm : i.names) {
                        pathParameterIndexes.put(nm, pathCount++);
                    }
                }
            }
        }
        for (URITemplate.TemplateComponent i : methodPathTemplate.components) {
            if (i.name != null) {
                pathParameterIndexes.put(i.name, pathCount++);
            } else if (i.names != null) {
                for (String nm : i.names) {
                    pathParameterIndexes.put(nm, pathCount++);
                }
            }
        }
        return pathParameterIndexes;
    }

    private Class<?> getRawType(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            return (Class<?>) ptype.getRawType();
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + type);
    }

    private Type getNonAsyncReturnType(Type returnType) {
        if (returnType instanceof Class)
            return returnType;
        if (returnType instanceof ParameterizedType) {
            // NOTE: same code in EndpointIndexer.getNonAsyncReturnType
            ParameterizedType type = (ParameterizedType) returnType;
            if (type.getRawType() == CompletionStage.class) {
                return type.getActualTypeArguments()[0];
            }
            if (type.getRawType() == Uni.class) {
                return type.getActualTypeArguments()[0];
            }
            if (type.getRawType() == Multi.class) {
                return type.getActualTypeArguments()[0];
            }
            return returnType;
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + returnType);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> loadClass(String name) {
        if (primitiveTypes.containsKey(name)) {
            return (Class<T>) primitiveTypes.get(name);
        }
        try {
            return (Class<T>) Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerExceptionMapper(ExceptionMapping exceptionMapping, String string,
            ResourceExceptionMapper<Throwable> mapper) {
        exceptionMapping.addExceptionMapper(loadClass(string), mapper);
    }

    public void registerContextResolver(ContextResolvers contextResolvers, String string,
            ResourceContextResolver resolver) {
        contextResolvers.addContextResolver(loadClass(string), resolver);
    }

    public void registerFeature(Features features, ResourceFeature feature) {
        features.addFeature(feature);
    }

    public void registerDynamicFeature(DynamicFeatures dynamicFeatures, ResourceDynamicFeature dynamicFeature) {
        dynamicFeatures.addFeature(dynamicFeature);
    }

    public void registerWriter(Serialisers serialisers, String entityClassName,
            ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }

    public void registerInvocationHandlerGenericType(GenericTypeMapping genericTypeMapping, String invocationHandlerClass,
            String resolvedType) {
        genericTypeMapping.addInvocationCallback(loadClass(invocationHandlerClass), loadClass(resolvedType));
    }
}
