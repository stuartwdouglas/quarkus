package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.NoLogWebApplicationException;

import io.quarkus.rest.test.client.proxy.ClientResponseFailureTest;

public class ClientResponseFailureResource implements ClientResponseFailureTest.ClientResponseFailureResourceInterface {
    public String get() {
        return "hello world";
    }

    public String error() {
        Response r = Response.status(404).type("text/plain").entity("there was an error").build();
        throw new NoLogWebApplicationException(r);
    }
}
