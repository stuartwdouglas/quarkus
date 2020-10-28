package io.quarkus.rest.server.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.quarkus.rest.server.runtime.core.ParamConverterProviders;
import io.quarkus.rest.server.runtime.util.PathSegmentImpl;

public class PathSegmentParamConverter implements ParameterConverter {
    @Override
    public Object convert(Object parameter) {
        return new PathSegmentImpl(parameter.toString(), true);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        // no init required
    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public String getClassName() {
            return PathSegmentParamConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return new PathSegmentParamConverter();
        }
    }
}
