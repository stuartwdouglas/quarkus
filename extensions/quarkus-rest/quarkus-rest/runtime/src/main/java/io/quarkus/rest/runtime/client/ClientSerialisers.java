package io.quarkus.rest.runtime.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.spi.QuarkusRestClientMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class ClientSerialisers {
    // FIXME: pass InvocationState to wrap args?
    public static Buffer invokeClientWriter(Entity<?> entity, Object entityObject, Class<?> entityClass, Type entityType,
            MultivaluedMap<String, String> headerMap, MessageBodyWriter writer, WriterInterceptor[] writerInterceptors,
            Map<String, Object> properties, Serialisers serialisers, QuarkusRestConfiguration configuration)
            throws IOException {
        if (writer instanceof QuarkusRestClientMessageBodyWriter && writerInterceptors == null) {
            QuarkusRestClientMessageBodyWriter<Object> quarkusRestWriter = (QuarkusRestClientMessageBodyWriter<Object>) writer;
            if (writer.isWriteable(entityClass, entityType, entity.getAnnotations(), entity.getMediaType())) {
                return quarkusRestWriter.writeResponse(entityObject);
            }
        } else {
            if (writer.isWriteable(entityClass, entityType, entity.getAnnotations(), entity.getMediaType())) {
                if (writerInterceptors == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entityObject, entityClass, entityType, entity.getAnnotations(),
                            entity.getMediaType(), headerMap, baos);
                    return Buffer.buffer(baos.toByteArray());
                } else {
                    return runClientWriterInterceptors(entityObject, entityClass, entityType, entity.getAnnotations(),
                            entity.getMediaType(), headerMap, writer, writerInterceptors, properties, serialisers,
                            configuration);
                }
            }
        }

        return null;
    }

    public static Buffer runClientWriterInterceptors(Object entity, Class<?> entityClass, Type entityType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers, MessageBodyWriter writer,
            WriterInterceptor[] writerInterceptors, Map<String, Object> properties, Serialisers serialisers,
            QuarkusRestConfiguration configuration) throws IOException {
        QuarkusRestClientWriterInterceptorContext wc = new QuarkusRestClientWriterInterceptorContext(writerInterceptors, writer,
                annotations, entityClass, entityType, entity, mediaType, headers, properties, serialisers, configuration);
        wc.proceed();
        return wc.getResult();
    }

    public static Object invokeClientReader(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType, Map<String, Object> properties,
            MultivaluedMap metadata, Serialisers serialisers, InputStream in, ReaderInterceptor[] interceptors,
            QuarkusRestConfiguration configuration)
            throws WebApplicationException, IOException {
        // FIXME: perhaps optimise for when we have no interceptor?
        QuarkusRestClientReaderInterceptorContext context = new QuarkusRestClientReaderInterceptorContext(annotations,
                entityClass, entityType, mediaType,
                properties, metadata, configuration, serialisers, in, interceptors);
        return context.proceed();
    }
}
