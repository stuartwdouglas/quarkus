package io.quarkus.rest.deployment.processor;

import static io.quarkus.gizmo.MethodDescriptor.*;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.*;

import java.lang.reflect.Modifier;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestHttpHeaders;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.spi.QuarkusRestContainerRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestContext;
import io.quarkus.rest.runtime.spi.SimplifiedResourceInfo;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Generates the actual implementation of a provider that allows user code using annotations like
 * {@link io.quarkus.rest.ContainerRequestFilter} to work seamlessly
 */
final class CustomProviderGenerator {

    private CustomProviderGenerator() {
    }

    /**
     * Generates an implementation of {@link javax.ws.rs.container.ContainerRequestFilter} that delegates to the method
     * annotated with {@code @ContainerRequestFilter}.
     * <p>
     * An example of the generated code is:
     *
     * <pre>
     *
     * &#64;Singleton
     * &#64;Unremovable
     * public class CustomContainerRequestFilter$GeneratedContainerRequestFilter$someMethod implements ContainerRequestFilter {
     *     private final CustomContainerRequestFilter delegate;
     *
     *     &#64;Inject
     *     public CustomContainerRequestFilter$GeneratedContainerRequestFilter$someMethod(CustomContainerRequestFilter var1) {
     *         this.delegate = var1;
     *     }
     *
     *     public void filter(ContainerRequestContext var1) {
     *         QuarkusRestRequestContext var2 = (QuarkusRestRequestContext) ((QuarkusRestContainerRequestContext) var1)
     *                 .getQuarkusRestContext();
     *         UriInfo var3 = var2.getUriInfo();
     *         QuarkusRestHttpHeaders var4 = var2.getHttpHeaders();
     *         this.delegate.someMethod(var3, (HttpHeaders) var4);
     *     }
     * }
     *
     * </pre>
     */
    static String generateContainerRequestFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        checkModifiers(targetMethod, CUSTOM_CONTAINER_REQUEST_FILTER);
        String generatedClassName = getGeneratedClassName(targetMethod, CUSTOM_CONTAINER_REQUEST_FILTER);
        DotName declaringClassName = targetMethod.declaringClass().name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(javax.ws.rs.container.ContainerRequestFilter.class.getName())
                .build()) {
            cc.addAnnotation(Singleton.class);
            cc.addAnnotation(Unremovable.class);

            FieldDescriptor delegateField = cc.getFieldCreator("delegate", declaringClassName.toString())
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                    .getFieldDescriptor();

            // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
            MethodCreator ctor = cc.getMethodCreator("<init>", void.class, declaringClassName.toString());
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.addAnnotation(Inject.class);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ResultHandle self = ctor.getThis();
            ResultHandle config = ctor.getMethodParam(0);
            ctor.writeInstanceField(delegateField, self, config);
            ctor.returnValue(null);

            // generate the implementation of the filter method

            MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class);
            ResultHandle qrReqCtxHandle = getQRReqCtxHandle(filterMethod, 0);

            // for each of the parameters of the user method, generate bytecode that pulls the argument out of QuarkusRestRequestContext
            ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parameters().size()];
            for (int i = 0; i < targetMethod.parameters().size(); i++) {
                Type param = targetMethod.parameters().get(i);
                DotName paramDotName = param.name();
                if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.getMethodParam(0);
                } else if (URI_INFO.equals(paramDotName)) {
                    paramHandleFromContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i, "getUriInfo",
                            URI_INFO);
                } else if (HTTP_HEADERS.equals(paramDotName)) {
                    paramHandleFromContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i, "getHttpHeaders",
                            QuarkusRestHttpHeaders.class);
                } else if (JAXRS_REQUEST.equals(paramDotName)) {
                    paramHandleFromContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i, "getRequest",
                            JAXRS_REQUEST);
                } else if (VERTX_HTTP_SERVER_REQUEST.equals(paramDotName)) {
                    ResultHandle routingContextHandle = routingContextHandler(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                            ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
                } else if (RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getLazyMethod", LazyMethod.class), runtimeResourceHandle);
                } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimplifiedResourceInfo.class),
                            runtimeResourceHandle);
                } else {
                    String parameterName = targetMethod.parameterName(i);
                    throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                            + " of class '" + declaringClassName
                            + "' is not allowed");
                }
            }
            // call the target method
            filterMethod.invokeVirtualMethod(targetMethod,
                    filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                    targetMethodParamHandles);
            filterMethod.returnValue(null);
        }
        return generatedClassName;
    }

    private static ResultHandle runtimeResourceHandle(MethodCreator filterMethod, ResultHandle qrReqCtxHandle) {
        return filterMethod.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class, "getTarget", RuntimeResource.class), qrReqCtxHandle);
    }

    private static ResultHandle routingContextHandler(MethodCreator filterMethod, ResultHandle qrReqCtxHandle) {
        return filterMethod.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class, "getContext", RoutingContext.class), qrReqCtxHandle);
    }

    private static void paramHandleFromContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, DotName returnType) {
        paramHandleFromContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName, returnType.toString());
    }

    private static void paramHandleFromContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, Class<?> returnType) {
        paramHandleFromContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName, returnType.getName());
    }

    private static void paramHandleFromContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, String returnType) {
        targetMethodParamHandles[i] = m.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class.getName(), methodName, returnType), qrReqCtxHandle);
    }

    private static ResultHandle getQRReqCtxHandle(MethodCreator filter, int containerReqCtxParamIndex) {
        ResultHandle containerReqCtxHandle = filter.getMethodParam(containerReqCtxParamIndex);
        ResultHandle qrContainerReqCtxHandle = filter.checkCast(containerReqCtxHandle,
                QuarkusRestContainerRequestContext.class);
        ResultHandle qrCtxHandle = filter.invokeInterfaceMethod(
                ofMethod(QuarkusRestContainerRequestContext.class, "getQuarkusRestContext", QuarkusRestContext.class),
                qrContainerReqCtxHandle);
        return filter.checkCast(qrCtxHandle, QuarkusRestRequestContext.class);
    }

    private static String getGeneratedClassName(MethodInfo targetMethod, DotName annotationDotName) {
        DotName declaringClassName = targetMethod.declaringClass().name();
        return declaringClassName.toString() + "$Generated" + annotationDotName.withoutPackagePrefix() + "$"
                + targetMethod.name();
    }

    private static void checkModifiers(MethodInfo info, DotName annotationDotName) {
        if ((info.flags() & Modifier.PRIVATE) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be private as it is annotated with '@" + annotationDotName + "'");
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be static as it is annotated with '@" + annotationDotName + "'");
        }
    }
}
