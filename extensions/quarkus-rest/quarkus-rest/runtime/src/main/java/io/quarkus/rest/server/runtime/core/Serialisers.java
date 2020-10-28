package io.quarkus.rest.server.runtime.core;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.common.runtime.core.UnmanagedBeanFactory;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.common.runtime.model.ResourceReader;
import io.quarkus.rest.common.runtime.model.ResourceWriter;
import io.quarkus.rest.common.runtime.util.MediaTypeHelper;
import io.quarkus.rest.common.runtime.util.QuarkusMultivaluedHashMap;
import io.quarkus.rest.common.runtime.util.QuarkusMultivaluedMap;
import io.quarkus.rest.server.runtime.providers.serialisers.BooleanMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.CharacterMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.ClientDefaultTextPlainBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.FileBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.server.runtime.providers.serialisers.InputStreamMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.NumberMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.ReaderBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.ServerDefaultTextPlainBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.rest.server.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class Serialisers {
    protected static final Map<Class<?>, Class<?>> primitivesToWrappers = new HashMap<>();
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
            new BuiltinReader(Object.class, ServerDefaultTextPlainBodyHandler.class, MediaType.TEXT_PLAIN, RuntimeType.SERVER),
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
    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection
    protected final QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers = new QuarkusMultivaluedHashMap<>();
    protected final QuarkusMultivaluedMap<Class<?>, ResourceReader> readers = new QuarkusMultivaluedHashMap<>();

    public List<MessageBodyReader<?>> findReaders(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType mediaType) {
        return findReaders(configuration, entityType, mediaType, null);
    }

    public List<MessageBodyReader<?>> findReaders(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType mediaType, RuntimeType runtimeType) {
        List<MediaType> mt = Collections.singletonList(mediaType);
        List<MessageBodyReader<?>> ret = new ArrayList<>();
        Deque<Class<?>> toProcess = new LinkedList<>();
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        QuarkusMultivaluedMap<Class<?>, ResourceReader> readers;
        if (configuration != null && !configuration.getResourceReaders().isEmpty()) {
            readers = new QuarkusMultivaluedHashMap<>();
            readers.putAll(this.readers);
            readers.addAll(configuration.getResourceReaders());
        } else {
            readers = this.readers;
        }
        do {
            Collections.addAll(toProcess, klass.getInterfaces());
            if (klass == Object.class || klass.getSuperclass() == null) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceReader> goodTypeReaders = readers.get(iface);
                    readerLookup(mediaType, runtimeType, mt, ret, goodTypeReaders);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceReader> goodTypeReaders = readers.get(klass);
            readerLookup(mediaType, runtimeType, mt, ret, goodTypeReaders);
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    private void readerLookup(MediaType mediaType, RuntimeType runtimeType, List<MediaType> mt, List<MessageBodyReader<?>> ret,
            List<ResourceReader> goodTypeReaders) {
        if (goodTypeReaders != null && !goodTypeReaders.isEmpty()) {
            List<ResourceReader> mediaTypeMatchingReaders = new ArrayList<>(goodTypeReaders.size());
            for (ResourceReader goodTypeReader : goodTypeReaders) {
                if (!goodTypeReader.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeReader.mediaTypes());
                if (match != null || mediaType == null) {
                    mediaTypeMatchingReaders.add(goodTypeReader);
                }
            }
            mediaTypeMatchingReaders.sort(ResourceReader.ResourceReaderComparator.INSTANCE);
            for (ResourceReader mediaTypeMatchingReader : mediaTypeMatchingReaders) {
                ret.add(mediaTypeMatchingReader.getInstance());
            }
        }
    }

    public <T> void addWriter(Class<T> entityClass, ResourceWriter writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader reader) {
        readers.add(entityClass, reader);
    }

    public List<MessageBodyWriter<?>> findBuildTimeWriters(Class<?> entityType, RuntimeType runtimeType, String... produces) {
        List<MediaType> type = new ArrayList<>();
        for (String i : produces) {
            type.add(MediaType.valueOf(i));
        }
        return findBuildTimeWriters(entityType, runtimeType, type);
    }

    private List<MessageBodyWriter<?>> findBuildTimeWriters(Class<?> entityType, RuntimeType runtimeType,
            List<MediaType> produces) {
        if (Response.class.isAssignableFrom(entityType)) {
            return Collections.emptyList();
        }
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        //first we check to make sure that the return type is build time selectable
        //this fails when there are eligible writers for a sub type of the entity type
        //e.g. if the entity type is Object and there are mappers for String then we
        //can't determine the type at build time
        for (Map.Entry<Class<?>, List<ResourceWriter>> entry : writers.entrySet()) {
            if (klass.isAssignableFrom(entry.getKey()) && !entry.getKey().equals(klass)) {
                //this is a writer registered under a sub type
                //check to see if the media type is relevant
                if (produces == null || produces.isEmpty()) {
                    return null;
                } else {
                    for (ResourceWriter writer : entry.getValue()) {
                        MediaType match = MediaTypeHelper.getBestMatch(produces, writer.modifiableMediaTypes());
                        if (match != null) {
                            return null;
                        }
                    }
                }
            }

        }
        return toMessageBodyWriters(findResourceWriters(writers, klass, produces, runtimeType));
    }

    protected List<ResourceWriter> findResourceWriters(QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers, Class<?> klass,
            List<MediaType> produces, RuntimeType runtimeType) {
        List<ResourceWriter> ret = new ArrayList<>();
        Deque<Class<?>> toProcess = new LinkedList<>();
        do {
            if (klass == Object.class) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceWriter> goodTypeWriters = writers.get(iface);
                    writerLookup(runtimeType, produces, ret, goodTypeWriters);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceWriter> goodTypeWriters = writers.get(klass);
            writerLookup(runtimeType, produces, ret, goodTypeWriters);
            toProcess.addAll(Arrays.asList(klass.getInterfaces()));
            // if we're an interface, pretend our superclass is Object to get us through the same logic as a class
            if (klass.isInterface())
                klass = Object.class;
            else
                klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    @SuppressWarnings("rawtypes")
    protected List<MessageBodyWriter<?>> toMessageBodyWriters(List<ResourceWriter> resourceWriters) {
        List<MessageBodyWriter<?>> ret = new ArrayList<>(resourceWriters.size());
        Set<Class<? extends MessageBodyWriter>> alreadySeenClasses = new HashSet<>(resourceWriters.size());
        for (ResourceWriter resourceWriter : resourceWriters) {
            MessageBodyWriter<?> instance = resourceWriter.getInstance();
            Class<? extends MessageBodyWriter> instanceClass = instance.getClass();
            if (alreadySeenClasses.contains(instanceClass)) {
                continue;
            }
            ret.add(instance);
            alreadySeenClasses.add(instanceClass);
        }
        return ret;
    }

    private void writerLookup(RuntimeType runtimeType, List<MediaType> mt, List<ResourceWriter> ret,
            List<ResourceWriter> goodTypeWriters) {
        if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
            List<ResourceWriter> mediaTypeMatchingWriters = new ArrayList<>(goodTypeWriters.size());
            for (ResourceWriter goodTypeWriter : goodTypeWriters) {
                if (!goodTypeWriter.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeWriter.modifiableMediaTypes());
                if (match != null) {
                    mediaTypeMatchingWriters.add(goodTypeWriter);
                }
            }
            // we sort here because the spec mentions that the writers closer to the requested java type are tried first
            mediaTypeMatchingWriters.sort(ResourceWriter.ResourceWriterComparator.INSTANCE);
            ret.addAll(mediaTypeMatchingWriters);
        }
    }

    public void registerBuiltins(RuntimeType constraint) {
        for (BuiltinWriter builtinWriter : BUILTIN_WRITERS) {
            if (builtinWriter.constraint == null || builtinWriter.constraint == constraint) {
                MessageBodyWriter<?> writer;
                try {
                    writer = builtinWriter.writerClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                ResourceWriter resourceWriter = new ResourceWriter();
                resourceWriter.setConstraint(builtinWriter.constraint);
                resourceWriter.setMediaTypeStrings(Collections.singletonList(builtinWriter.mediaType));
                // FIXME: we could still support beans
                resourceWriter.setFactory(new UnmanagedBeanFactory<MessageBodyWriter<?>>(writer));
                addWriter(builtinWriter.entityClass, resourceWriter);
            }
        }
        for (BuiltinReader builtinReader : BUILTIN_READERS) {
            if (builtinReader.constraint == null || builtinReader.constraint == constraint) {
                MessageBodyReader<?> reader;
                try {
                    reader = builtinReader.readerClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                ResourceReader resourceWriter = new ResourceReader();
                resourceWriter.setConstraint(builtinReader.constraint);
                resourceWriter.setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType));
                // FIXME: we could still support beans
                resourceWriter.setFactory(new UnmanagedBeanFactory<MessageBodyReader<?>>(reader));
                addReader(builtinReader.entityClass, resourceWriter);
            }
        }
    }

    public List<MessageBodyWriter<?>> findWriters(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType resolvedMediaType) {
        return findWriters(configuration, entityType, resolvedMediaType, null);
    }

    public List<MessageBodyWriter<?>> findWriters(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType resolvedMediaType, RuntimeType runtimeType) {
        // FIXME: invocation is very different between client and server, where the server doesn't treat GenericEntity specially
        // it's probably missing from there, while the client handles it upstack
        List<MediaType> mt = Collections.singletonList(resolvedMediaType);
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers;
        if (configuration != null && !configuration.getResourceWriters().isEmpty()) {
            writers = new QuarkusMultivaluedHashMap<>();
            writers.putAll(this.writers);
            writers.addAll(configuration.getResourceWriters());
        } else {
            writers = this.writers;
        }

        return toMessageBodyWriters(findResourceWriters(writers, klass, mt, runtimeType));
    }

    public static class Builtin {
        public final Class<?> entityClass;
        public final String mediaType;
        public final RuntimeType constraint;

        public Builtin(Class<?> entityClass, String mediaType, RuntimeType constraint) {
            this.entityClass = entityClass;
            this.mediaType = mediaType;
            this.constraint = constraint;
        }
    }

    public static class BuiltinWriter extends Builtin {
        public final Class<? extends MessageBodyWriter<?>> writerClass;

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType) {
            this(entityClass, writerClass, mediaType, null);
        }

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.writerClass = writerClass;
        }
    }

    public static class BuiltinReader extends Builtin {
        public final Class<? extends MessageBodyReader<?>> readerClass;

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType) {
            this(entityClass, readerClass, mediaType, null);
        }

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.readerClass = readerClass;
        }
    }
}
