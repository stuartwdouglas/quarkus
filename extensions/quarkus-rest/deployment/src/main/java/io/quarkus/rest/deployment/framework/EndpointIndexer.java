package io.quarkus.rest.deployment.framework;

import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BLOCKING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.CONSUMES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.LIST;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.MULTI_VALUED_MAP;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.NAME_BINDING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PATH;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRODUCES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.QUERY_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SORTED_SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.STRING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SUSPENDED;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.runtime.QuarkusRestConfig;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.parameters.converters.GeneratedParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ListConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SetConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SortedSetConverter;
import io.quarkus.rest.runtime.model.MethodParameter;
import io.quarkus.rest.runtime.model.ParameterType;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceMethod;
import io.quarkus.rest.runtime.model.RestClientInterface;
import io.quarkus.rest.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonArrayReader;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonObjectReader;
import io.quarkus.rest.runtime.providers.serialisers.jsonp.JsonStructureReader;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.runtime.util.HashUtil;

public class EndpointIndexer {

    private static final Map<String, String> primitiveTypes;

    private static final Logger log = Logger.getLogger(EndpointInvoker.class);

    static {
        Map<String, String> prims = new HashMap<>();
        prims.put(byte.class.getName(), Byte.class.getName());
        prims.put(Byte.class.getName(), Byte.class.getName());
        prims.put(boolean.class.getName(), Boolean.class.getName());
        prims.put(Boolean.class.getName(), Boolean.class.getName());
        prims.put(char.class.getName(), Character.class.getName());
        prims.put(Character.class.getName(), Character.class.getName());
        prims.put(short.class.getName(), Short.class.getName());
        prims.put(Short.class.getName(), Short.class.getName());
        prims.put(int.class.getName(), Integer.class.getName());
        prims.put(Integer.class.getName(), Integer.class.getName());
        prims.put(float.class.getName(), Float.class.getName());
        prims.put(Float.class.getName(), Float.class.getName());
        prims.put(double.class.getName(), Double.class.getName());
        prims.put(Double.class.getName(), Double.class.getName());
        prims.put(long.class.getName(), Long.class.getName());
        prims.put(Long.class.getName(), Long.class.getName());
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public static ResourceClass createEndpoints(IndexView index, ClassInfo classInfo, BeanContainer beanContainer,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, Map<DotName, String> scannedResourcePaths, QuarkusRestConfig config,
            AdditionalReaders additionalReaders) {
        try {
            String path = scannedResourcePaths.get(classInfo.name());
            List<ResourceMethod> methods = createEndpoints(index, classInfo, classInfo, new HashSet<>(),
                    generatedClassBuildItemBuildProducer, recorder, existingConverters, config, additionalReaders);
            ResourceClass clazz = new ResourceClass();
            clazz.getMethods().addAll(methods);
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(path);
            }
            clazz.setFactory(recorder.factory(clazz.getClassName(), beanContainer));
            return clazz;
        } catch (Exception e) {
            if (Modifier.isInterface(classInfo.flags()) || Modifier.isAbstract(classInfo.flags())) {
                //kinda bogus, but we just ignore failed interfaces for now
                //they can have methods that are not valid until they are actually extended by a concrete type
                log.debug("Ignoring interface " + classInfo.name(), e);
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public static RestClientInterface createClientProxy(IndexView index, ClassInfo classInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, String path, QuarkusRestConfig config,
            AdditionalReaders additionalReaders) {
        try {
            List<ResourceMethod> methods = createEndpoints(index, classInfo, classInfo, new HashSet<>(),
                    generatedClassBuildItemBuildProducer, recorder, existingConverters, config, additionalReaders);
            RestClientInterface clazz = new RestClientInterface();
            clazz.getMethods().addAll(methods);
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(path);
            }
            return clazz;
        } catch (Exception e) {
            //kinda bogus, but we just ignore failed interfaces for now
            //they can have methods that are not valid until they are actually extended by a concrete type
            log.debug("Ignoring interface for creating client proxy" + classInfo.name(), e);
            return null;
        }
    }

    private static List<ResourceMethod> createEndpoints(IndexView index, ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, Set<String> seenMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, QuarkusRestConfig config, AdditionalReaders additionalReaders) {
        List<ResourceMethod> ret = new ArrayList<>();
        String[] classProduces = extractProducesConsumesValues(currentClassInfo.classAnnotation(QuarkusRestDotNames.PRODUCES));
        String[] classConsumes = extractProducesConsumesValues(currentClassInfo.classAnnotation(QuarkusRestDotNames.CONSUMES));
        Set<String> classNameBindings = nameBindingNames(currentClassInfo, index);

        for (DotName httpMethod : QuarkusRestDotNames.JAXRS_METHOD_ANNOTATIONS) {
            List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(httpMethod);
            if (foundMethods != null) {
                for (AnnotationInstance annotation : foundMethods) {
                    MethodInfo info = annotation.target().asMethod();
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(info.annotation(PATH));
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    } else {
                        methodPath = "/";
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer,
                            recorder, classProduces, classConsumes, classNameBindings, httpMethod, info, methodPath, index,
                            existingConverters,
                            config, additionalReaders);

                    ret.add(method);
                }
            }
        }
        //now resource locator methods
        List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(PATH);
        if (foundMethods != null) {
            for (AnnotationInstance annotation : foundMethods) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo info = annotation.target().asMethod();
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(annotation);
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer,
                            recorder, classProduces, classConsumes, classNameBindings, null, info, methodPath, index,
                            existingConverters, config, additionalReaders);
                    ret.add(method);
                }
            }
        }

        DotName superClassName = currentClassInfo.superName();
        if (superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                ret.addAll(createEndpoints(index, superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, recorder, existingConverters, config, additionalReaders));
            }
        }
        List<DotName> interfaces = currentClassInfo.interfaceNames();
        for (DotName i : interfaces) {
            ClassInfo superClass = index.getClassByName(i);
            if (superClass != null) {
                ret.addAll(createEndpoints(index, superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, recorder, existingConverters, config, additionalReaders));
            }
        }
        return ret;
    }

    private static ResourceMethod createResourceMethod(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            String[] classProduces, String[] classConsumes, Set<String> classNameBindings, DotName httpMethod, MethodInfo info,
            String methodPath,
            IndexView indexView, Map<String, String> existingEndpoints, QuarkusRestConfig config,
            AdditionalReaders additionalReaders) {
        try {
            Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[info.parameters().size()];
            MethodParameter[] methodParameters = new MethodParameter[info.parameters()
                    .size()];
            for (int paramPos = 0; paramPos < info.parameters().size(); ++paramPos) {
                parameterAnnotations[paramPos] = new HashMap<>();
            }
            for (AnnotationInstance i : info.annotations()) {
                if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
                }
            }
            String[] consumes = extractProducesConsumesValues(info.annotation(CONSUMES), classConsumes);
            boolean suspended = false;
            boolean sse = false;
            for (int i = 0; i < methodParameters.length; ++i) {
                Map<DotName, AnnotationInstance> anns = parameterAnnotations[i];

                String name = null;
                AnnotationInstance pathParam = anns.get(QuarkusRestDotNames.PATH_PARAM);
                AnnotationInstance queryParam = anns.get(QUERY_PARAM);
                AnnotationInstance headerParam = anns.get(QuarkusRestDotNames.HEADER_PARAM);
                AnnotationInstance formParam = anns.get(QuarkusRestDotNames.FORM_PARAM);
                AnnotationInstance contextParam = anns.get(QuarkusRestDotNames.CONTEXT);
                AnnotationInstance matrixParam = anns.get(QuarkusRestDotNames.MATRIX_PARAM);
                AnnotationInstance defaultValueAnnotation = anns.get(QuarkusRestDotNames.DEFAULT_VALUE);
                AnnotationInstance suspendedAnnotation = anns.get(SUSPENDED);
                String defaultValue = null;
                if (defaultValueAnnotation != null) {
                    defaultValue = defaultValueAnnotation.value().asString();
                }
                ParameterType type;
                if (moreThanOne(pathParam, queryParam, headerParam, formParam, contextParam)) {
                    throw new RuntimeException(
                            "Cannot have more than one of @PathParam, @QueryParam, @HeaderParam, @FormParam, @Context on "
                                    + info);
                } else if (pathParam != null) {
                    name = pathParam.value().asString();
                    type = ParameterType.PATH;
                } else if (queryParam != null) {
                    name = queryParam.value().asString();
                    type = ParameterType.QUERY;
                } else if (headerParam != null) {
                    name = headerParam.value().asString();
                    type = ParameterType.HEADER;
                } else if (formParam != null) {
                    name = formParam.value().asString();
                    type = ParameterType.FORM;
                } else if (contextParam != null) {
                    // no name required
                    type = ParameterType.CONTEXT;
                } else if (suspendedAnnotation != null) {
                    // no name required
                    type = ParameterType.ASYNC_RESPONSE;
                    suspended = true;
                } else if (matrixParam != null) {
                    // no name required
                    type = ParameterType.MATRIX;
                } else {
                    type = ParameterType.BODY;
                }
                String elementType;
                boolean single = true;
                Supplier<ParameterConverter> converter = null;
                Type paramType = info.parameters().get(i);
                if (paramType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType pt = paramType.asParameterizedType();
                    if (pt.name().equals(LIST)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new ListConverter.ListSupplier(converter);
                    } else if (pt.name().equals(SET)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new SetConverter.SetSupplier(converter);
                    } else if (pt.name().equals(SORTED_SET)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new SortedSetConverter.SortedSetSupplier(converter);
                    } else if ((pt.name().equals(MULTI_VALUED_MAP)) && (type == ParameterType.BODY)) {
                        elementType = pt.name().toString();
                        single = true;
                        converter = null;
                        additionalReaders.add(FormUrlEncodedProvider.class, APPLICATION_FORM_URLENCODED, MultivaluedMap.class);
                    } else {
                        throw new RuntimeException("Invalid parameter type '" + pt + "' used on method '" + info.name()
                                + "' of class '" + info.declaringClass().name().toString() + "'");
                    }
                } else {
                    elementType = toClassName(paramType, currentClassInfo, actualEndpointInfo, indexView);

                    if (paramType.name().equals(QuarkusRestDotNames.BYTE_ARRAY_DOT_NAME)) {
                        additionalReaders.add(ByteArrayMessageBodyHandler.class, WILDCARD, byte[].class);
                    } else if (paramType.name().equals(QuarkusRestDotNames.JSONP_JSON_OBJECT)) {
                        additionalReaders.add(JsonObjectReader.class, APPLICATION_JSON, javax.json.JsonObject.class);
                    } else if (paramType.name().equals(QuarkusRestDotNames.JSONP_JSON_ARRAY)) {
                        additionalReaders.add(JsonArrayReader.class, APPLICATION_JSON, javax.json.JsonArray.class);
                    } else if (paramType.name().equals(QuarkusRestDotNames.JSONP_JSON_STRUCTURE)) {
                        additionalReaders.add(JsonStructureReader.class, APPLICATION_JSON, javax.json.JsonStructure.class);
                    }

                    if (type != ParameterType.CONTEXT && type != ParameterType.BODY && type != ParameterType.ASYNC_RESPONSE) {
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                    }
                    if (type == ParameterType.CONTEXT && elementType.equals(SseEventSink.class.getName())) {
                        sse = true;
                    }
                }
                if (suspendedAnnotation != null && !elementType.equals(AsyncResponse.class.getName())) {
                    throw new RuntimeException("Can only inject AsyncResponse on methods marked @Suspended");
                }
                methodParameters[i] = new MethodParameter(name,
                        elementType, type, single, converter, defaultValue);
            }

            String[] produces = extractProducesConsumesValues(info.annotation(PRODUCES), classProduces);
            Set<String> nameBindingNames = nameBindingNames(info, indexView, classNameBindings);
            boolean blocking = config.blocking;
            AnnotationInstance blockingAnnotation = getInheritableAnnotation(info, BLOCKING);
            if (blockingAnnotation != null) {
                AnnotationValue value = blockingAnnotation.value();
                if (value != null) {
                    blocking = value.asBoolean();
                } else {
                    blocking = true;
                }
            }

            ResourceMethod method = new ResourceMethod()
                    .setHttpMethod(annotationToMethod(httpMethod))
                    .setPath(methodPath)
                    .setConsumes(consumes)
                    .setProduces(produces)
                    .setNameBindingNames(nameBindingNames)
                    .setName(info.name())
                    .setBlocking(blocking)
                    .setSuspended(suspended)
                    .setSse(sse)
                    .setParameters(methodParameters)
                    .setSimpleReturnType(toClassName(info.returnType(), currentClassInfo, actualEndpointInfo, indexView))
                    // FIXME: resolved arguments ?
                    .setReturnType(AsmUtil.getSignature(info.returnType(), new Function<String, String>() {
                        @Override
                        public String apply(String v) {
                            //we attempt to resolve type variables
                            ClassInfo declarer = info.declaringClass();
                            int pos = -1;
                            for (;;) {
                                if (declarer == null) {
                                    return null;
                                }
                                List<TypeVariable> typeParameters = declarer.typeParameters();
                                for (int i = 0; i < typeParameters.size(); i++) {
                                    TypeVariable tv = typeParameters.get(i);
                                    if (tv.identifier().equals(v)) {
                                        pos = i;
                                    }
                                }
                                if (pos != -1) {
                                    break;
                                }
                                declarer = indexView.getClassByName(declarer.superName());
                            }
                            Type type = JandexUtil
                                    .resolveTypeParameters(info.declaringClass().name(), declarer.name(), indexView)
                                    .get(pos);
                            if (type.kind() == Type.Kind.TYPE_VARIABLE && type.asTypeVariable().identifier().equals(v)) {
                                List<Type> bounds = type.asTypeVariable().bounds();
                                if (bounds.isEmpty()) {
                                    return "Ljava/lang/Object;";
                                }
                                return AsmUtil.getSignature(bounds.get(0), this);
                            } else {
                                return AsmUtil.getSignature(type, this);
                            }
                        }
                    }));

            StringBuilder sigBuilder = new StringBuilder();
            sigBuilder.append(method.getName())
                    .append(method.getReturnType());
            for (MethodParameter t : method.getParameters()) {
                sigBuilder.append(t);
            }
            String baseName = currentClassInfo.name() + "$quarkusrestinvoker$" + method.getName() + "_"
                    + HashUtil.sha1(sigBuilder.toString());
            try (ClassCreator classCreator = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                    Object.class.getName(), EndpointInvoker.class.getName())) {
                MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
                ResultHandle[] args = new ResultHandle[method.getParameters().length];
                ResultHandle array = mc.getMethodParam(1);
                for (int i = 0; i < method.getParameters().length; ++i) {
                    args[i] = mc.readArrayValue(array, i);
                }
                ResultHandle res;
                if (Modifier.isInterface(currentClassInfo.flags())) {
                    res = mc.invokeInterfaceMethod(info, mc.getMethodParam(0), args);
                } else {
                    res = mc.invokeVirtualMethod(info, mc.getMethodParam(0), args);
                }
                if (info.returnType().kind() == Type.Kind.VOID) {
                    mc.returnValue(mc.loadNull());
                } else {
                    mc.returnValue(res);
                }
            }
            method.setInvoker(recorder.invoker(baseName));
            return method;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process method " + info.declaringClass().name() + "#" + info.toString(), e);
        }
    }

    private static AnnotationInstance getInheritableAnnotation(MethodInfo info, DotName name) {
        // try method first, class second
        AnnotationInstance annotation = info.annotation(name);
        if (annotation == null) {
            annotation = info.declaringClass().classAnnotation(name);
        }
        return annotation;
    }

    private static Supplier<ParameterConverter> extractConverter(String elementType, IndexView indexView,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            Map<String, String> existingConverters, MethodInfo methodInfo) {
        if (elementType.equals(String.class.getName())) {
            return null;
        } else if (existingConverters.containsKey(elementType)) {
            return new GeneratedParameterConverter().setClassName(existingConverters.get(elementType));
        }
        MethodDescriptor fromString = null;
        MethodDescriptor valueOf = null;
        MethodInfo stringCtor = null;
        String prim = primitiveTypes.get(elementType);
        String prefix = "";
        if (prim != null) {
            elementType = prim;
            valueOf = MethodDescriptor.ofMethod(elementType, "valueOf", elementType, String.class);
            prefix = "io.quarkus.generated.";
        } else {
            ClassInfo type = indexView.getClassByName(DotName.createSimple(elementType));
            if (type == null) {
                //todo: should we fall back to reflection here?
                throw new RuntimeException("Unknown parameter type " + elementType);
            }
            for (MethodInfo i : type.methods()) {
                if (i.parameters().size() == 1) {
                    if (i.parameters().get(0).name().equals(STRING)) {
                        if (i.name().equals("<init>")) {
                            stringCtor = i;
                        } else if (i.name().equals("valueOf")) {
                            valueOf = MethodDescriptor.of(i);
                        } else if (i.name().equals("fromString")) {
                            fromString = MethodDescriptor.of(i);
                        }
                    }
                }
            }
            if (type.isEnum()) {
                //spec weirdness, enums order is different
                if (fromString != null) {
                    valueOf = null;
                }
            }
        }

        String baseName = prefix + elementType + "$quarkusrestparamConverter$";
        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                Object.class.getName(), ParameterConverter.class.getName())) {
            MethodCreator mc = classCreator.getMethodCreator("convert", Object.class, Object.class);
            if (stringCtor != null) {
                ResultHandle ret = mc.newInstance(stringCtor, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else if (valueOf != null) {
                ResultHandle ret = mc.invokeStaticMethod(valueOf, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else if (fromString != null) {
                ResultHandle ret = mc.invokeStaticMethod(fromString, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else {
                throw new RuntimeException("Unknown parameter type " + elementType + " on method " + methodInfo + " on class "
                        + methodInfo.declaringClass());
            }
        }
        existingConverters.put(elementType, baseName);
        return new GeneratedParameterConverter().setClassName(baseName);
    }

    /**
     * Returns the class names of the {@code @NameBinding} annotations or null if non are present
     */
    public static Set<String> nameBindingNames(ClassInfo classInfo, IndexView indexView) {
        return nameBindingNames(instanceDotNames(classInfo.classAnnotations()), indexView);
    }

    private static Set<String> nameBindingNames(MethodInfo methodInfo, IndexView indexView, Set<String> defaultValue) {
        Set<String> fromMethod = nameBindingNames(instanceDotNames(methodInfo.annotations()), indexView);
        if (fromMethod.isEmpty()) {
            return defaultValue;
        }
        return fromMethod;
    }

    private static List<DotName> instanceDotNames(Collection<AnnotationInstance> instances) {
        List<DotName> result = new ArrayList<>(instances.size());
        for (AnnotationInstance instance : instances) {
            result.add(instance.name());
        }
        return result;
    }

    private static Set<String> nameBindingNames(Collection<DotName> annotations, IndexView indexView) {
        Set<String> result = new HashSet<>();
        for (DotName classAnnotationDotName : annotations) {
            if (classAnnotationDotName.equals(PATH) || classAnnotationDotName.equals(CONSUMES)
                    || classAnnotationDotName.equals(PRODUCES)) {
                continue;
            }
            ClassInfo classAnnotation = indexView.getClassByName(classAnnotationDotName);
            if (classAnnotation == null) {
                return result;
            }
            if (classAnnotation.classAnnotation(NAME_BINDING) != null) {
                result.add(classAnnotation.name().toString());
            }
        }
        return result;
    }

    private static String methodDescriptor(MethodInfo info) {
        return info.name() + ":" + AsmUtil.getDescriptor(info, s -> null);
    }

    private static boolean moreThanOne(AnnotationInstance... annotations) {
        boolean oneNonNull = false;
        for (AnnotationInstance annotation : annotations) {
            if (annotation != null) {
                if (oneNonNull)
                    return true;
                oneNonNull = true;
            }
        }
        return false;
    }

    private static String[] extractProducesConsumesValues(AnnotationInstance annotation, String[] defaultValue) {
        String[] read = extractProducesConsumesValues(annotation);
        if (read == null) {
            return defaultValue;
        }
        return read;
    }

    private static String[] extractProducesConsumesValues(AnnotationInstance annotation) {
        if (annotation == null) {
            return null;
        }
        String[] originalStrings = annotation.value().asStringArray();
        if (originalStrings.length > 0) {
            List<String> result = new ArrayList<>(originalStrings.length);
            for (String s : originalStrings) {
                String[] trimmed = s.split(","); // spec says that the value can be a comma separated list...
                for (String t : trimmed) {
                    result.add(t.trim());
                }
            }
            return result.toArray(new String[0]);
        } else {
            return originalStrings;
        }

    }

    private static String annotationToMethod(DotName httpMethod) {
        if (httpMethod == null) {
            return null; //resource locators
        }
        if (httpMethod.equals(QuarkusRestDotNames.GET)) {
            return "GET";
        } else if (httpMethod.equals(QuarkusRestDotNames.POST)) {
            return "POST";
        } else if (httpMethod.equals(QuarkusRestDotNames.HEAD)) {
            return "HEAD";
        } else if (httpMethod.equals(QuarkusRestDotNames.PUT)) {
            return "PUT";
        } else if (httpMethod.equals(QuarkusRestDotNames.DELETE)) {
            return "DELETE";
        } else if (httpMethod.equals(QuarkusRestDotNames.PATCH)) {
            return "PATCH";
        } else if (httpMethod.equals(QuarkusRestDotNames.OPTIONS)) {
            return "OPTIONS";
        }
        throw new IllegalStateException("Unknown HTTP method annotation " + httpMethod);
    }

    public static String readStringValue(AnnotationInstance annotation, String defaultValue) {
        String val = readStringValue(annotation);
        return val == null ? defaultValue : val;
    }

    public static String readStringValue(AnnotationInstance annotationInstance) {
        String classProduces = null;
        if (annotationInstance != null) {
            classProduces = annotationInstance.value().asString();
        }
        return classProduces;
    }

    private static String toClassName(Type indexType, ClassInfo currentClass, ClassInfo actualEndpointClass,
            IndexView indexView) {
        switch (indexType.kind()) {
            case VOID:
                return "void";
            case CLASS:
                return indexType.asClassType().name().toString();
            case PRIMITIVE:
                return indexType.asPrimitiveType().primitive().name().toLowerCase(Locale.ENGLISH);
            case PARAMETERIZED_TYPE:
                return indexType.asParameterizedType().name().toString();
            case ARRAY:
                return indexType.asArrayType().name().toString();
            case TYPE_VARIABLE:
                TypeVariable typeVariable = indexType.asTypeVariable();
                if (typeVariable.bounds().isEmpty()) {
                    return Object.class.getName();
                }
                int pos = -1;
                for (int i = 0; i < currentClass.typeParameters().size(); ++i) {
                    if (currentClass.typeParameters().get(i).identifier().equals(typeVariable.identifier())) {
                        pos = i;
                        break;
                    }
                }
                if (pos != -1) {
                    List<Type> params = JandexUtil.resolveTypeParameters(actualEndpointClass.name(), currentClass.name(),
                            indexView);

                    Type resolved = params.get(pos);
                    if (resolved.kind() != Type.Kind.TYPE_VARIABLE
                            || !resolved.asTypeVariable().identifier().equals(typeVariable.identifier())) {
                        return toClassName(resolved, currentClass, actualEndpointClass, indexView);
                    }
                }
                return toClassName(typeVariable.bounds().get(0), currentClass, actualEndpointClass, indexView);
            default:
                throw new RuntimeException("Unknown parameter type " + indexType);
        }
    }

    private static String appendPath(String prefix, String suffix) {
        if (prefix == null) {
            return suffix;
        } else if (suffix == null) {
            return prefix;
        }
        if ((prefix.endsWith("/") && !suffix.startsWith("/")) ||
                (!prefix.endsWith("/") && suffix.startsWith("/"))) {
            return prefix + suffix;
        } else if (prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1) + suffix;
        } else {
            return prefix + "/" + suffix;
        }
    }

    static class MethodDesc {
        AnnotationInstance httpMethod;
        AnnotationInstance path;
        boolean blocking;

    }

}
