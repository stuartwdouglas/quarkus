package io.quarkus.rest.jsonb.runtime.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;

public class JsonbMessageBodyWriter implements QuarkusRestMessageBodyWriter<Object> {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyWriter(Jsonb json) {
        this.json = json;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return !String.class.equals(type);
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        json.toJson(o, type, entityStream);
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return !String.class.equals(type);
    }

    @Override
    public void writeResponse(Object o, QuarkusRestRequestContext context) throws WebApplicationException, IOException {
        try (OutputStream stream = context.getOrCreateOutputStream()) {
            json.toJson(o, stream);
        }

    }
}
