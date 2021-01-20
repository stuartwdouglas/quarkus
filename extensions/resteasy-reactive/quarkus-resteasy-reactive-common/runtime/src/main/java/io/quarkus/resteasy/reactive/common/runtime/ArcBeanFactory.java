package io.quarkus.resteasy.reactive.common.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public class ArcBeanFactory<T> implements BeanFactory<T> {

    private final Supplier<InstanceHandle<T>> factory;
    // for toString
    private final String targetClassName;

    public ArcBeanFactory(Class<T> target, ArcContainer beanContainer) {
        targetClassName = target.getName();
        factory = beanContainer.instanceSupplier(target);
    }

    @Override
    public String toString() {
        return "ArcBeanFactory[" + targetClassName + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        InstanceHandle<T> instance = factory.get();
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                return instance.get();
            }

            @Override
            public void close() {
                instance.close();
            }
        };
    }
}
