package io.quarkus.rest.server.test.stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.rest.common.runtime.util.MultiCollectors;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

@Path("stream")
public class StreamResource {

    @Path("text/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getCollectedText() {
        return MultiCollectors.concatenateStrings(getStreamedText());
    }

    @Path("text/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> getStreamedText() {
        return Multi.createFrom().items("foo", "bar");
    }

    @Path("byte-arrays/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<byte[]> getCollectedByteArrays() {
        return MultiCollectors.concatenateByteArrays(getStreamedByteArrays());
    }

    @Path("byte-arrays/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<byte[]> getStreamedByteArrays() {
        return Multi.createFrom().items("foo".getBytes(), "bar".getBytes());
    }

    @Path("char-arrays/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<char[]> getCollectedCharacterArrays() {
        return MultiCollectors.concatenateCharArrays(getStreamedCharArrays());
    }

    @Path("char-arrays/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<char[]> getStreamedCharArrays() {
        return Multi.createFrom().items("foo".toCharArray(), "bar".toCharArray());
    }

    @Path("buffer/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Buffer> getCollectedBuffers() {
        return MultiCollectors.concatenateBuffers(getStreamedBuffers());
    }

    @Path("buffer/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<Buffer> getStreamedBuffers() {
        return Multi.createFrom().items(Buffer.buffer("foo"), Buffer.buffer("bar"));
    }
}
