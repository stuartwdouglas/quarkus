package io.quarkus.rest.server.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;

import io.quarkus.rest.server.runtime.core.ParamConverterProviders;

public class RuntimeParameterConverter implements ParameterConverter {

    private final ParamConverter<?> converter;

    public RuntimeParameterConverter(ParamConverter<?> converter) {
        this.converter = converter;
    }

    @Override
    public Object convert(Object value) {
        return converter.fromString((String) value);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        // we're initialised in our constructor
    }

}
