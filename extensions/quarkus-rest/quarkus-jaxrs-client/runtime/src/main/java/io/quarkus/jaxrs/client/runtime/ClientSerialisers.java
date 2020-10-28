package io.quarkus.jaxrs.client.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.jaxrs.client.runtime.providers.serialisers.ClientDefaultTextPlainBodyHandler;
import io.quarkus.jaxrs.client.spi.QuarkusRestClientMessageBodyWriter;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.common.runtime.providers.serialisers.BooleanMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.CharacterMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.FileBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.common.runtime.providers.serialisers.InputStreamMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.NumberMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.ReaderBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class ClientSerialisers extends Serialisers {

    public static BuiltinReader[] BUILTIN_READERS = new BuiltinReader[] {
            new BuiltinReader(String.class, StringMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinReader(Boolean.class, BooleanMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(Character.class, CharacterMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(Number.class, NumberMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(InputStream.class, InputStreamMessageBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(Reader.class, ReaderBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(File.class, FileBodyHandler.class, MediaType.WILDCARD),

            new BuiltinReader(byte[].class, ByteArrayMessageBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(MultivaluedMap.class, FormUrlEncodedProvider.class, MediaType.APPLICATION_FORM_URLENCODED,
                    RuntimeType.CLIENT),
            new BuiltinReader(Object.class, ClientDefaultTextPlainBodyHandler.class, MediaType.TEXT_PLAIN, RuntimeType.CLIENT),
    };
    public static BuiltinWriter[] BUILTIN_WRITERS = new BuiltinWriter[] {
            new BuiltinWriter(String.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Number.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Boolean.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Character.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Object.class, StringMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(char[].class, CharArrayMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(byte[].class, ByteArrayMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(Buffer.class, VertxBufferMessageBodyWriter.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(MultivaluedMap.class, FormUrlEncodedProvider.class,
                    MediaType.APPLICATION_FORM_URLENCODED),
            new BuiltinWriter(InputStream.class, InputStreamMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(Reader.class, ReaderBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(File.class, FileBodyHandler.class,
                    MediaType.WILDCARD),
    };

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

    @Override
    public BuiltinWriter[] getBultinWriters() {
        return BUILTIN_WRITERS;
    }

    @Override
    public BuiltinReader[] getBultinReaders() {
        return BUILTIN_READERS;
    }
}
