package io.quarkus.rest.deployment.processor;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.deployment.framework.AdditionalReaders;
import io.quarkus.rest.deployment.framework.EndpointIndexer;
import io.quarkus.rest.deployment.framework.QuarkusRestDotNames;
import io.quarkus.rest.runtime.QuarkusRestConfig;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.ContextResolvers;
import io.quarkus.rest.runtime.core.DynamicFeatures;
import io.quarkus.rest.runtime.core.ExceptionMapping;
import io.quarkus.rest.runtime.core.Features;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.injection.ContextProducers;
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
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.model.RestClientInterface;
import io.quarkus.rest.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.runtime.providers.serialisers.InputStreamMessageBodyReader;
import io.quarkus.rest.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.quarkus.rest.runtime.providers.serialisers.VertxJsonMessageBodyWriter;
import io.quarkus.rest.runtime.providers.serialisers.jsonb.JsonbMessageBodyReader;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.buffer.Buffer;

public class QuarkusRestProcessor {

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem(Feature.QUARKUS_REST);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.QUARKUS_REST);
    }

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class).build());
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Context.class.getName()));

    }

    @BuildStep
    void scanResources(
            // TODO: We need to use this index instead of BeanArchiveIndexBuildItem to avoid build cycles. It it OK?
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerBuildItemBuildProducer,
            BuildProducer<ResourceScanningResultBuildItem> resourceScanningResultBuildItemBuildProducer) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> paths = index.getAnnotations(QuarkusRestDotNames.PATH);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return;
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Map<DotName, String> scannedResourcePaths = new HashMap<>();
        Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
        Map<DotName, String> pathInterfaces = new HashMap<>();
        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = new HashMap<>();

        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                    scannedResourcePaths.put(clazz.name(), annotation.value().asString());
                } else {
                    pathInterfaces.put(clazz.name(), annotation.value().asString());
                }
                MethodInfo ctor = hasJaxRsCtorParams(clazz);
                if (ctor != null) {
                    resourcesThatNeedCustomProducer.put(clazz.name(), ctor);
                }
            }
        }

        if (!resourcesThatNeedCustomProducer.isEmpty()) {
            annotationsTransformerBuildItemBuildProducer
                    .produce(new AnnotationsTransformerBuildItem(
                            new VetoingAnnotationTransformer(resourcesThatNeedCustomProducer.keySet())));
        }

        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            for (ClassInfo clazz : index.getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        if (!scannedResources.containsKey(clazz.name())) {
                            scannedResources.put(clazz.name(), clazz);
                            scannedResourcePaths.put(clazz.name(), i.getValue());
                        }
                    }
                }
            }
        }

        resourceScanningResultBuildItemBuildProducer.produce(new ResourceScanningResultBuildItem(scannedResources,
                scannedResourcePaths, possibleSubResources, pathInterfaces, resourcesThatNeedCustomProducer));
    }

    @BuildStep
    void generateCustomProducer(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }

        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = resourceScanningResultBuildItem.get()
                .getResourcesThatNeedCustomProducer();
        if (!resourcesThatNeedCustomProducer.isEmpty()) {
            CustomResourceProducersGenerator.generate(resourcesThatNeedCustomProducer, generatedBeanBuildItemBuildProducer);
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public FilterBuildItem setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            QuarkusRestConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            QuarkusRestRecorder recorder,
            RecorderContext recorderContext,
            ShutdownContextBuildItem shutdownContext,
            HttpBuildTimeConfig vertxConfig) {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return null;
        }

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<ClassInfo> containerRequestFilters = index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_REQUEST_FILTER);
        Collection<ClassInfo> containerResponseFilters = index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_RESPONSE_FILTER);
        Collection<ClassInfo> exceptionMappers = index
                .getAllKnownImplementors(QuarkusRestDotNames.EXCEPTION_MAPPER);
        Collection<ClassInfo> writers = index
                .getAllKnownImplementors(QuarkusRestDotNames.MESSAGE_BODY_WRITER);
        Collection<ClassInfo> readers = index
                .getAllKnownImplementors(QuarkusRestDotNames.MESSAGE_BODY_READER);
        Collection<ClassInfo> contextResolvers = index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTEXT_RESOLVER);
        Collection<ClassInfo> features = index
                .getAllKnownImplementors(QuarkusRestDotNames.FEATURE);
        Collection<ClassInfo> dynamicFeatures = index
                .getAllKnownImplementors(QuarkusRestDotNames.DYNAMIC_FEATURE);

        Map<DotName, ClassInfo> scannedResources = resourceScanningResultBuildItem.get().getScannedResources();
        Map<DotName, String> scannedResourcePaths = resourceScanningResultBuildItem.get().getScannedResourcePaths();
        Map<DotName, ClassInfo> possibleSubResources = resourceScanningResultBuildItem.get().getPossibleSubResources();
        Map<DotName, String> pathInterfaces = resourceScanningResultBuildItem.get().getPathInterfaces();

        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        AdditionalReaders additionalReaders = new AdditionalReaders();
        for (ClassInfo i : scannedResources.values()) {
            ResourceClass endpoints = EndpointIndexer.createEndpoints(index, i,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths, config, additionalReaders);
            if (endpoints != null) {
                resourceClasses.add(endpoints);
            }
        }

        List<RestClientInterface> clientDefinitions = new ArrayList<>();
        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            ClassInfo clazz = index.getClassByName(i.getKey());
            //these interfaces can also be clients
            //so we generate client proxies for them
            clientDefinitions.add(EndpointIndexer.createClientProxy(index, clazz,
                    generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    i.getValue(), config, additionalReaders));
        }
        Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations = generateClientInvokers(recorderContext,
                clientDefinitions, generatedClassBuildItemBuildProducer);

        //now index possible sub resources. These are all classes that have method annotations
        //that are not annotated @Path
        //TODO custom method annotations
        Deque<ClassInfo> toScan = new ArrayDeque<>();
        for (DotName methodAnnotation : QuarkusRestDotNames.JAXRS_METHOD_ANNOTATIONS) {
            for (AnnotationInstance instance : index.getAnnotations(methodAnnotation)) {
                MethodInfo method = instance.target().asMethod();
                ClassInfo classInfo = method.declaringClass();
                toScan.add(classInfo);
            }
        }
        while (!toScan.isEmpty()) {
            ClassInfo classInfo = toScan.poll();
            if (scannedResources.containsKey(classInfo.name()) ||
                    pathInterfaces.containsKey(classInfo.name()) ||
                    possibleSubResources.containsKey(classInfo.name())) {
                continue;
            }
            possibleSubResources.put(classInfo.name(), classInfo);
            ResourceClass endpoints = EndpointIndexer.createEndpoints(index, classInfo,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths, config, additionalReaders);
            if (endpoints != null) {
                subResourceClasses.add(endpoints);
            }
            //we need to also look for all sub classes and interfaces
            //they may have type variables that need to be handled
            toScan.addAll(index.getKnownDirectImplementors(classInfo.name()));
            toScan.addAll(index.getKnownDirectSubclasses(classInfo.name()));
        }

        ResourceInterceptors interceptors = new ResourceInterceptors();
        for (ClassInfo filterClass : containerRequestFilters) {
            if (filterClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceRequestInterceptor interceptor = new ResourceRequestInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                interceptor.setPreMatching(filterClass.classAnnotation(QuarkusRestDotNames.PRE_MATCHING) != null);
                if (interceptor.isPreMatching()) {
                    interceptors.addResourcePreMatchInterceptor(interceptor);
                } else {
                    Set<String> nameBindingNames = EndpointIndexer.nameBindingNames(filterClass, index);
                    if (nameBindingNames.isEmpty()) {
                        interceptors.addGlobalRequestInterceptor(interceptor);
                    } else {
                        interceptor.setNameBindingNames(nameBindingNames);
                        interceptors.addNameRequestInterceptor(interceptor);
                    }
                }
                AnnotationInstance priorityInstance = filterClass.classAnnotation(QuarkusRestDotNames.PRIORITY);
                if (priorityInstance != null) {
                    interceptor.setPriority(priorityInstance.value().asInt());
                }
            }
        }
        for (ClassInfo filterClass : containerResponseFilters) {
            if (filterClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceResponseInterceptor interceptor = new ResourceResponseInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                Set<String> nameBindingNames = EndpointIndexer.nameBindingNames(filterClass, index);
                if (nameBindingNames.isEmpty()) {
                    interceptors.addGlobalResponseInterceptor(interceptor);
                } else {
                    interceptor.setNameBindingNames(nameBindingNames);
                    interceptors.addNameResponseInterceptor(interceptor);
                }
                AnnotationInstance priorityInstance = filterClass.classAnnotation(QuarkusRestDotNames.PRIORITY);
                if (priorityInstance != null) {
                    interceptor.setPriority(priorityInstance.value().asInt());
                }
            }
        }

        ExceptionMapping exceptionMapping = new ExceptionMapping();
        for (ClassInfo mapperClass : exceptionMappers) {
            if (mapperClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(),
                        QuarkusRestDotNames.EXCEPTION_MAPPER,
                        index);
                ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
                mapper.setFactory(recorder.factory(mapperClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerExceptionMapper(exceptionMapping, typeParameters.get(0).name().toString(), mapper);
            }
        }

        ContextResolvers ctxResolvers = new ContextResolvers();
        for (ClassInfo resolverClass : contextResolvers) {
            if (resolverClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(resolverClass.name(),
                        QuarkusRestDotNames.CONTEXT_RESOLVER,
                        index);
                ResourceContextResolver resolver = new ResourceContextResolver();
                resolver.setFactory(recorder.factory(resolverClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                resolver.setMediaTypeStrings(getProducesMediaTypes(resolverClass));
                recorder.registerContextResolver(ctxResolvers, typeParameters.get(0).name().toString(), resolver);
            }
        }

        Features feats = new Features();
        for (ClassInfo featureClass : features) {
            if (featureClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceFeature resourceFeature = new ResourceFeature();
                resourceFeature.setFactory(recorder.factory(featureClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerFeature(feats, resourceFeature);
            }
        }

        DynamicFeatures dynamicFeats = new DynamicFeatures();
        for (ClassInfo dynamicFeatureClass : dynamicFeatures) {
            if (dynamicFeatureClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceDynamicFeature resourceFeature = new ResourceDynamicFeature();
                resourceFeature.setFactory(recorder.factory(dynamicFeatureClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerDynamicFeature(dynamicFeats, resourceFeature);
            }
        }

        Serialisers serialisers = new Serialisers();
        for (ClassInfo writerClass : writers) {
            if (writerClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceWriter writer = new ResourceWriter();
                AnnotationInstance producesAnnotation = writerClass.classAnnotation(QuarkusRestDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    writer.setMediaTypeStrings(Arrays.asList(producesAnnotation.value().asStringArray()));
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        QuarkusRestDotNames.MESSAGE_BODY_WRITER,
                        index);
                writer.setFactory(recorder.factory(writerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerWriter(serialisers, typeParameters.get(0).name().toString(), writer);
            }
        }
        for (ClassInfo readerClass : readers) {
            if (readerClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        QuarkusRestDotNames.MESSAGE_BODY_READER,
                        index);
                ResourceReader reader = new ResourceReader();
                reader.setFactory(recorder.factory(readerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerReader(serialisers, typeParameters.get(0).name().toString(), reader);
            }
        }

        // built-ins
        //        registerWriter(recorder, serialisers, Object.class, JsonbMessageBodyWriter.class, beanContainerBuildItem.getValue(),
        //                false);
        registerWriter(recorder, serialisers, Object.class, VertxJsonMessageBodyWriter.class, beanContainerBuildItem.getValue(),
                MediaType.APPLICATION_JSON);
        registerWriter(recorder, serialisers, String.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Number.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Boolean.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Character.class, StringMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Object.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, char[].class, CharArrayMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, byte[].class, ByteArrayMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, Buffer.class, VertxBufferMessageBodyWriter.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, MultivaluedMap.class, FormUrlEncodedProvider.class,
                beanContainerBuildItem.getValue(), MediaType.APPLICATION_FORM_URLENCODED);

        registerReader(recorder, serialisers, String.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, InputStream.class, InputStreamMessageBodyReader.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, Object.class, JsonbMessageBodyReader.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, Object.class, JsonbMessageBodyReader.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        for (AdditionalReaders.Entry additionalReader : additionalReaders.get()) {
            registerReader(recorder, serialisers, additionalReader.getEntityClass(), additionalReader.getReaderClass(),
                    beanContainerBuildItem.getValue(), additionalReader.getMediaType());
        }

        return new FilterBuildItem(
                recorder.handler(interceptors.sort(), exceptionMapping, ctxResolvers, feats, dynamicFeats,
                        serialisers, resourceClasses, subResourceClasses,
                        beanContainerBuildItem.getValue(), shutdownContext, config, vertxConfig, clientImplementations),
                10);
    }

    private void registerWriter(QuarkusRestRecorder recorder, Serialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyWriter<?>> writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass.getName(), beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    private <T> void registerReader(QuarkusRestRecorder recorder, Serialisers serialisers, Class<T> entityClass,
            Class<? extends MessageBodyReader<T>> readerClass, BeanContainer beanContainer, String mediaType) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass.getName(), beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerReader(serialisers, entityClass.getName(), reader);

    }

    private List<String> getProducesMediaTypes(ClassInfo classInfo) {
        AnnotationInstance produces = classInfo.classAnnotation(QuarkusRestDotNames.PRODUCES);
        if (produces == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(produces.value().asStringArray());
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }

    private Map<String, RuntimeValue<Function<WebTarget, ?>>> generateClientInvokers(RecorderContext recorderContext,
            List<RestClientInterface> clientDefinitions,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
        Map<String, RuntimeValue<Function<WebTarget, ?>>> ret = new HashMap<>();
        for (RestClientInterface restClientInterface : clientDefinitions) {
            boolean subResource = false;
            //if the interface contains sub resource locator methods we ignore it
            for (ResourceMethod i : restClientInterface.getMethods()) {
                if (i.getHttpMethod() == null) {
                    subResource = true;
                }
                break;
            }
            if (subResource) {
                continue;
            }
            String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
            MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
            try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                    name, null, Object.class.getName(), restClientInterface.getClassName())) {

                FieldDescriptor target = FieldDescriptor.of(name, "target", WebTarget.class);
                c.getFieldCreator(target).setModifiers(Modifier.FINAL);

                MethodCreator ctor = c.getMethodCreator(ctorDesc);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());

                ResultHandle res = ctor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                        ctor.getMethodParam(0), ctor.load(restClientInterface.getPath()));
                ctor.writeInstanceField(target, ctor.getThis(), res);
                ctor.returnValue(null);

                for (ResourceMethod method : restClientInterface.getMethods()) {
                    MethodCreator m = c.getMethodCreator(method.getName(), method.getReturnType(),
                            Arrays.stream(method.getParameters()).map(s -> s.type).toArray());
                    ResultHandle tg = m.readInstanceField(target, m.getThis());
                    if (method.getPath() != null) {
                        tg = m.invokeInterfaceMethod(MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class,
                                String.class), tg, m.load(method.getPath()));
                    }

                    for (int i = 0; i < method.getParameters().length; ++i) {
                        MethodParameter p = method.getParameters()[i];
                        if (p.parameterType == ParameterType.QUERY) {
                            //TODO: converters
                            ResultHandle array = m.newArray(Object.class, 1);
                            m.writeArrayValue(array, 0, m.getMethodParam(i));
                            tg = m.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                                            String.class, Object[].class),
                                    tg, m.load(p.name), array);
                        }

                    }

                    ResultHandle builder;
                    if (method.getProduces() == null || method.getProduces().length == 0) {
                        builder = m.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class), tg);
                    } else {

                        ResultHandle array = m.newArray(String.class, method.getProduces().length);
                        for (int i = 0; i < method.getProduces().length; ++i) {
                            m.writeArrayValue(array, i, m.load(method.getProduces()[i]));
                        }
                        builder = m.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class, String[].class),
                                tg, array);
                    }
                    //TODO: async return types

                    ResultHandle result = m
                            .invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            Class.class),
                                    builder, m.load(method.getHttpMethod()), m.loadClass(method.getSimpleReturnType()));

                    m.returnValue(result);
                }

            }
            String creatorName = restClientInterface.getClassName() + "$$QuarkusRestClientInterfaceCreator";
            try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                    creatorName, null, Object.class.getName(), Function.class.getName())) {

                MethodCreator apply = c
                        .getMethodCreator(MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class));
                apply.returnValue(apply.newInstance(ctorDesc, apply.getMethodParam(0)));
            }
            ret.put(restClientInterface.getClassName(), recorderContext.newInstance(creatorName));

        }
        return ret;
    }

    private MethodInfo hasJaxRsCtorParams(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        List<MethodInfo> ctors = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.name().equals("<init>")) {
                ctors.add(method);
            }
        }
        if (ctors.size() != 1) { // we only need to deal with a single ctor here
            return null;
        }
        MethodInfo ctor = ctors.get(0);
        if (ctor.parameters().size() == 0) { // default ctor - we don't need to do anything
            return null;
        }

        boolean needsHandling = false;
        for (DotName dotName : QuarkusRestDotNames.RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING) {
            if (ctor.hasAnnotation(dotName)) {
                needsHandling = true;
                break;
            }
        }
        return needsHandling ? ctor : null;
    }

}
