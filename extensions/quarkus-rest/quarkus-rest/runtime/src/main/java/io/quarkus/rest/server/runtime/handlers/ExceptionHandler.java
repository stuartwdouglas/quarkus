package io.quarkus.rest.server.runtime.handlers;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

/**
 * Our job is to turn an exception into a Response instance. This is only present in the abort chain
 */
public class ExceptionHandler implements ServerRestHandler {

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.mapExceptionIfPresent();
    }
}
