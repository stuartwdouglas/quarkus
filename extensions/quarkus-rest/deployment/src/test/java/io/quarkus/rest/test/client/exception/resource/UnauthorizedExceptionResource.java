package io.quarkus.rest.test.client.exception.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class UnauthorizedExceptionResource implements UnauthorizedExceptionInterface {
    public void postIt(String msg) {
        throw new WebApplicationException(Status.UNAUTHORIZED);
    }
}
