package io.quarkus.test.junit.callback;

import java.lang.reflect.Method;

/**
 * Can be implemented by classes to modify the method parameters of a quarkus test.
 *
 * <p>
 * The implementing class has to be {@linkplain java.util.ServiceLoader deployed as service provider on the class path}.
 */
public interface QuarkusTestMethodParametersInterceptor {

    /**
     *
     * @param method The method that is being invoked
     * @param params A mutable parameter array. This array can be modified by the interceptor
     */
    void interceptTestMethod(Method method, Object[] params);
}
