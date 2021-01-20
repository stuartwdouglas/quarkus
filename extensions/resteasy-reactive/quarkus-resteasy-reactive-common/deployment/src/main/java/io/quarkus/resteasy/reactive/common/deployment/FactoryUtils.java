package io.quarkus.resteasy.reactive.common.deployment;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;

public class FactoryUtils {
    public static <T> BeanFactory<T> factory(ClassInfo providerClass, Set<String> singletons,
            ResteasyReactiveCommonRecorder recorder) {
        return factory(providerClass.name().toString(), singletons, recorder);
    }

    public static <T> BeanFactory<T> factory(String providerClass, Set<String> singletons,
            ResteasyReactiveCommonRecorder recorder) {
        Objects.requireNonNull(providerClass, "providerClass cannot be null");
        if (singletons.contains(providerClass)) {
            return new SingletonBeanFactory<>(providerClass);
        } else {
            return recorder.factory(providerClass);
        }
    }
}
