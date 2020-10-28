package io.quarkus.rest.server.runtime.core.serialization;

import java.io.IOException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.core.ServerSerialisers;

/**
 * A fixed entity writer that can be used when we know the result will always be written
 * by a given provider.
 */
public class FixedEntityWriter implements EntityWriter {

    private final MessageBodyWriter writer;
    private final ServerSerialisers serialisers;

    public FixedEntityWriter(MessageBodyWriter writer, ServerSerialisers serialisers) {
        this.writer = writer;
        this.serialisers = serialisers;
    }

    @Override
    public void write(QuarkusRestRequestContext context, Object entity) throws IOException {
        if (!ServerSerialisers.invokeWriter(context, entity, writer, serialisers)) {
            throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                    Response.serverError().build());
        }
    }

}
