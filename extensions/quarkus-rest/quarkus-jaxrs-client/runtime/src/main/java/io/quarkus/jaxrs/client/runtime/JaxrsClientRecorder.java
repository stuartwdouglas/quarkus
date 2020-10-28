package io.quarkus.jaxrs.client.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.client.WebTarget;

import io.quarkus.rest.common.runtime.core.GenericTypeMapping;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaxrsClientRecorder {

    private static final Map<String, Class<?>> primitiveTypes;
    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };
    private static volatile Serialisers serialisers;
    private static volatile GenericTypeMapping genericTypeMapping;

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

    private static volatile ClientProxies clientProxies = new ClientProxies(Collections.emptyMap());

    public static ClientProxies getClientProxies() {
        return clientProxies;
    }

    public static Serialisers getSerialisers() {
        return serialisers;
    }

    public static GenericTypeMapping getGenericTypeMapping() {
        return genericTypeMapping;
    }

    public void setupClientProxies(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        clientProxies = createClientImpls(clientImplementations);
    }

    private ClientProxies createClientImpls(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        Map<Class<?>, Function<WebTarget, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<Function<WebTarget, ?>>> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), entry.getValue().getValue());
        }
        return new ClientProxies(map);
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
}
