package io.quarkus.jaxrs.client.runtime;

import java.io.InputStream;

import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestResponse;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestResponseBuilder;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestServerResponseBuilder;

public class QuarkusRestClientResponseBuilder extends QuarkusRestServerResponseBuilder { //TODO: should not extend the server version

    InputStream entityStream;
    RestClientRequestContext restClientRequestContext;

    public QuarkusRestClientResponseBuilder invocationState(RestClientRequestContext restClientRequestContext) {
        this.restClientRequestContext = restClientRequestContext;
        return this;
    }

    public QuarkusRestClientResponseBuilder entityStream(InputStream entityStream) {
        this.entityStream = entityStream;
        return this;
    }

    @Override
    protected QuarkusRestResponseBuilder doClone() {
        return new QuarkusRestClientResponseBuilder();
    }

    @Override
    public QuarkusRestResponse build() {
        QuarkusRestClientResponse response = new QuarkusRestClientResponse();
        populateResponse(response);
        response.restClientRequestContext = restClientRequestContext;
        response.setEntityStream(entityStream);
        return response;
    }
}
