package io.quarkus.rest.common.runtime.core;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.spi.BeanFactory;

public class ArcBeanFactory<T> implements BeanFactory<T> {

    private final BeanContainer.Factory<T> factory;
    // for toString
    private final String targetClassName;

    public ArcBeanFactory(Class<T> target, BeanContainer beanContainer) {
        targetClassName = target.getName();
        factory = beanContainer.instanceFactory(target);
    }

    @Override
    public String toString() {
        return "ArcBeanFactory[" + targetClassName + "]";
    }

    @Override
    public BeanInstance<T> createInstance() {
        BeanContainer.Instance<T> instance = factory.create();
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
