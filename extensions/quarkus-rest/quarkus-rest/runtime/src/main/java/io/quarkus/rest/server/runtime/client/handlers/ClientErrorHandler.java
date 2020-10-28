package io.quarkus.rest.server.runtime.client.handlers;

import io.quarkus.rest.server.runtime.client.ClientRestHandler;
import io.quarkus.rest.server.runtime.client.RestClientRequestContext;

/**
 * Simple error handler that fails the result
 */
public class ClientErrorHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (requestContext.getThrowable() != null) {
            requestContext.getResult().completeExceptionally(requestContext.getThrowable());
        }
    }
}
