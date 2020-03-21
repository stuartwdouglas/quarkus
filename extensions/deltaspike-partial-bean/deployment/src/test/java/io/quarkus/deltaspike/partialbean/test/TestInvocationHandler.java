package io.quarkus.deltaspike.partialbean.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@TestBinding
public class TestInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.getName();
    }
}
