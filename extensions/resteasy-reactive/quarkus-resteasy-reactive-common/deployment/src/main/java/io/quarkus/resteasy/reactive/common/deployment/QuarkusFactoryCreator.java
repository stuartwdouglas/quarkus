package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Function;

import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;

public class QuarkusFactoryCreator implements Function<String, BeanFactory<Object>> {

    final ResteasyReactiveCommonRecorder recorder;

    public QuarkusFactoryCreator(ResteasyReactiveCommonRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public BeanFactory<Object> apply(String s) {
        return recorder.factory(s);
    }
}
