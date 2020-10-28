package io.quarkus.rest.server.runtime.jaxrs;

import java.lang.reflect.Method;

import javax.ws.rs.container.ResourceInfo;

import io.quarkus.rest.server.runtime.model.ResourceClass;
import io.quarkus.rest.server.runtime.model.ResourceMethod;

public class QuarkusRestResourceMethod implements ResourceInfo {

    private final Class<?> resourceClass;
    private final Method resourceMethod;

    public QuarkusRestResourceMethod(ResourceClass resourceClass, ResourceMethod resourceMethod) {
        try {
            Class<?> clazz = Class.forName(resourceClass.getClassName(), false, Thread.currentThread().getContextClassLoader());
            Method[] methods = clazz.getMethods();
            Method method = null;
            for (Method m : methods) {
                if ((m.getName().equals(resourceMethod.getName()))
                        && (m.getParameterCount() == resourceMethod.getParameters().length)) {
                    if (m.getParameterCount() == 0) {
                        method = m;
                        break;
                    } else {
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        boolean typesMatch = true;
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (!parameterTypes[i].getName().equals(resourceMethod.getParameters()[i].type)) {
                                typesMatch = false;
                                break;
                            }
                        }
                        if (typesMatch) {
                            method = m;
                        }
                    }
                }
                if (method != null) {
                    break;
                }
            }
            if (method == null) {
                throw new IllegalStateException();
            }
            this.resourceClass = clazz;
            this.resourceMethod = method;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public QuarkusRestResourceMethod(Class<?> resourceClass, Method resourceMethod) {
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
    }

    @Override
    public Method getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public Class<?> getResourceClass() {
        return resourceClass;
    }
}
