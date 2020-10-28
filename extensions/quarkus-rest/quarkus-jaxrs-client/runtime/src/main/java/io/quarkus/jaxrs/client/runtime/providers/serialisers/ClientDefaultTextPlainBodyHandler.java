package io.quarkus.jaxrs.client.runtime.providers.serialisers;

import javax.ws.rs.Consumes;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.common.runtime.providers.serialisers.DefaultTextPlainBodyHandler;

@Provider
@Consumes("text/plain")
public class ClientDefaultTextPlainBodyHandler extends DefaultTextPlainBodyHandler {

    @Override
    protected void validateInput(String input) throws ProcessingException {
        if (input.isEmpty()) {
            throw new ProcessingException(new NoContentException("Input was empty"));
        }
    }
}
