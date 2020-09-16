package io.quarkus.rest.test.exception.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
public class WebApplicationExceptionResource {
    @Path("/exception")
    @GET
    public Response get() throws WebApplicationException {
        throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
    }

    @Path("/exception/entity")
    @GET
    public Response getEntity() throws WebApplicationException {
        throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity("error").build());
    }
}
