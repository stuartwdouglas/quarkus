package io.quarkus.rest.server.runtime.handlers;

import io.quarkus.rest.server.runtime.core.AbstractQuarkusRestContext;

public interface RestHandler<T extends AbstractQuarkusRestContext> {

    void handle(T requestContext) throws Exception;

}
