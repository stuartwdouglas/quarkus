package io.quarkus.rest.test.resource.request.resource;

import java.util.GregorianCalendar;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

@Path("/")
public class PreconditionLastModifiedResource {

    @GET
    public Response doGet(@Context Request request) {
        GregorianCalendar lastModified = new GregorianCalendar(2007, 0, 0, 0, 0, 0);
        Response.ResponseBuilder rb = request.evaluatePreconditions(lastModified.getTime());
        if (rb != null) {
            return rb.build();
        }

        return Response.ok("foo", "text/plain").build();
    }
}
